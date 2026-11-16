package com.skillquest.gamification.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Émis par code-execution-service quand une solution passe tous les tests.
 * {@code eventId} (optionnel) permet d'ignorer une re-livraison du même message.
 */
public record CodeSolutionPassedEvent(
        UUID eventId,
        UUID userId,
        String challengeId,
        Instant occurredAt) {
}
