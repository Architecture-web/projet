package com.skillquest.gamification.messaging;

import java.time.Instant;
import java.util.UUID;

/** Émis par course-service quand un apprenant termine un cours. */
public record CourseCompletedEvent(
        UUID eventId,
        UUID userId,
        String courseId,
        String studentFullName,
        Instant occurredAt) {
}
