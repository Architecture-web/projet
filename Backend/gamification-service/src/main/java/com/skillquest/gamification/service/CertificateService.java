package com.skillquest.gamification.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.skillquest.gamification.client.CourseClient;
import com.skillquest.gamification.domain.Certificate;
import com.skillquest.gamification.dto.ApiDtos.CertificateResponse;
import com.skillquest.gamification.dto.ApiDtos.CertifiedProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.VerificationResponse;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.repository.CertificateRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CertificateService {

    public static final String COURSE_GRADUATE_BADGE = "COURSE_GRADUATE";
    static final String DEFAULT_STUDENT_NAME = "Étudiant SkillQuest";

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final CertificateHasher hasher;
    private final CertificatePdfGenerator pdfGenerator;
    private final CourseClient courseClient;
    private final GamificationService gamificationService;
    private final TransactionTemplate transactionTemplate;

    public CertificateService(CertificateRepository certificateRepository,
                              CertificateHasher hasher,
                              CertificatePdfGenerator pdfGenerator,
                              CourseClient courseClient,
                              GamificationService gamificationService,
                              PlatformTransactionManager transactionManager) {
        this.certificateRepository = certificateRepository;
        this.hasher = hasher;
        this.pdfGenerator = pdfGenerator;
        this.courseClient = courseClient;
        this.gamificationService = gamificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Émet (de façon idempotente) le certificat d'un cours terminé : appelle course-service pour
     * le titre exact du cours, signe le certificat puis attribue le badge COURSE_GRADUATE.
     * L'appel REST se fait <i>hors transaction</i> pour ne pas bloquer une connexion SQL pendant l'appel réseau.
     */
    public Certificate issueCertificate(UUID userId, String courseId, String studentFullName) {
        var existing = certificateRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            log.info("Certificate already issued for user={} course={}", userId, courseId);
            return existing.get();
        }

        String courseTitle = courseClient.fetchCourseTitle(courseId);
        String fullName = (studentFullName == null || studentFullName.isBlank())
                ? DEFAULT_STUDENT_NAME : studentFullName.trim();

        return transactionTemplate.execute(status -> {
            var concurrent = certificateRepository.findByUserIdAndCourseId(userId, courseId);
            if (concurrent.isPresent()) {
                return concurrent.get();
            }
            // Précision à la seconde : l'horodatage relu depuis la base redonne exactement la même empreinte.
            Certificate certificate = new Certificate(userId, courseId, fullName, courseTitle,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS));
            certificate.setCertificateHash(hasher.generateHash(certificate));
            certificateRepository.saveAndFlush(certificate);
            gamificationService.awardBadge(userId, COURSE_GRADUATE_BADGE);
            log.info("Issued certificate {} for user={} course={}", certificate.getId(), userId, courseId);
            return certificate;
        });
    }

    /** Profil certifié (XP, niveau, badges, certificats) consommé par job-board-service. */
    @Transactional(readOnly = true)
    public CertifiedProfileResponse getCertifiedProfile(UUID userId) {
        ProfileResponse profile = gamificationService.getProfile(userId);
        List<CertificateResponse> certificates = certificateRepository.findAllByUserIdOrderByIssuedAtDesc(userId)
                .stream().map(CertificateService::toResponse).toList();
        return new CertifiedProfileResponse(userId, profile.totalXP(), profile.currentLevel(),
                gamificationService.getUserBadges(userId), certificates);
    }

    /**
     * Vérification publique. Le certificat est valide si son contenu n'a pas été altéré
     * et, lorsqu'une empreinte est fournie, si elle correspond à celle du certificat.
     */
    @Transactional(readOnly = true)
    public VerificationResponse verify(UUID certificateId, String candidateHash) {
        Certificate c = requireCertificate(certificateId);
        boolean valid = hasher.isIntact(c) && (candidateHash == null || hasher.verifyHash(c, candidateHash));
        return new VerificationResponse(valid, c.getId(), c.getStudentFullName(), c.getCourseTitle(),
                c.getIssuedAt(), c.getCertificateHash());
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(UUID certificateId) {
        return pdfGenerator.generate(requireCertificate(certificateId));
    }

    private Certificate requireCertificate(UUID certificateId) {
        return certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate " + certificateId + " not found"));
    }

    private static CertificateResponse toResponse(Certificate c) {
        return new CertificateResponse(c.getId(), c.getUserId(), c.getCourseId(), c.getStudentFullName(),
                c.getCourseTitle(), c.getCertificateHash(), c.getIssuedAt());
    }
}
