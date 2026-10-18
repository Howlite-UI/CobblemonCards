package com.howlite.cobblemoncards.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Shared, immutable eligibility rules for booster chest loot on both loaders.
 *
 * <p>A loot table ID or CHEST parameter set cannot prove that a table is used by a
 * naturally generated structure. Automatic matching therefore retains only the
 * established {@code chests/} path convention and exact verified vanilla container
 * IDs; pack authors must verify other tables before explicitly adding them. The
 * container block need not be a chest. ALL_PARAMS is accepted because
 * Minecraft uses it when a loot table JSON omits its type. Known non-container
 * parameter sets and protected source paths are rejected even when configured.</p>
 */
public final class BoosterLootTableMatcher {
    // Verified LootTable references in vanilla 1.21.1 structure NBT, not name guesses.
    private static final Set<ResourceLocation> VERIFIED_STRUCTURE_TABLES = Set.of(
            ResourceLocation.withDefaultNamespace("dispensers/trial_chambers/corridor"),
            ResourceLocation.withDefaultNamespace("dispensers/trial_chambers/chamber"));

    // These declare minecraft:chest despite being gameplay rewards/nested helpers.
    // reward itself is also used by natural chests: keep its one pool and reject vault
    // callers at generation time instead. Pot inventory has only one slot; an extra
    // pool could discard its original item, so it cannot use this additive mechanism.
    // Cobblemon's verified LootInjector helpers are called by vanilla parent tables;
    // give the parent its booster pool without adding a second chance in the helper.
    private static final Set<ResourceLocation> PROTECTED_SOURCE_TABLES = Set.of(
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_common"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_rare"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_unique"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous_common"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous_rare"),
            ResourceLocation.withDefaultNamespace("chests/trial_chambers/reward_ominous_unique"),
            ResourceLocation.withDefaultNamespace("pots/trial_chambers/corridor"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/abandoned_mineshaft"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/ancient_city"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/bastion_bridge"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/bastion_hoglin_stable"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/bastion_other"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/bastion_treasure"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/end_city_treasure"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/igloo_chest"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/jungle_temple"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/nether_bridge"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/pillager_outpost"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/shipwreck_supply"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/simple_dungeon"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/spawn_bonus_chest"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/stronghold_corridor"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/village_house"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "injection/chests/woodland_mansion"));

    private final Set<ResourceLocation> additionalTables;
    private final Set<ResourceLocation> excludedTables;

    private BoosterLootTableMatcher(Set<ResourceLocation> additionalTables,
                                    Set<ResourceLocation> excludedTables) {
        this.additionalTables = Set.copyOf(additionalTables);
        this.excludedTables = Set.copyOf(excludedTables);
    }

    /** Parses configuration once; missing, syntactically valid IDs need no registry lookup. */
    public static BoosterLootTableMatcher create(List<String> additionalIds,
                                                 List<String> excludedIds,
                                                 Consumer<String> warning) {
        Consumer<String> warnings = warning == null ? ignored -> {} : warning;
        return new BoosterLootTableMatcher(
                parseIds(additionalIds, "additionalBoosterLootTables", true, warnings),
                parseIds(excludedIds, "excludedBoosterLootTables", false, warnings));
    }

    public Decision match(ResourceLocation id, LootContextParamSet paramSet) {
        if (id == null) return new Decision(false, Reason.INVALID);
        if (excludedTables.contains(id)) return new Decision(false, Reason.EXCLUDED);
        if (isProtectedPath(id)) return new Decision(false, Reason.PROTECTED_PATH);
        if (PROTECTED_SOURCE_TABLES.contains(id)) return new Decision(false, Reason.PROTECTED_SOURCE);
        if (paramSet != LootContextParamSets.CHEST && paramSet != LootContextParamSets.ALL_PARAMS) {
            return new Decision(false, Reason.UNSUPPORTED_TYPE);
        }
        if (additionalTables.contains(id)) return new Decision(true, Reason.CONFIGURATION);
        if (VERIFIED_STRUCTURE_TABLES.contains(id)) return new Decision(true, Reason.VERIFIED_STRUCTURE);
        String path = id.getPath();
        if (path.startsWith("chests/") || path.contains("/chests/")) {
            return new Decision(true, Reason.AUTOMATIC);
        }
        return new Decision(false, Reason.UNMATCHED);
    }

    private static Set<ResourceLocation> parseIds(List<String> entries, String setting,
                                                 boolean protectSources, Consumer<String> warning) {
        Set<ResourceLocation> parsed = new HashSet<>();
        if (entries == null) {
            warning.accept("Ignoring null " + setting + "; expected a list of namespace:path loot table IDs.");
            return parsed;
        }
        for (Object entry : entries) {
            String value = entry instanceof String text ? text.trim() : null;
            int separator = value == null ? -1 : value.indexOf(':');
            ResourceLocation id = value == null || separator <= 0 || separator == value.length() - 1
                    || separator != value.lastIndexOf(':') ? null : ResourceLocation.tryParse(value);
            if (id == null) {
                warning.accept("Ignoring invalid " + setting + " entry '" + entry
                        + "'; expected an explicit namespace:path loot table ID.");
            } else if (protectSources && (isProtectedPath(id) || PROTECTED_SOURCE_TABLES.contains(id))) {
                warning.accept("Ignoring protected " + setting + " entry '" + id
                        + "'; block/entity/fishing/spawner loot, protected reward helpers and single-slot pots"
                        + " cannot receive structure boosters.");
            } else {
                parsed.add(id);
            }
        }
        return parsed;
    }

    private static boolean isProtectedPath(ResourceLocation id) {
        for (String segment : id.getPath().split("/")) {
            if (segment.equals("blocks") || segment.equals("entities")
                    || segment.equals("gameplay") || segment.equals("fishing") || segment.equals("spawners")) {
                return true;
            }
        }
        return false;
    }

    public record Decision(boolean eligible, Reason reason) {}

    public enum Reason {
        AUTOMATIC,
        VERIFIED_STRUCTURE,
        CONFIGURATION,
        EXCLUDED,
        PROTECTED_PATH,
        PROTECTED_SOURCE,
        UNSUPPORTED_TYPE,
        UNMATCHED,
        INVALID
    }
}
