package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.howlite.cobblemoncards.compat.BoosterRewardHelper.Selection.*;
import static com.howlite.cobblemoncards.compat.CompatConfig.Integration.*;

/** Exact, verified reward tables only. Optional addon classes are never linked from loader entrypoints. */
public final class CompatManager {
    private static final Map<ResourceLocation, List<CompatReward>> TABLE_REWARDS = createRules();
    private static Set<CompatConfig.Integration> detected = Set.of();
    private static boolean initialized;

    private CompatManager() {}

    public static synchronized void initialize(Predicate<String> isModLoaded) {
        if (initialized) return;
        EnumSet<CompatConfig.Integration> present = EnumSet.noneOf(CompatConfig.Integration.class);
        for (CompatConfig.Integration integration : CompatConfig.Integration.values()) {
            if (isModLoaded.test(integration.modId())) {
                present.add(integration);
                CobblemonCards.LOGGER.info("Optional booster integration detected: {} (enabled={})",
                        integration.modId(), CompatConfig.enabled(integration));
            }
        }
        detected = Set.copyOf(present);
        initialized = true;
        if (present.contains(RUSTLING_SPOTS)) RustlingSpotsCompat.register();
    }

    public static boolean isDetected(CompatConfig.Integration integration) { return detected.contains(integration); }

    public static List<CompatReward> rewardsForTable(ResourceLocation table) {
        return table == null ? List.of() : TABLE_REWARDS.getOrDefault(table, List.of());
    }

    public static CompatReward reward(String id) {
        return TABLE_REWARDS.values().stream().flatMap(List::stream)
                .filter(reward -> reward.id().equals(id)).findFirst().orElse(null);
    }

    private static Map<ResourceLocation, List<CompatReward>> createRules() {
        Map<ResourceLocation, List<CompatReward>> rules = new LinkedHashMap<>();
        String[] tiers = {"one", "two", "three", "four", "five", "six", "seven"};
        for (int i = 0; i < tiers.length; i++) {
            add(rules, "cobblemonraiddens:raid/tier/tier_" + tiers[i],
                    new CompatReward("raid_" + (i + 1), RAID_DENS,
                            i < 2 ? CLASSIC : i < 4 ? GENERATION : THEMED));
        }
        String[] capsules = {"poke", "great", "ultra", "master", "cherish"};
        BoosterRewardHelper.Selection[] selections = {
                CLASSIC, CLASSIC_OR_GENERATION, GENERATION_OR_THEMED, THEMED, THEMED};
        for (int i = 0; i < capsules.length; i++) {
            add(rules, "cobbledgacha:gacha_capsules/capsule_a" + (i + 1),
                    new CompatReward("gacha_" + capsules[i], COBBLED_GACHA, selections[i]));
        }
        add(rules, "cobblemon:fishing/pokerod", new CompatReward("fishing_water", FISHING_LOOT,
                BoosterRewardHelper.Selection.fromToken("water")));
        add(rules, "cobblemon:fishing/pokerod", new CompatReward("fishing_classic", FISHING_LOOT, CLASSIC));
        rules.replaceAll((id, rewards) -> List.copyOf(rewards));
        return Map.copyOf(rules);
    }

    private static void add(Map<ResourceLocation, List<CompatReward>> rules, String table, CompatReward reward) {
        rules.computeIfAbsent(ResourceLocation.parse(table), ignored -> new ArrayList<>()).add(reward);
    }
}
