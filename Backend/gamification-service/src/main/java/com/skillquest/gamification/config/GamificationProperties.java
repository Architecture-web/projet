package com.skillquest.gamification.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamification")
public record GamificationProperties(
        Xp xp,
        CourseService courseService,
        CertificateSettings certificate,
        Messaging messaging) {

    public record Xp(int codeSolutionPassed) {
    }

    public record CourseService(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }

    public record CertificateSettings(String secret, String verificationBaseUrl) {
    }

    public record Messaging(
            String exchange,
            String deadLetterExchange,
            String deadLetterQueue,
            String badgeUnlockedRoutingKey,
            String codeSolutionQueue,
            String codeSolutionRoutingKey,
            String courseCompletedQueue,
            String courseCompletedRoutingKey) {
    }
}
