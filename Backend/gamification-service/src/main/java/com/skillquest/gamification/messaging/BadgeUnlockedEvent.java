package com.skillquest.gamification.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement publié sur {@code skillquest.events} (routing key {@code gamification.badge.unlocked})
 * quand un apprenant débloque un badge.
 */
public record BadgeUnlockedEvent(
        UUID eventId,
        UUID userId,
        Long badgeId,
        String badgeCode,
        String badgeTitle,
        int totalXP,
        Instant unlockedAt) {
}
