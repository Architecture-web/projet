package com.skillquest.gamification.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Un quiz (mini-jeu) rattaché à un cours. {@code xpReward} est l'XP maximale gagnée avec 100 % de bonnes réponses. */
@Entity
@Table(name = "quiz", indexes = @Index(name = "idx_quiz_course", columnList = "course_id"))
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 20;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    protected Quiz() {
    }

    public Quiz(String courseId, String title, String description, int xpReward) {
        update(courseId, title, description, xpReward);
    }

    public void update(String courseId, String title, String description, int xpReward) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.xpReward = xpReward;
    }

    /** Remplace toutes les questions (les anciennes sont supprimées grâce à orphanRemoval). */
    public void replaceQuestions(List<QuizQuestion> newQuestions) {
        questions.clear();
        int position = 0;
        for (QuizQuestion question : newQuestions) {
            question.attachTo(this, position++);
            questions.add(question);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getXpReward() {
        return xpReward;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }
}
