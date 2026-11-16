package com.skillquest.gamification.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 500)
    private String text;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizOption> options = new ArrayList<>();

    protected QuizQuestion() {
    }

    public QuizQuestion(String text) {
        this.text = text;
    }

    void attachTo(Quiz quiz, int position) {
        this.quiz = quiz;
        this.position = position;
    }

    public void addOption(QuizOption option) {
        option.attachTo(this, options.size());
        options.add(option);
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public List<QuizOption> getOptions() {
        return options;
    }
}
