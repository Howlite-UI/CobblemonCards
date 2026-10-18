package com.howlite.cobblemoncards.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoosterLootConfigTest {
    @TempDir
    Path temporaryDirectory;

    private List<String> originalAdditional;
    private List<String> originalExcluded;
    private boolean originalDebug;
    private boolean originalEnabled;
    private float originalChance;

    @BeforeEach
    void saveConfig() {
        originalAdditional = CobblemonCardsConfig.additionalBoosterLootTables;
        originalExcluded = CobblemonCardsConfig.excludedBoosterLootTables;
        originalDebug = CobblemonCardsConfig.debugBoosterLootTables;
        originalEnabled = CobblemonCardsConfig.enableBoosterChestSpawn;
        originalChance = CobblemonCardsConfig.boosterChestSpawnChance;
    }

    @AfterEach
    void restoreConfig() {
        CobblemonCardsConfig.additionalBoosterLootTables = originalAdditional;
        CobblemonCardsConfig.excludedBoosterLootTables = originalExcluded;
        CobblemonCardsConfig.debugBoosterLootTables = originalDebug;
        CobblemonCardsConfig.enableBoosterChestSpawn = originalEnabled;
        CobblemonCardsConfig.boosterChestSpawnChance = originalChance;
    }

    @Test
    void compatibilityDefaultsKeepTheExistingTwoPercentChance() {
        assertTrue(CobblemonCardsConfig.enableBoosterChestSpawn);
        assertEquals(2.0f, CobblemonCardsConfig.boosterChestSpawnChance);
        assertTrue(CobblemonCardsConfig.additionalBoosterLootTables.isEmpty());
        assertTrue(CobblemonCardsConfig.excludedBoosterLootTables.isEmpty());
        assertFalse(CobblemonCardsConfig.debugBoosterLootTables);
    }

    @Test
    void midnightConfigPersistsAndLoadsListsDebugAndExistingSettings() throws Exception {
        TemporaryConfig config = temporaryConfig();
        CobblemonCardsConfig.additionalBoosterLootTables = new ArrayList<>(List.of("examplemod:structures/treasure"));
        CobblemonCardsConfig.excludedBoosterLootTables = new ArrayList<>(List.of("examplemod:chests/special_reward"));
        CobblemonCardsConfig.debugBoosterLootTables = true;
        CobblemonCardsConfig.enableBoosterChestSpawn = false;
        CobblemonCardsConfig.boosterChestSpawnChance = 3.75f;

        config.writeChanges();
        JsonObject persisted = JsonParser.parseString(Files.readString(config.getJsonFilePath())).getAsJsonObject();
        assertEquals("examplemod:structures/treasure", persisted.getAsJsonArray("additionalBoosterLootTables").get(0).getAsString());
        assertEquals("examplemod:chests/special_reward", persisted.getAsJsonArray("excludedBoosterLootTables").get(0).getAsString());
        assertTrue(persisted.get("debugBoosterLootTables").getAsBoolean());

        CobblemonCardsConfig.additionalBoosterLootTables = new ArrayList<>();
        CobblemonCardsConfig.excludedBoosterLootTables = new ArrayList<>();
        CobblemonCardsConfig.debugBoosterLootTables = false;
        CobblemonCardsConfig.enableBoosterChestSpawn = true;
        CobblemonCardsConfig.boosterChestSpawnChance = 2.0f;
        config.loadValuesFromJson();

        assertEquals(List.of("examplemod:structures/treasure"), CobblemonCardsConfig.additionalBoosterLootTables);
        assertEquals(List.of("examplemod:chests/special_reward"), CobblemonCardsConfig.excludedBoosterLootTables);
        assertTrue(CobblemonCardsConfig.debugBoosterLootTables);
        assertFalse(CobblemonCardsConfig.enableBoosterChestSpawn);
        assertEquals(3.75f, CobblemonCardsConfig.boosterChestSpawnChance);
    }

    @Test
    void legacyConfigWithoutNewFieldsRetainsDefaultsAndItsConfiguredChance() throws Exception {
        TemporaryConfig config = temporaryConfig();
        CobblemonCardsConfig.additionalBoosterLootTables = new ArrayList<>();
        CobblemonCardsConfig.excludedBoosterLootTables = new ArrayList<>();
        CobblemonCardsConfig.debugBoosterLootTables = false;
        Files.writeString(config.getJsonFilePath(), """
                {"enableBoosterChestSpawn": true, "boosterChestSpawnChance": 7.5}
                """);

        config.loadValuesFromJson();

        assertTrue(CobblemonCardsConfig.enableBoosterChestSpawn);
        assertEquals(7.5f, CobblemonCardsConfig.boosterChestSpawnChance);
        assertTrue(CobblemonCardsConfig.additionalBoosterLootTables.isEmpty());
        assertTrue(CobblemonCardsConfig.excludedBoosterLootTables.isEmpty());
        assertFalse(CobblemonCardsConfig.debugBoosterLootTables);
    }

    private TemporaryConfig temporaryConfig() {
        TemporaryConfig config = new TemporaryConfig(temporaryDirectory.resolve("cobblemon-cards.json"));
        config.configClass = CobblemonCardsConfig.class;
        return config;
    }

    /** Exercises MidnightLib's real writer/reader while redirecting its path away from user configuration. */
    private static final class TemporaryConfig extends CobblemonCardsConfig {
        private final Path jsonPath;

        private TemporaryConfig(Path jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public Path getJsonFilePath() {
            return jsonPath;
        }
    }
}
