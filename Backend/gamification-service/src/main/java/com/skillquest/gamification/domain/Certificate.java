package com.skillquest.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Certificat de fin de cours. Un seul certificat par couple (utilisateur, cours). */
@Entity
@Table(name = "certificate",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_certificate_user_course", columnNames = {"user_id", "course_id"}),
                @UniqueConstraint(name = "uk_certificate_hash", columnNames = "certificate_hash")
        },
        indexes = @Index(name = "idx_certificate_user", columnList = "user_id"))
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "student_full_name", nullable = false)
    private String studentFullName;

    @Column(name = "course_title", nullable = false)
    private String courseTitle;

    @Column(name = "certificate_hash", nullable = false, length = 64)
    private String certificateHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected Certificate() {
    }

    public Certificate(UUID userId, String courseId, String studentFullName, String courseTitle, Instant issuedAt) {
        this.userId = userId;
        this.courseId = courseId;
        this.studentFullName = studentFullName;
        this.courseTitle = courseTitle;
        this.issuedAt = issuedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getStudentFullName() {
        return studentFullName;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public String getCertificateHash() {
        return certificateHash;
    }

    public void setCertificateHash(String certificateHash) {
        this.certificateHash = certificateHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
