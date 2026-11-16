package com.skillquest.gamification.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.skillquest.gamification.domain.Badge;
import com.skillquest.gamification.domain.Certificate;
import com.skillquest.gamification.domain.Quiz;
import com.skillquest.gamification.domain.QuizOption;
import com.skillquest.gamification.domain.QuizQuestion;
import com.skillquest.gamification.domain.UserProfileGamification;
import com.skillquest.gamification.dto.ApiDtos.BadgeResponse;
import com.skillquest.gamification.dto.ApiDtos.ProfileResponse;
import com.skillquest.gamification.dto.QuizDtos.AdminCertificateView;
import com.skillquest.gamification.dto.QuizDtos.AdminOptionView;
import com.skillquest.gamification.dto.QuizDtos.AdminPlayerView;
import com.skillquest.gamification.dto.QuizDtos.AdminQuestionView;
import com.skillquest.gamification.dto.QuizDtos.AdminQuizView;
import com.skillquest.gamification.dto.QuizDtos.BadgeUpsert;
import com.skillquest.gamification.dto.QuizDtos.QuestionUpsert;
import com.skillquest.gamification.dto.QuizDtos.QuizUpsert;
import com.skillquest.gamification.exception.BadRequestException;
import com.skillquest.gamification.exception.ConflictException;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.repository.BadgeRepository;
import com.skillquest.gamification.repository.CertificateRepository;
import com.skillquest.gamification.repository.QuizAttemptRepository;
import com.skillquest.gamification.repository.QuizRepository;
import com.skillquest.gamification.repository.UserBadgeRepository;
import com.skillquest.gamification.repository.UserProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operations du dashboard administrateur : CRUD des quiz et des badges, certificats, joueurs et XP. */
@Service
public class AdminService {

    public static final int MAX_LIST_SIZE = 200;

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final CertificateRepository certificateRepository;
    private final UserProfileRepository profileRepository;
    private final GamificationService gamificationService;

    public AdminService(QuizRepository quizRepository,
                        QuizAttemptRepository attemptRepository,
                        BadgeRepository badgeRepository,
                        UserBadgeRepository userBadgeRepository,
                        CertificateRepository certificateRepository,
                        UserProfileRepository profileRepository,
                        GamificationService gamificationService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.certificateRepository = certificateRepository;
        this.profileRepository = profileRepository;
        this.gamificationService = gamificationService;
    }

    // ------------------------------------------------------------ quiz

    @Transactional(readOnly = true)
    public List<AdminQuizView> listQuizzes() {
        return quizRepository.findAllByOrderByCourseIdAscCreatedAtAsc().stream().map(AdminService::toView).toList();
    }

    @Transactional(readOnly = true)
    public AdminQuizView getQuiz(UUID id) {
        return toView(requireQuiz(id));
    }

    @Transactional
    public AdminQuizView createQuiz(QuizUpsert request) {
        Quiz quiz = new Quiz(request.courseId().trim(), request.title().trim(), request.description(), xp(request));
        quiz.replaceQuestions(toQuestions(request.questions()));
        return toView(quizRepository.save(quiz));
    }

    @Transactional
    public AdminQuizView updateQuiz(UUID id, QuizUpsert request) {
        Quiz quiz = requireQuiz(id);
        quiz.update(request.courseId().trim(), request.title().trim(), request.description(), xp(request));
        quiz.replaceQuestions(toQuestions(request.questions()));
        return toView(quizRepository.save(quiz));
    }

    @Transactional
    public void deleteQuiz(UUID id) {
        Quiz quiz = requireQuiz(id);
        attemptRepository.deleteByQuizId(id);
        quizRepository.delete(quiz);
    }

    // ------------------------------------------------------------ badges

    @Transactional
    public BadgeResponse createBadge(BadgeUpsert request) {
        if (badgeRepository.findByCodeName(request.codeName()).isPresent()) {
            throw new ConflictException("Badge " + request.codeName() + " already exists");
        }
        Badge badge = badgeRepository.save(new Badge(request.codeName(), request.title(), request.description(),
                request.iconUrl(), request.requiredXP()));
        return toResponse(badge);
    }

    @Transactional
    public BadgeResponse updateBadge(Long id, BadgeUpsert request) {
        Badge badge = requireBadge(id);
        boolean system = CertificateService.COURSE_GRADUATE_BADGE.equals(badge.getCodeName());
        if (system && !badge.getCodeName().equals(request.codeName())) {
            throw new BadRequestException("The code of the system badge " + badge.getCodeName() + " cannot change");
        }
        badgeRepository.findByCodeName(request.codeName()).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Badge " + request.codeName() + " already exists");
                });
        badge.update(request.codeName(), request.title(), request.description(), request.iconUrl(),
                system ? null : request.requiredXP());
        return toResponse(badge);
    }

    @Transactional
    public void deleteBadge(Long id) {
        Badge badge = requireBadge(id);
        if (CertificateService.COURSE_GRADUATE_BADGE.equals(badge.getCodeName())) {
            throw new BadRequestException("The system badge " + badge.getCodeName() + " cannot be deleted");
        }
        userBadgeRepository.deleteByBadgeId(id);
        badgeRepository.delete(badge);
    }

    // ------------------------------------------------------------ certificats

    @Transactional(readOnly = true)
    public List<AdminCertificateView> listCertificates() {
        return certificateRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE, Sort.by(Sort.Direction.DESC, "issuedAt")))
                .stream()
                .map(c -> new AdminCertificateView(c.getId(), c.getUserId(), c.getCourseId(), c.getStudentFullName(),
                        c.getCourseTitle(), c.getCertificateHash(), c.getIssuedAt(), c.getRevokedAt()))
                .toList();
    }

    /** Revoque un certificat : il n'est plus valide a la verification publique et ne peut pas etre re-emis automatiquement. */
    @Transactional
    public AdminCertificateView revokeCertificate(UUID id) {
        Certificate c = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate " + id + " not found"));
        c.revoke();
        log.info("admin revoked certificate {}", id);
        return new AdminCertificateView(c.getId(), c.getUserId(), c.getCourseId(), c.getStudentFullName(),
                c.getCourseTitle(), c.getCertificateHash(), c.getIssuedAt(), c.getRevokedAt());
    }

    // ------------------------------------------------------------ joueurs

    @Transactional(readOnly = true)
    public List<AdminPlayerView> listPlayers(int limit) {
        int size = Math.min(Math.max(limit, 1), MAX_LIST_SIZE);
        List<UserProfileGamification> profiles = profileRepository.findTop(PageRequest.of(0, size));
        Map<UUID, Integer> badgeCounts = new HashMap<>();
        for (Object[] row : userBadgeRepository.countBadgesPerUser()) {
            badgeCounts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        List<AdminPlayerView> result = new ArrayList<>();
        int rank = 0;
        for (int i = 0; i < profiles.size(); i++) {
            UserProfileGamification p = profiles.get(i);
            if (i == 0 || p.getTotalXP() != profiles.get(i - 1).getTotalXP()) {
                rank = i + 1;
            }
            result.add(new AdminPlayerView(p.getUserId(), p.getTotalXP(), p.getCurrentLevel(), rank,
                    badgeCounts.getOrDefault(p.getUserId(), 0), p.getUpdatedAt()));
        }
        return result;
    }

    public ProfileResponse adjustXP(UUID userId, int amount) {
        return gamificationService.adjustXP(userId, amount);
    }

    // ------------------------------------------------------------ interne

    private Quiz requireQuiz(UUID id) {
        return quizRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quiz " + id + " not found"));
    }

    private Badge requireBadge(Long id) {
        return badgeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Badge " + id + " not found"));
    }

    private static int xp(QuizUpsert request) {
        return request.xpReward() == null ? 20 : request.xpReward();
    }

    /** Chaque question doit avoir exactement une bonne reponse. */
    private static List<QuizQuestion> toQuestions(List<QuestionUpsert> requested) {
        List<QuizQuestion> questions = new ArrayList<>();
        int number = 1;
        for (QuestionUpsert q : requested) {
            long correct = q.options().stream().filter(o -> o.correct()).count();
            if (correct != 1) {
                throw new BadRequestException("Question " + number + " must have exactly one correct answer");
            }
            QuizQuestion question = new QuizQuestion(q.text().trim());
            q.options().forEach(o -> question.addOption(new QuizOption(o.text().trim(), o.correct())));
            questions.add(question);
            number++;
        }
        return questions;
    }

    private static AdminQuizView toView(Quiz quiz) {
        List<AdminQuestionView> questions = quiz.getQuestions().stream()
                .map(q -> new AdminQuestionView(q.getId(), q.getText(), q.getOptions().stream()
                        .map(o -> new AdminOptionView(o.getId(), o.getText(), o.isCorrect())).toList()))
                .toList();
        return new AdminQuizView(quiz.getId(), quiz.getCourseId(), quiz.getTitle(), quiz.getDescription(),
                quiz.getXpReward(), questions);
    }

    private static BadgeResponse toResponse(Badge b) {
        return new BadgeResponse(b.getId(), b.getCodeName(), b.getTitle(), b.getDescription(), b.getIconUrl(),
                b.getRequiredXP());
    }
}
