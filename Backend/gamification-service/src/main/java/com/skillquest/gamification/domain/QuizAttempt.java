package com.skillquest.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Une tentative d'un étudiant sur un quiz. On conserve toutes les tentatives, seule la meilleure compte pour le certificat. */
@Entity
@Table(name = "quiz_attempt", indexes = @Index(name = "idx_attempt_user_quiz", columnList = "user_id, quiz_id"))
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    protected QuizAttempt() {
    }

    public QuizAttempt(UUID userId, Quiz quiz, int scorePercent, int correctCount, int totalQuestions, int xpAwarded) {
        this.userId = userId;
        this.quiz = quiz;
        this.scorePercent = scorePercent;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.xpAwarded = xpAwarded;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }
}
