package com.skillquest.gamification.domain;

/**
 * Paliers de progression : le niveau N demande {@code 100 * (N-1)^2} XP
 * (niveau 2 = 100 XP, niveau 3 = 400 XP, niveau 4 = 900 XP, ...).
 */
public final class LevelCalculator {

    private static final int XP_UNIT = 100;

    private LevelCalculator() {
    }

    public static int levelFor(int totalXP) {
        if (totalXP < 0) {
            throw new IllegalArgumentException("totalXP must be >= 0");
        }
        return 1 + (int) Math.sqrt(totalXP / XP_UNIT);
    }

    public static int xpRequiredForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        int steps = level - 1;
        return XP_UNIT * steps * steps;
    }

    public static int xpToNextLevel(int totalXP) {
        return xpRequiredForLevel(levelFor(totalXP) + 1) - totalXP;
    }
}
