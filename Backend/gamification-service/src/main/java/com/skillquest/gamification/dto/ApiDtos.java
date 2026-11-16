package com.skillquest.gamification.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Contrats JSON exposés par l'API REST. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record CreateProfileRequest(@NotNull(message = "userId is required") UUID userId) {
    }

    public record ProfileResponse(UUID userId, int totalXP, int currentLevel, int xpToNextLevel,
                                  int globalRank, Instant updatedAt) {
    }

    public record LeaderboardEntry(int rank, UUID userId, int totalXP, int currentLevel) {
    }

    public record BadgeResponse(Long id, String codeName, String title, String description, String iconUrl,
                                Integer requiredXP) {
    }

    public record UserBadgeResponse(Long badgeId, String codeName, String title, String description,
                                    String iconUrl, Instant unlockedAt) {
    }

    public record CertificateResponse(UUID id, UUID userId, String courseId, String studentFullName,
                                      String courseTitle, String certificateHash, Instant issuedAt) {
    }

    /** Profil certifié consommé par le job-board-service. */
    public record CertifiedProfileResponse(UUID userId, int totalXP, int currentLevel,
                                           List<UserBadgeResponse> badges,
                                           List<CertificateResponse> certificates) {
    }

    public record VerificationResponse(boolean valid, UUID certificateId, String studentFullName,
                                       String courseTitle, Instant issuedAt, String certificateHash) {
    }
}
