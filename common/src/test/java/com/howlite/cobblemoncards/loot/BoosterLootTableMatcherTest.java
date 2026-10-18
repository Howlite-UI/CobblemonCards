package com.howlite.cobblemoncards.loot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.howlite.cobblemoncards.loot.BoosterLootTableMatcher.Reason.*;
import static org.junit.jupiter.api.Assertions.*;

class BoosterLootTableMatcherTest {
    private final BoosterLootTableMatcher defaults = matcher(List.of(), List.of());

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:chests/simple_dungeon", "minecraft:chests/ancient_city",
            "minecraft:chests/trial_chambers/entrance", "minecraft:chests/trial_chambers/corridor",
            "minecraft:chests/trial_chambers/intersection", "minecraft:chests/trial_chambers/intersection_barrel",
            "minecraft:chests/trial_chambers/supply", "minecraft:chests/trial_chambers/reward",
            "examplemod:structures/chests/ruins"
    })
    void standardChestPathsRemainAutomatic(String table) {
        assertDecision(defaults, table, LootContextParamSets.CHEST, true, AUTOMATIC);
        assertDecision(defaults, table, LootContextParamSets.ALL_PARAMS, true, AUTOMATIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:blocks/chest", "minecraft:blocks/trapped_chest", "minecraft:blocks/ender_chest",
            "minecraft:blocks/barrel", "minecraft:blocks/dispenser", "minecraft:blocks/decorated_pot",
            "minecraft:entities/zombie", "minecraft:gameplay/fishing/treasure",
            "examplemod:structures/blocks/chests/reward", "examplemod:chests/entities/reward",
            "examplemod:chests/gameplay/reward", "examplemod:loot/fishing/chests/reward"
    })
    void protectedSourcesCannotReceiveBoostersEvenIfAllowlistedOrMislabeledAsChests(String table) {
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(List.of(table), List.of(), warnings::add);
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, PROTECTED_PATH);
        assertDecision(configured, table, LootContextParamSets.ALL_PARAMS, false, PROTECTED_PATH);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("protected"));
        assertTrue(warnings.getFirst().contains(table));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:dispensers/trial_chambers/corridor", "minecraft:dispensers/trial_chambers/chamber"
    })
    void verifiedTrialChamberDispensersRemainSubjectToExclusionsAndTypes(String table) {
        assertDecision(defaults, table, LootContextParamSets.CHEST, true, VERIFIED_STRUCTURE);
        assertDecision(defaults, table, LootContextParamSets.ALL_PARAMS, true, VERIFIED_STRUCTURE);
        BoosterLootTableMatcher excluded = matcher(List.of(table), List.of(table));
        assertDecision(excluded, table, LootContextParamSets.CHEST, false, EXCLUDED);
        assertDecision(defaults, table, LootContextParamSets.BLOCK, false, UNSUPPORTED_TYPE);
        assertDecision(defaults, table, LootContextParamSets.ENTITY, false, UNSUPPORTED_TYPE);
        assertDecision(defaults, table, LootContextParamSets.FISHING, false, UNSUPPORTED_TYPE);
        BoosterLootTableMatcher configured = matcher(List.of(table), List.of());
        assertDecision(configured, table, LootContextParamSets.BLOCK, false, UNSUPPORTED_TYPE);
        assertDecision(configured, table, LootContextParamSets.ENTITY, false, UNSUPPORTED_TYPE);
        assertDecision(configured, table, LootContextParamSets.FISHING, false, UNSUPPORTED_TYPE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:chests/trial_chambers/reward_common", "minecraft:chests/trial_chambers/reward_rare",
            "minecraft:chests/trial_chambers/reward_unique", "minecraft:chests/trial_chambers/reward_ominous",
            "minecraft:chests/trial_chambers/reward_ominous_common",
            "minecraft:chests/trial_chambers/reward_ominous_rare",
            "minecraft:chests/trial_chambers/reward_ominous_unique",
            "minecraft:pots/trial_chambers/corridor"
    })
    void protectedVaultAndSingleSlotPotTablesCannotBeAllowlisted(String table) {
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(List.of(table), List.of(), warnings::add);
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, PROTECTED_SOURCE);
        assertDecision(configured, table, LootContextParamSets.ALL_PARAMS, false, PROTECTED_SOURCE);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("protected"));
        assertTrue(warnings.getFirst().contains(table));
        BoosterLootTableMatcher excluded = matcher(List.of(), List.of(table));
        assertDecision(excluded, table, LootContextParamSets.CHEST, false, EXCLUDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:spawners/trial_chamber/consumables", "minecraft:spawners/trial_chamber/key",
            "minecraft:spawners/trial_chamber/items_to_drop_when_ominous",
            "minecraft:spawners/ominous/trial_chamber/consumables", "minecraft:spawners/ominous/trial_chamber/key",
            "minecraft:spawners/ominous/trial_chamber/items_to_drop_when_ominous",
            "examplemod:structures/spawners/chests/reward"
    })
    void spawnerLootIsProtectedEvenWithAChestTypeAndExplicitAllowlist(String table) {
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(List.of(table), List.of(), warnings::add);
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, PROTECTED_PATH);
        assertDecision(configured, table, LootContextParamSets.ALL_PARAMS, false, PROTECTED_PATH);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains(table));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:dispensers/trial_chambers/water", "minecraft:dispensers/trial_chambers/corridor/extra",
            "minecraft:barrels/trial_chambers/corridor", "examplemod:pots/trial_chambers/corridor",
            "examplemod:dispensers/trial_chambers/corridor", "examplemod:dispensers/trial_chambers/chamber",
            "examplemod:barrels/trial_chambers/intersection", "examplemod:trial_chambers/treasure"
    })
    void verifiedVanillaExceptionsDoNotCreateBroadContainerOrTrialPathMatching(String table) {
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, UNMATCHED);
        BoosterLootTableMatcher configured = matcher(List.of(table), List.of());
        assertDecision(configured, table, LootContextParamSets.CHEST, true, CONFIGURATION);
    }

    @Test
    void protectedVanillaRewardIdsDoNotBlockUnrelatedModNamespaces() {
        assertDecision(defaults, "examplemod:chests/trial_chambers/reward_common",
                LootContextParamSets.CHEST, true, AUTOMATIC);
        assertDecision(defaults, "examplemod:pots/trial_chambers/corridor",
                LootContextParamSets.CHEST, false, UNMATCHED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abandoned_mineshaft", "ancient_city", "bastion_bridge", "bastion_hoglin_stable",
            "bastion_other", "bastion_treasure", "end_city_treasure", "igloo_chest", "jungle_temple",
            "nether_bridge", "pillager_outpost", "shipwreck_supply", "simple_dungeon", "spawn_bonus_chest",
            "stronghold_corridor", "village_house", "woodland_mansion"
    })
    void cobblemonNestedInjectionTablesCannotAddASecondBoosterRoll(String suffix) {
        String table = "cobblemon:injection/chests/" + suffix;
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(List.of(table), List.of(), warnings::add);
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, PROTECTED_SOURCE);
        assertDecision(configured, table, LootContextParamSets.ALL_PARAMS, false, PROTECTED_SOURCE);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("protected"));
        assertTrue(warnings.getFirst().contains(table));

        BoosterLootTableMatcher excluded = matcher(List.of(), List.of(table));
        assertDecision(excluded, table, LootContextParamSets.CHEST, false, EXCLUDED);
        assertDecision(defaults, "examplemod:injection/chests/" + suffix,
                LootContextParamSets.CHEST, true, AUTOMATIC);
        if (!suffix.equals("village_house")) {
            assertDecision(defaults, "minecraft:chests/" + suffix,
                    LootContextParamSets.CHEST, true, AUTOMATIC);
        }
    }

    @Test
    void cobblemonHelperProtectionUsesVerifiedExactIds() {
        assertDecision(defaults, "cobblemon:injection/chests/custom_structure",
                LootContextParamSets.CHEST, true, AUTOMATIC);
        assertDecision(defaults, "cobblemon:injection/chests/simple_dungeon/extra",
                LootContextParamSets.CHEST, true, AUTOMATIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "examplemod:structures/ruins/treasure", "examplemod:loot/chest_rewards",
            "examplemod:loot_tables/dungeon_treasure", "examplemod:ancient_city/rewards",
            "examplemod:loot/dungeon_chest", "examplemod:chest", "examplemod:chests"
    })
    void genericKeywordsRequireAnExplicitVerifiedEntry(String table) {
        assertDecision(defaults, table, LootContextParamSets.CHEST, false, UNMATCHED);
        BoosterLootTableMatcher configured = matcher(List.of(table), List.of());
        assertDecision(configured, table, LootContextParamSets.CHEST, true, CONFIGURATION);
    }

    @Test
    void exclusionOverridesBothAutomaticAndConfiguredTables() {
        String automatic = "examplemod:chests/special_reward";
        String configured = "examplemod:structures/ruins/treasure";
        BoosterLootTableMatcher matcher = matcher(List.of(automatic, configured), List.of(automatic, configured));
        assertDecision(matcher, automatic, LootContextParamSets.CHEST, false, EXCLUDED);
        assertDecision(matcher, configured, LootContextParamSets.CHEST, false, EXCLUDED);
    }

    @Test
    void nonContainerTypesCannotBeAllowedThroughTheirNameOrConfiguration() {
        String table = "examplemod:chests/rewards";
        BoosterLootTableMatcher configured = matcher(List.of(table), List.of());
        for (LootContextParamSet paramSet : List.of(LootContextParamSets.BLOCK, LootContextParamSets.ENTITY,
                LootContextParamSets.FISHING, LootContextParamSets.GIFT, LootContextParamSets.PIGLIN_BARTER,
                LootContextParamSets.ADVANCEMENT_REWARD, LootContextParamSets.EMPTY)) {
            assertDecision(defaults, table, paramSet, false, UNSUPPORTED_TYPE);
            assertDecision(configured, table, paramSet, false, UNSUPPORTED_TYPE);
        }
        assertDecision(configured, table, null, false, UNSUPPORTED_TYPE);
    }

    @Test
    void malformedAndUnqualifiedEntriesAreIgnoredWithUsefulWarnings() {
        List<String> invalid = Arrays.asList(null, "", " ", "chests/reward", ":chests/reward", "examplemod:",
                "ExampleMod:chests/reward", "examplemod:chests/Reward", "examplemod:two:colons", "examplemod:bad path");
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(invalid, invalid, warnings::add);
        assertEquals(invalid.size() * 2, warnings.size());
        assertTrue(warnings.stream().allMatch(message -> message.contains("namespace:path")));
        assertTrue(warnings.stream().anyMatch(message -> message.contains("additionalBoosterLootTables")));
        assertTrue(warnings.stream().anyMatch(message -> message.contains("excludedBoosterLootTables")));
        assertEquals(new BoosterLootTableMatcher.Decision(false, INVALID), configured.match(null, LootContextParamSets.CHEST));
        assertDecision(configured, "minecraft:chests/reward", LootContextParamSets.CHEST, true, AUTOMATIC);
    }

    @Test
    void nullListsAreSafelyIgnoredAndWarned() {
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(null, null, warnings::add);
        assertEquals(2, warnings.size());
        assertDecision(configured, "minecraft:chests/simple_dungeon", LootContextParamSets.CHEST, true, AUTOMATIC);
    }

    @Test
    void duplicatesAndMissingTableIdsDoNotCreateAdditionalMatchesOrRequireRegistryLookups() {
        String missing = "not_installed:structures/treasure";
        List<String> warnings = new ArrayList<>();
        BoosterLootTableMatcher configured = BoosterLootTableMatcher.create(
                List.of(missing, missing, " " + missing + " "), List.of(), warnings::add);
        assertTrue(warnings.isEmpty());
        for (int attempt = 0; attempt < 3; attempt++) {
            assertDecision(configured, missing, LootContextParamSets.CHEST, true, CONFIGURATION);
        }
        assertDecision(defaults, missing, LootContextParamSets.CHEST, false, UNMATCHED);
    }

    @Test
    void matcherSnapshotsListsUntilTheNextReload() {
        String custom = "examplemod:structures/treasure";
        String vanilla = "minecraft:chests/ancient_city";
        List<String> additional = new ArrayList<>(List.of(custom));
        List<String> excluded = new ArrayList<>(List.of(vanilla));
        BoosterLootTableMatcher configured = matcher(additional, excluded);
        additional.clear();
        excluded.clear();
        assertDecision(configured, custom, LootContextParamSets.CHEST, true, CONFIGURATION);
        assertDecision(configured, vanilla, LootContextParamSets.CHEST, false, EXCLUDED);
        BoosterLootTableMatcher reloaded = matcher(additional, excluded);
        assertDecision(reloaded, custom, LootContextParamSets.CHEST, false, UNMATCHED);
        assertDecision(reloaded, vanilla, LootContextParamSets.CHEST, true, AUTOMATIC);
    }

    private static BoosterLootTableMatcher matcher(List<String> additional, List<String> excluded) {
        return BoosterLootTableMatcher.create(additional, excluded, message -> fail(message));
    }

    private static void assertDecision(BoosterLootTableMatcher matcher, String table,
                                       LootContextParamSet type, boolean expected,
                                       BoosterLootTableMatcher.Reason reason) {
        assertEquals(new BoosterLootTableMatcher.Decision(expected, reason),
                matcher.match(ResourceLocation.parse(table), type), table);
    }
}
