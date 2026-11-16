package com.skillquest.gamification.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.skillquest.gamification.domain.Quiz;
import com.skillquest.gamification.domain.QuizAttempt;
import com.skillquest.gamification.domain.QuizOption;
import com.skillquest.gamification.domain.QuizQuestion;
import com.skillquest.gamification.dto.QuizDtos.AnswerRequest;
import com.skillquest.gamification.dto.QuizDtos.AttemptResult;
import com.skillquest.gamification.dto.QuizDtos.CertificationProgress;
import com.skillquest.gamification.dto.QuizDtos.OptionView;
import com.skillquest.gamification.dto.QuizDtos.QuestionView;
import com.skillquest.gamification.dto.QuizDtos.QuizDetail;
import com.skillquest.gamification.dto.QuizDtos.QuizSummary;
import com.skillquest.gamification.dto.QuizDtos.ReviewItem;
import com.skillquest.gamification.exception.BadRequestException;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.repository.QuizAttemptRepository;
import com.skillquest.gamification.repository.QuizRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Le mini-jeu de quiz. Chaque tentative est notee en pourcentage. L'XP gagnee recompense la progression :
 * seul le gain par rapport au meilleur score precedent est credite, ce qui empeche de farmer de l'XP en rejouant.
 */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final GamificationService gamificationService;
    private final CertificateService certificateService;
    private final TransactionTemplate transactionTemplate;
    private final int passingScore;

    public QuizService(QuizRepository quizRepository,
                       QuizAttemptRepository attemptRepository,
                       GamificationService gamificationService,
                       CertificateService certificateService,
                       PlatformTransactionManager transactionManager,
                       @Value("${gamification.certificate.min-average-score:80}") int passingScore) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.gamificationService = gamificationService;
        this.certificateService = certificateService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.passingScore = passingScore;
    }

    @Transactional(readOnly = true)
    public List<QuizSummary> list(UUID userId, String courseId) {
        List<Quiz> quizzes = (courseId == null || courseId.isBlank())
                ? quizRepository.findAllByOrderByCourseIdAscCreatedAtAsc()
                : quizRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        Map<UUID, Integer> best = new HashMap<>();
        for (Object[] row : attemptRepository.findBestScoresByUser(userId)) {
            best.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return quizzes.stream()
                .map(q -> new QuizSummary(q.getId(), q.getCourseId(), q.getTitle(), q.getDescription(),
                        q.getXpReward(), q.getQuestions().size(), best.get(q.getId())))
                .toList();
    }

    /** Le quiz a jouer : les bonnes reponses ne sont volontairement pas incluses. */
    @Transactional(readOnly = true)
    public QuizDetail get(UUID quizId) {
        Quiz quiz = requireQuiz(quizId);
        List<QuestionView> questions = quiz.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getText(),
                        q.getOptions().stream().map(o -> new OptionView(o.getId(), o.getText())).toList()))
                .toList();
        return new QuizDetail(quiz.getId(), quiz.getCourseId(), quiz.getTitle(), quiz.getDescription(),
                quiz.getXpReward(), questions);
    }

    /** Corrige une tentative, credite l'XP gagnee puis tente de delivrer le certificat du cours. */
    public AttemptResult submit(UUID userId, UUID quizId, List<AnswerRequest> answers) {
        Outcome outcome = transactionTemplate.execute(status -> play(userId, quizId, answers));
        // Hors transaction : l'appel a course-service ne doit pas bloquer une connexion SQL.
        CertificationProgress certification = certificateService.evaluate(userId, outcome.courseId(), false);
        return new AttemptResult(outcome.attemptId(), outcome.score(), outcome.correct(), outcome.total(),
                outcome.bestScore(), outcome.score() >= passingScore, outcome.xpGained(), outcome.review(),
                certification);
    }

    @Transactional(readOnly = true)
    public CertificationProgress progress(UUID userId, String courseId) {
        return certificateService.progress(userId, courseId);
    }

    private Outcome play(UUID userId, UUID quizId, List<AnswerRequest> answers) {
        Quiz quiz = requireQuiz(quizId);
        Map<UUID, QuizQuestion> questions = quiz.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
        Map<UUID, UUID> selectedByQuestion = new HashMap<>();
        for (AnswerRequest answer : answers) {
            if (!questions.containsKey(answer.questionId())) {
                throw new BadRequestException("Question " + answer.questionId() + " does not belong to this quiz");
            }
            selectedByQuestion.put(answer.questionId(), answer.optionId());
        }

        int correctCount = 0;
        List<ReviewItem> review = new ArrayList<>();
        for (QuizQuestion question : quiz.getQuestions()) {
            UUID correctOption = question.getOptions().stream().filter(QuizOption::isCorrect)
                    .map(QuizOption::getId).findFirst().orElse(null);
            UUID selected = selectedByQuestion.get(question.getId());
            boolean correct = selected != null && selected.equals(correctOption);
            if (correct) {
                correctCount++;
            }
            review.add(new ReviewItem(question.getId(), correctOption, selected, correct));
        }

        int total = quiz.getQuestions().size();
        int score = total == 0 ? 0 : (int) Math.round(100.0 * correctCount / total);
        Integer previousBest = attemptRepository.findBestScore(userId, quizId);
        int oldBest = previousBest == null ? 0 : previousBest;
        int newBest = Math.max(oldBest, score);
        int xpGained = newBest > oldBest ? (int) Math.round(quiz.getXpReward() * (newBest - oldBest) / 100.0) : 0;

        QuizAttempt attempt = attemptRepository.save(new QuizAttempt(userId, quiz, score, correctCount, total, xpGained));
        if (xpGained > 0) {
            gamificationService.addXP(userId, xpGained);
        }
        return new Outcome(quiz.getCourseId(), attempt.getId(), score, correctCount, total, newBest, xpGained, review);
    }

    private Quiz requireQuiz(UUID quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz " + quizId + " not found"));
    }

    private record Outcome(String courseId, UUID attemptId, int score, int correct, int total, int bestScore,
                           int xpGained, List<ReviewItem> review) {
    }
}
