package com.skillquest.gamification.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Profil joueur : solde d'XP, niveau et rang. L'identifiant est celui de l'utilisateur (auth-service). */
@Entity
@Table(name = "user_profile_gamification")
public class UserProfileGamification {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "total_xp", nullable = false)
    private int totalXP;

    @Column(name = "current_level", nullable = false)
    private int currentLevel = 1;

    /** Dernier rang calculé ; la valeur « live » est recalculée à la lecture. */
    @Column(name = "global_rank")
    private Integer globalRank;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfileGamification() {
    }

    public UserProfileGamification(UUID userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    /** Ajoute de l'XP et renvoie {@code true} si l'apprenant vient de passer un niveau. */
    public boolean addXP(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        int levelBefore = currentLevel;
        totalXP = Math.addExact(totalXP, amount);
        updatedAt = Instant.now();
        evaluateLevelProgression();
        return currentLevel > levelBefore;
    }

    /** Ajustement manuel par un administrateur (positif ou négatif) ; l'XP ne descend jamais sous 0. */
    public void adjustXP(int delta) {
        totalXP = Math.max(0, totalXP + delta);
        updatedAt = Instant.now();
        evaluateLevelProgression();
    }

    public void evaluateLevelProgression() {
        currentLevel = LevelCalculator.levelFor(totalXP);
    }

    public UUID getUserId() {
        return userId;
    }

    public int getTotalXP() {
        return totalXP;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public Integer getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(Integer globalRank) {
        this.globalRank = globalRank;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
