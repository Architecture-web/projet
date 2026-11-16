package com.skillquest.gamification.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Contrats JSON du jeu de quiz (étudiant) et du dashboard administrateur. */
public final class QuizDtos {

    private QuizDtos() {
    }

    // ------------------------------------------------------------ côté étudiant

    /** Une réponse possible : la bonne réponse n'est jamais envoyée avant la soumission. */
    public record OptionView(UUID id, String text) {
    }

    public record QuestionView(UUID id, String text, List<OptionView> options) {
    }

    public record QuizSummary(UUID id, String courseId, String title, String description, int xpReward,
                              int questionCount, Integer bestScore) {
    }

    public record QuizDetail(UUID id, String courseId, String title, String description, int xpReward,
                             List<QuestionView> questions) {
    }

    public record AnswerRequest(@NotNull UUID questionId, UUID optionId) {
    }

    public record AttemptRequest(@NotNull @Valid List<AnswerRequest> answers) {
    }

    public record ReviewItem(UUID questionId, UUID correctOptionId, UUID selectedOptionId, boolean correct) {
    }

    /** Où en est l'étudiant pour obtenir le certificat d'un cours. */
    public record CertificationProgress(String courseId, boolean courseCompleted, int quizCount,
                                        Integer averageBestScore, int requiredAverage, boolean eligible,
                                        boolean certificateIssued, UUID certificateId) {
    }

    public record AttemptResult(UUID attemptId, int scorePercent, int correctCount, int totalQuestions,
                                int bestScore, boolean passed, int xpGained, List<ReviewItem> review,
                                CertificationProgress certification) {
    }

    // ------------------------------------------------------------ côté admin

    public record OptionUpsert(@NotBlank @Size(max = 300) String text, boolean correct) {
    }

    public record QuestionUpsert(@NotBlank @Size(max = 500) String text,
                                 @NotNull @Size(min = 2, max = 6) @Valid List<OptionUpsert> options) {
    }

    public record QuizUpsert(@NotBlank @Size(max = 100) String courseId,
                             @NotBlank @Size(max = 200) String title,
                             @Size(max = 500) String description,
                             @Min(0) @Max(1000) Integer xpReward,
                             @NotNull @Size(min = 1, max = 50) @Valid List<QuestionUpsert> questions) {
    }

    public record AdminOptionView(UUID id, String text, boolean correct) {
    }

    public record AdminQuestionView(UUID id, String text, List<AdminOptionView> options) {
    }

    public record AdminQuizView(UUID id, String courseId, String title, String description, int xpReward,
                                List<AdminQuestionView> questions) {
    }

    public record BadgeUpsert(@NotBlank @Pattern(regexp = "^[A-Z0-9_]{3,64}$",
            message = "codeName: 3 to 64 chars, A-Z, 0-9 and _") String codeName,
                              @NotBlank @Size(max = 200) String title,
                              @Size(max = 500) String description,
                              @Size(max = 500) String iconUrl,
                              @Min(1) Integer requiredXP) {
    }

    public record AdminCertificateView(UUID id, UUID userId, String courseId, String studentFullName,
                                       String courseTitle, String certificateHash, Instant issuedAt,
                                       Instant revokedAt) {
    }

    public record AdminPlayerView(UUID userId, int totalXP, int currentLevel, int rank, int badgeCount,
                                  Instant updatedAt) {
    }

    public record XpAdjustRequest(@Min(-100000) @Max(100000) int amount, @Size(max = 200) String reason) {
    }
}
