package com.howlite.cobblemoncards.compat;

/** Percentages are validated at the reward boundary, including hand-edited server configs. */
public final class RewardProbability {
    private RewardProbability() {}

    public static double clamp(double percentage) {
        return Double.isFinite(percentage) ? Math.clamp(percentage, 0.0, 100.0) : 0.0;
    }

    public static boolean succeeds(double percentage, double roll) {
        return Double.isFinite(roll) && roll >= 0.0 && roll < 1.0
                && roll < clamp(percentage) / 100.0;
    }
}
