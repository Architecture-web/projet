package com.skillquest.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Mémorise qu'un étudiant a terminé un cours (événement {@code CourseCompletedEvent}).
 * Le certificat n'est délivré que si, en plus, la moyenne de ses quiz atteint le seuil : il peut donc
 * être émis plus tard, au moment où l'étudiant réussit enfin ses quiz.
 */
@Entity
@Table(name = "course_completion",
        uniqueConstraints = @UniqueConstraint(name = "uk_completion_user_course", columnNames = {"user_id", "course_id"}))
public class CourseCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "student_full_name", nullable = false)
    private String studentFullName;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    protected CourseCompletion() {
    }

    public CourseCompletion(UUID userId, String courseId, String studentFullName) {
        this.userId = userId;
        this.courseId = courseId;
        this.studentFullName = studentFullName;
    }

    public String getStudentFullName() {
        return studentFullName;
    }
}
