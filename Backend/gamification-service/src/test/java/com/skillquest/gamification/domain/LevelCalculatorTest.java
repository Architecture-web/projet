package com.skillquest.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelCalculatorTest {

    @ParameterizedTest
    @CsvSource({"0,1", "99,1", "100,2", "399,2", "400,3", "899,3", "900,4", "10000,11"})
    void levelFollowsXpThresholds(int xp, int expectedLevel) {
        assertThat(LevelCalculator.levelFor(xp)).isEqualTo(expectedLevel);
    }

    @Test
    void xpToNextLevelIsDistanceToNextThreshold() {
        assertThat(LevelCalculator.xpToNextLevel(0)).isEqualTo(100);
        assertThat(LevelCalculator.xpToNextLevel(150)).isEqualTo(250);
        assertThat(LevelCalculator.xpToNextLevel(400)).isEqualTo(500);
    }

    @Test
    void profileReportsLevelUp() {
        UserProfileGamification profile = new UserProfileGamification(java.util.UUID.randomUUID());
        assertThat(profile.addXP(50)).isFalse();
        assertThat(profile.addXP(50)).isTrue();
        assertThat(profile.getCurrentLevel()).isEqualTo(2);
        assertThat(profile.getTotalXP()).isEqualTo(100);
    }

    @Test
    void profileRejectsNonPositiveXp() {
        UserProfileGamification profile = new UserProfileGamification(java.util.UUID.randomUUID());
        assertThatThrownBy(() -> profile.addXP(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile.addXP(-5)).isInstanceOf(IllegalArgumentException.class);
    }
}
