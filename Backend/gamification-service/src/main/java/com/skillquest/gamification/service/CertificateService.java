package com.skillquest.gamification.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.skillquest.gamification.client.CourseClient;
import com.skillquest.gamification.domain.Certificate;
import com.skillquest.gamification.domain.CourseCompletion;
import com.skillquest.gamification.domain.Quiz;
import com.skillquest.gamification.dto.ApiDtos.CertificateResponse;
import com.skillquest.gamification.dto.ApiDtos.CertifiedProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.ApiDtos.VerificationResponse;
import com.skillquest.gamification.dto.QuizDtos.CertificationProgress;
import com.skillquest.gamification.exception.CourseServiceException;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.repository.CertificateRepository;
import com.skillquest.gamification.repository.CourseCompletionRepository;
import com.skillquest.gamification.repository.QuizAttemptRepository;
import com.skillquest.gamification.repository.QuizRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Delivrance des certificats. Regle : un certificat est emis quand le cours est termine
 * ET que la moyenne des meilleurs scores des quiz du cours atteint le seuil (80 % par defaut).
 * Un cours sans quiz n'impose pas de condition de score.
 */
@Service
public class CertificateService {

    public static final String COURSE_GRADUATE_BADGE = "COURSE_GRADUATE";
    static final String DEFAULT_STUDENT_NAME = "Étudiant SkillQuest";

    private static final Logger log = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final CourseCompletionRepository completionRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CertificateHasher hasher;
    private final CertificatePdfGenerator pdfGenerator;
    private final CourseClient courseClient;
    private final GamificationService gamificationService;
    private final TransactionTemplate transactionTemplate;
    private final int requiredAverage;

    public CertificateService(CertificateRepository certificateRepository,
                              CourseCompletionRepository completionRepository,
                              QuizRepository quizRepository,
                              QuizAttemptRepository attemptRepository,
                              CertificateHasher hasher,
                              CertificatePdfGenerator pdfGenerator,
                              CourseClient courseClient,
                              GamificationService gamificationService,
                              PlatformTransactionManager transactionManager,
                              @Value("${gamification.certificate.min-average-score:80}") int requiredAverage) {
        this.certificateRepository = certificateRepository;
        this.completionRepository = completionRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.hasher = hasher;
        this.pdfGenerator = pdfGenerator;
        this.courseClient = courseClient;
        this.gamificationService = gamificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.requiredAverage = requiredAverage;
    }

    // ------------------------------------------------------------ regle des 80 %

    /**
     * Evenement "cours termine" : on memorise la completion, puis on delivre le certificat si les quiz
     * sont reussis. Les erreurs (ex. course-service injoignable) remontent pour que RabbitMQ rejoue le message.
     */
    public void onCourseCompleted(UUID userId, String courseId, String studentFullName) {
        String name = normalizeName(studentFullName);
        transactionTemplate.executeWithoutResult(status -> {
            if (completionRepository.findByUserIdAndCourseId(userId, courseId).isEmpty()) {
                completionRepository.saveAndFlush(new CourseCompletion(userId, courseId, name));
            }
        });
        evaluate(userId, courseId, true);
    }

    /** Delivre le certificat si l'etudiant y a droit et ne l'a pas encore. */
    public CertificationProgress evaluate(UUID userId, String courseId, boolean propagateFailures) {
        CertificationProgress progress = progress(userId, courseId);
        if (!progress.eligible() || progress.certificateId() != null) {
            return progress;
        }
        String name = completionRepository.findByUserIdAndCourseId(userId, courseId)
                .map(CourseCompletion::getStudentFullName).orElse(DEFAULT_STUDENT_NAME);
        try {
            issueCertificate(userId, courseId, name);
        } catch (CourseServiceException ex) {
            if (propagateFailures) {
                throw ex;
            }
            // La tentative de quiz est deja enregistree : le certificat sera emis plus tard.
            log.warn("Certificate for user={} course={} postponed: {}", userId, courseId, ex.getMessage());
            return progress;
        } catch (DataIntegrityViolationException ex) {
            log.info("Certificate for user={} course={} was issued concurrently", userId, courseId);
        }
        return progress(userId, courseId);
    }

    /** Progression de l'etudiant vers le certificat d'un cours. */
    @Transactional(readOnly = true)
    public CertificationProgress progress(UUID userId, String courseId) {
        boolean completed = completionRepository.findByUserIdAndCourseId(userId, courseId).isPresent();
        List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId);

        Integer averageRounded = null;
        boolean quizzesPassed = true;
        if (!quizzes.isEmpty()) {
            Map<UUID, Integer> best = new HashMap<>();
            for (Object[] row : attemptRepository.findBestScoresByCourse(userId, courseId)) {
                best.put((UUID) row[0], ((Number) row[1]).intValue());
            }
            double average = quizzes.stream().mapToInt(q -> best.getOrDefault(q.getId(), 0)).sum()
                    / (double) quizzes.size();
            averageRounded = (int) Math.round(average);
            quizzesPassed = average >= requiredAverage;
        }

        var certificate = certificateRepository.findByUserIdAndCourseId(userId, courseId);
        boolean issued = certificate.isPresent() && certificate.get().getRevokedAt() == null;
        return new CertificationProgress(courseId, completed, quizzes.size(), averageRounded, requiredAverage,
                completed && quizzesPassed, issued, certificate.map(Certificate::getId).orElse(null));
    }

    // ------------------------------------------------------------ emission

    /**
     * Emet (de facon idempotente) le certificat d'un cours : appelle course-service pour le titre exact,
     * signe le certificat puis attribue le badge COURSE_GRADUATE. L'appel REST se fait hors transaction SQL.
     */
    public Certificate issueCertificate(UUID userId, String courseId, String studentFullName) {
        var existing = certificateRepository.findByUserIdAndCourseId(userId, courseId);
        if (existing.isPresent()) {
            log.info("Certificate already issued for user={} course={}", userId, courseId);
            return existing.get();
        }

        String courseTitle = courseClient.fetchCourseTitle(courseId);
        String fullName = normalizeName(studentFullName);

        return transactionTemplate.execute(status -> {
            var concurrent = certificateRepository.findByUserIdAndCourseId(userId, courseId);
            if (concurrent.isPresent()) {
                return concurrent.get();
            }
            // Precision a la seconde : l'horodatage relu depuis la base redonne exactement la meme empreinte.
            Certificate certificate = new Certificate(userId, courseId, fullName, courseTitle,
                    Instant.now().truncatedTo(ChronoUnit.SECONDS));
            certificate.setCertificateHash(hasher.generateHash(certificate));
            certificateRepository.saveAndFlush(certificate);
            gamificationService.awardBadge(userId, COURSE_GRADUATE_BADGE);
            log.info("Issued certificate {} for user={} course={}", certificate.getId(), userId, courseId);
            return certificate;
        });
    }

    // ------------------------------------------------------------ lecture

    /** Profil certifie (XP, niveau, badges, certificats non revoques) consomme par job-board-service. */
    @Transactional(readOnly = true)
    public CertifiedProfileResponse getCertifiedProfile(UUID userId) {
        ProfileResponse profile = gamificationService.getProfile(userId);
        List<CertificateResponse> certificates = certificateRepository.findAllByUserIdOrderByIssuedAtDesc(userId)
                .stream().filter(c -> c.getRevokedAt() == null).map(CertificateService::toResponse).toList();
        return new CertifiedProfileResponse(userId, profile.totalXP(), profile.currentLevel(),
                gamificationService.getUserBadges(userId), certificates);
    }

    /**
     * Verification publique. Valide si le contenu n'a pas ete altere, si le certificat n'est pas revoque et,
     * lorsqu'une empreinte est fournie, si elle correspond a celle du certificat.
     */
    @Transactional(readOnly = true)
    public VerificationResponse verify(UUID certificateId, String candidateHash) {
        Certificate c = requireCertificate(certificateId);
        boolean valid = c.getRevokedAt() == null && hasher.isIntact(c)
                && (candidateHash == null || hasher.verifyHash(c, candidateHash));
        return new VerificationResponse(valid, c.getId(), c.getStudentFullName(), c.getCourseTitle(),
                c.getIssuedAt(), c.getCertificateHash());
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(UUID certificateId) {
        Certificate c = requireCertificate(certificateId);
        if (c.getRevokedAt() != null) {
            throw new ResourceNotFoundException("Certificate " + certificateId + " has been revoked");
        }
        return pdfGenerator.generate(c);
    }

    private Certificate requireCertificate(UUID certificateId) {
        return certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate " + certificateId + " not found"));
    }

    private static String normalizeName(String studentFullName) {
        return (studentFullName == null || studentFullName.isBlank()) ? DEFAULT_STUDENT_NAME : studentFullName.trim();
    }

    private static CertificateResponse toResponse(Certificate c) {
        return new CertificateResponse(c.getId(), c.getUserId(), c.getCourseId(), c.getStudentFullName(),
                c.getCourseTitle(), c.getCertificateHash(), c.getIssuedAt());
    }
}
