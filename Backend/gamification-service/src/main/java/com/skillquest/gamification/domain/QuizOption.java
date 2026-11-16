package com.skillquest.gamification.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_option")
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 300)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    protected QuizOption() {
    }

    public QuizOption(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    void attachTo(QuizQuestion question, int position) {
        this.question = question;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
