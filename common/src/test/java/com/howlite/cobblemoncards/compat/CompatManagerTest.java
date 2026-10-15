package com.howlite.cobblemoncards.compat;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.howlite.cobblemoncards.compat.CompatConfig.Integration.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatManagerTest {
    @Test
    void absentAddonsRegisterNoIntegrationAndInitializationRunsOnlyOnce() {
        AtomicInteger detections = new AtomicInteger();
        Set<String> queriedModIds = new LinkedHashSet<>();
        CompatManager.initialize(modId -> {
            detections.incrementAndGet();
            queriedModIds.add(modId);
            return false;
        });
        assertEquals(Set.of("rustlingspots", "cobblemonraiddens", "cobbledgacha", "cobblemonfishingloot"), queriedModIds);
        for (CompatConfig.Integration integration : CompatConfig.Integration.values()) {
            assertFalse(CompatManager.isDetected(integration));
        }
        CompatManager.initialize(modId -> {
            fail("Mod detection must not be repeated after initialization");
            return true;
        });
        assertEquals(4, detections.get());
        assertFalse(CompatManager.reward("fishing_water").isEnabled());
    }

    @Test
    void verifiedTablesHaveExactlyOneRulePerRewardExceptTwoFishingChoices() {
        Map<String, List<String>> expected = Map.ofEntries(
                Map.entry("cobblemonraiddens:raid/tier/tier_one", List.of("raid_1")),
                Map.entry("cobblemonraiddens:raid/tier/tier_two", List.of("raid_2")),
                Map.entry("cobblemonraiddens:raid/tier/tier_three", List.of("raid_3")),
                Map.entry("cobblemonraiddens:raid/tier/tier_four", List.of("raid_4")),
                Map.entry("cobblemonraiddens:raid/tier/tier_five", List.of("raid_5")),
                Map.entry("cobblemonraiddens:raid/tier/tier_six", List.of("raid_6")),
                Map.entry("cobblemonraiddens:raid/tier/tier_seven", List.of("raid_7")),
                Map.entry("cobbledgacha:gacha_capsules/capsule_a1", List.of("gacha_poke")),
                Map.entry("cobbledgacha:gacha_capsules/capsule_a2", List.of("gacha_great")),
                Map.entry("cobbledgacha:gacha_capsules/capsule_a3", List.of("gacha_ultra")),
                Map.entry("cobbledgacha:gacha_capsules/capsule_a4", List.of("gacha_master")),
                Map.entry("cobbledgacha:gacha_capsules/capsule_a5", List.of("gacha_cherish")),
                Map.entry("cobblemon:fishing/pokerod", List.of("fishing_water", "fishing_classic")));
        Set<String> rewardIds = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> entry : expected.entrySet()) {
            List<CompatReward> rules = CompatManager.rewardsForTable(ResourceLocation.parse(entry.getKey()));
            assertEquals(entry.getValue(), rules.stream().map(CompatReward::id).toList(), entry.getKey());
            for (CompatReward rule : rules) {
                assertTrue(rewardIds.add(rule.id()), "Duplicate reward ID: " + rule.id());
                assertSame(rule, CompatManager.reward(rule.id()));
            }
        }
        assertEquals(13, expected.size());
        assertEquals(14, rewardIds.size());
    }

    @Test
    void unrelatedAndNestedTablesCannotAccidentallyReceiveBoosterRules() {
        for (String id : List.of("minecraft:gameplay/fishing", "minecraft:gameplay/fishing/treasure",
                "cobblemon:fishing/pokerod_treasure", "cobblemon:fishing/pokerod_rare_treasure",
                "cobblemonraiddens:raid/tier/tier_eight", "cobblemonraiddens:blocks/raid_den",
                "cobbledgacha:gacha_capsules/capsule_a6", "pokeloot:blocks/poke_ball",
                "minecraft:blocks/stone", "differentmod:fishing/pokerod")) {
            assertTrue(CompatManager.rewardsForTable(ResourceLocation.parse(id)).isEmpty(), id);
        }
        assertTrue(CompatManager.rewardsForTable(null).isEmpty());
        assertNull(CompatManager.reward("unknown_reward"));
    }

    @Test
    void rewardRulesKeepVerifiedRarityProgressionAndCannotBeMutated() {
        assertEquals(BoosterRewardHelper.Selection.CLASSIC, CompatManager.reward("raid_1").selection());
        assertEquals(BoosterRewardHelper.Selection.GENERATION, CompatManager.reward("raid_3").selection());
        assertEquals(BoosterRewardHelper.Selection.THEMED, CompatManager.reward("raid_7").selection());
        assertEquals(BoosterRewardHelper.Selection.CLASSIC_OR_GENERATION, CompatManager.reward("gacha_great").selection());
        assertEquals(BoosterRewardHelper.Selection.GENERATION_OR_THEMED, CompatManager.reward("gacha_ultra").selection());
        assertEquals(List.of("water"), CompatManager.reward("fishing_water").selection().boosterTypes());
        assertEquals(FISHING_LOOT, CompatManager.reward("fishing_water").integration());
        List<CompatReward> fishing = CompatManager.rewardsForTable(ResourceLocation.parse("cobblemon:fishing/pokerod"));
        assertThrows(UnsupportedOperationException.class, () -> fishing.add(CompatManager.reward("raid_1")));
    }
}
