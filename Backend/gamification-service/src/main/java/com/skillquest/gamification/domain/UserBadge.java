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
import jakarta.persistence.UniqueConstraint;

/** Un badge débloqué par un utilisateur. La contrainte unique empêche toute double attribution. */
@Entity
@Table(name = "user_badge",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_badge", columnNames = {"user_id", "badge_id"}),
        indexes = @Index(name = "idx_user_badge_user", columnList = "user_id"))
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    protected UserBadge() {
    }

    public UserBadge(UUID userId, Badge badge) {
        this.userId = userId;
        this.badge = badge;
        this.unlockedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Badge getBadge() {
        return badge;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }
}
