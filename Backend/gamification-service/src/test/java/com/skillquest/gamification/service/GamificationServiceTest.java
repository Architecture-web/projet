package com.skillquest.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.skillquest.gamification.dto.ApiDtos.LeaderboardEntry;
import com.skillquest.gamification.dto.ApiDtos.UserBadgeResponse;
import com.skillquest.gamification.exception.ConflictException;
import com.skillquest.gamification.exception.ResourceNotFoundException;
import com.skillquest.gamification.messaging.BadgeUnlockedEvent;
import com.skillquest.gamification.support.IntegrationTestBase;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class GamificationServiceTest extends IntegrationTestBase {

    @Autowired
    private GamificationService service;

    @Test
    void profileCannotBeCreatedTwice() {
        UUID user = UUID.randomUUID();
        var created = service.createProfile(user);

        assertThat(created.totalXP()).isZero();
        assertThat(created.currentLevel()).isEqualTo(1);
        assertThatThrownBy(() -> service.createProfile(user)).isInstanceOf(ConflictException.class);
    }

    @Test
    void addingXpComputesLevelAndUnlocksXpBadgesOnce() {
        UUID user = UUID.randomUUID();
        service.createProfile(user);

        service.addXP(user, 50);
        assertThat(codes(service.getUserBadges(user))).containsExactly("FIRST_STEPS");

        service.addXP(user, 50);
        service.addXP(user, 50);
        service.addXP(user, 50); // 200 XP au total
        assertThat(codes(service.getUserBadges(user))).containsExactly("FIRST_STEPS", "CODE_APPRENTICE");
        assertThat(service.getProfile(user).totalXP()).isEqualTo(200);
        assertThat(service.getProfile(user).currentLevel()).isEqualTo(2);
    }

    @Test
    void badgeUnlockedEventIsPublishedAfterCommit() {
        UUID user = UUID.randomUUID();
        service.addXP(user, 50); // le profil est créé à la volée

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("skillquest.events"), eq("gamification.badge.unlocked"),
                payload.capture());
        BadgeUnlockedEvent event = (BadgeUnlockedEvent) payload.getValue();
        assertThat(event.userId()).isEqualTo(user);
        assertThat(event.badgeCode()).isEqualTo("FIRST_STEPS");
    }

    @Test
    void noEventIsPublishedWhenNoBadgeIsUnlocked() {
        UUID user = UUID.randomUUID();
        service.addXP(user, 10);

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void redeliveredEventDoesNotCreditXpTwice() {
        UUID user = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        assertThat(service.addXPForEvent(eventId, user, 50)).isTrue();
        assertThat(service.addXPForEvent(eventId, user, 50)).isFalse();

        assertThat(service.getProfile(user).totalXP()).isEqualTo(50);
    }

    @Test
    void concurrentXpAdditionsAreNeverLost() throws Exception {
        UUID user = UUID.randomUUID();
        service.createProfile(user);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 40; i++) {
            futures.add(pool.submit(() -> service.addXP(user, 10)));
        }
        for (var f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(service.getProfile(user).totalXP()).isEqualTo(400);
        assertThat(codes(service.getUserBadges(user))).containsExactly("FIRST_STEPS", "CODE_APPRENTICE");
    }

    @Test
    void leaderboardIsSortedAndTiesShareARank() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        service.addXP(a, 10);
        service.addXP(b, 80);
        service.addXP(c, 80);
        service.addXP(d, 30);

        List<LeaderboardEntry> board = service.leaderboard(50);

        assertThat(board).extracting(LeaderboardEntry::totalXP).containsExactly(80, 80, 30, 10);
        assertThat(board).extracting(LeaderboardEntry::rank).containsExactly(1, 1, 3, 4);
        assertThat(service.getProfile(d).globalRank()).isEqualTo(3);
        assertThat(service.leaderboard(2)).hasSize(2);
    }

    @Test
    void badgesOfUnknownUserAreNotFound() {
        assertThatThrownBy(() -> service.getUserBadges(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static List<String> codes(List<UserBadgeResponse> badges) {
        return badges.stream().map(UserBadgeResponse::codeName).toList();
    }
}
