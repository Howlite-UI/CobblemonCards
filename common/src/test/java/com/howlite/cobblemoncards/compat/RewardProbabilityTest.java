package com.howlite.cobblemoncards.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class RewardProbabilityTest {
    @Test
    void zeroAndGuaranteedChancesRespectValidRandomRange() {
        for (double roll : new double[]{0.0, 0.5, Math.nextDown(1.0)}) {
            assertFalse(RewardProbability.succeeds(0.0, roll));
            assertTrue(RewardProbability.succeeds(100.0, roll));
        }
    }

    @Test
    void editedChancesClampAtBothLimits() {
        assertEquals(0.0, RewardProbability.clamp(-10.0));
        assertEquals(100.0, RewardProbability.clamp(150.0));
        assertFalse(RewardProbability.succeeds(-10.0, 0.0));
        assertTrue(RewardProbability.succeeds(150.0, Math.nextDown(1.0)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidConfiguredChanceCannotGrantRewards(double percentage) {
        assertEquals(0.0, RewardProbability.clamp(percentage));
        assertFalse(RewardProbability.succeeds(percentage, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, 1.0, 2.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidRandomRollCannotGrantEvenGuaranteedReward(double roll) {
        assertFalse(RewardProbability.succeeds(100.0, roll));
    }

    @Test
    void chanceUsesStrictThresholdWithoutRoundingSmallPercentages() {
        assertTrue(RewardProbability.succeeds(3.0, Math.nextDown(0.03)));
        assertFalse(RewardProbability.succeeds(3.0, 0.03));
        assertFalse(RewardProbability.succeeds(3.0, Math.nextUp(0.03)));
        assertTrue(RewardProbability.succeeds(0.01, 0.000099));
        assertFalse(RewardProbability.succeeds(0.01, 0.0001));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 3.0, 5.0, 8.0, 10.0, 15.0, 20.0, 30.0, 40.0, 50.0})
    void balancedDefaultsAcceptExactlyTheirShareOfUniformSamples(double percentage) {
        int successes = 0;
        for (int i = 0; i < 10_000; i++) {
            if (RewardProbability.succeeds(percentage, (i + 0.5) / 10_000.0)) successes++;
        }
        assertEquals((int) (percentage * 100), successes);
    }
}
