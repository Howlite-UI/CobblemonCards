package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.howlite.cobblemoncards.compat.CompatConfig.Integration.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatConfigTest {
    private final Map<Field, Object> originalValues = new LinkedHashMap<>();

    @BeforeEach
    void saveCompatibilityConfig() throws IllegalAccessException {
        for (Field field : CobblemonCardsConfig.class.getFields()) {
            String name = field.getName();
            if (name.startsWith("rustlingSpots") || name.startsWith("raidDens")
                    || name.startsWith("cobbledGacha") || name.startsWith("fishingLoot")
                    || name.startsWith("partingGifts") || name.startsWith("radGyms") || name.startsWith("rct")) {
                originalValues.put(field, field.get(null));
            }
        }
    }

    @AfterEach
    void restoreCompatibilityConfig() throws IllegalAccessException {
        for (Map.Entry<Field, Object> entry : originalValues.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
    }

    @Test
    void everyIntegrationReadsItsIndependentRuntimeToggle() {
        CobblemonCardsConfig.rustlingSpotsEnabled = false;
        CobblemonCardsConfig.raidDensEnabled = true;
        CobblemonCardsConfig.cobbledGachaEnabled = false;
        CobblemonCardsConfig.fishingLootEnabled = true;
        assertFalse(CompatConfig.enabled(RUSTLING_SPOTS));
        assertTrue(CompatConfig.enabled(RAID_DENS));
        assertFalse(CompatConfig.enabled(COBBLED_GACHA));
        assertTrue(CompatConfig.enabled(FISHING_LOOT));

        CobblemonCardsConfig.rustlingSpotsEnabled = true;
        CobblemonCardsConfig.raidDensEnabled = false;
        CobblemonCardsConfig.cobbledGachaEnabled = true;
        CobblemonCardsConfig.fishingLootEnabled = false;
        assertTrue(CompatConfig.enabled(RUSTLING_SPOTS));
        assertFalse(CompatConfig.enabled(RAID_DENS));
        assertTrue(CompatConfig.enabled(COBBLED_GACHA));
        assertFalse(CompatConfig.enabled(FISHING_LOOT));
    }

    @Test
    void allRewardIdsUseTheirOwnCurrentConfiguredChance() throws ReflectiveOperationException {
        String[] rewardIds = {"raid_1", "raid_2", "raid_3", "raid_4", "raid_5", "raid_6", "raid_7",
                "gacha_poke", "gacha_great", "gacha_ultra", "gacha_master", "gacha_cherish",
                "fishing_water", "fishing_classic"};
        String[] configFields = {"raidDensTier1Chance", "raidDensTier2Chance", "raidDensTier3Chance",
                "raidDensTier4Chance", "raidDensTier5Chance", "raidDensTier6Chance", "raidDensTier7Chance",
                "cobbledGachaPokeChance", "cobbledGachaGreatChance", "cobbledGachaUltraChance",
                "cobbledGachaMasterChance", "cobbledGachaCherishChance", "fishingLootWaterChance",
                "fishingLootClassicChance"};
        for (int i = 0; i < rewardIds.length; i++) {
            double customChance = i + 0.25;
            CobblemonCardsConfig.class.getField(configFields[i]).setDouble(null, customChance);
            assertEquals(customChance, CompatConfig.chance(rewardIds[i]), rewardIds[i]);
        }
        CobblemonCardsConfig.rustlingSpotsBoosterChance = 12.75;
        assertEquals(12.75, CompatConfig.rustlingChance());
        assertEquals(0.0, CompatConfig.chance("unknown_reward"));
    }

    @Test
    void newIntegrationsHaveIndependentTogglesAndPercentages() {
        CobblemonCardsConfig.partingGiftsEnabled = false;
        CobblemonCardsConfig.radGymsEnabled = true;
        CobblemonCardsConfig.rctEnabled = false;
        assertFalse(CompatConfig.enabled(PARTING_GIFTS));
        assertTrue(CompatConfig.enabled(RAD_GYMS));
        assertFalse(CompatConfig.enabled(RCT));
        CobblemonCardsConfig.partingGiftsEnabled = true;
        CobblemonCardsConfig.radGymsEnabled = false;
        CobblemonCardsConfig.rctEnabled = true;
        assertTrue(CompatConfig.enabled(PARTING_GIFTS));
        assertFalse(CompatConfig.enabled(RAD_GYMS));
        assertTrue(CompatConfig.enabled(RCT));
        CobblemonCardsConfig.partingGiftsBoosterChance = 0.0;
        CobblemonCardsConfig.radGymsBoosterChance = 100.0;
        CobblemonCardsConfig.rctBoosterChance = 17.25;
        assertEquals(0.0, CompatConfig.chance("parting_gifts"));
        assertEquals(100.0, CompatConfig.chance("rad_gyms"));
        assertEquals(17.25, CompatConfig.chance("rctmod:trainers/groups/leader"));
        assertEquals(0.0, CompatConfig.chance("rctmod:generic/common/berries"));
    }

    @Test
    void familyMappingsAcceptActualEnumNamesAndNormalizedConfigKeys() {
        CobblemonCardsConfig.rustlingSpotsFamilyBoosters = List.of(" grass = WATER ", "NETHERFLAMME=fire", "Snow=generation");
        assertEquals(List.of("water"), CompatConfig.rustlingSelection("GRASS").boosterTypes());
        assertEquals(List.of("water"), CompatConfig.rustlingSelection("grass").boosterTypes());
        assertEquals(List.of("fire"), CompatConfig.rustlingSelection("netherflamme").boosterTypes());
        assertEquals(BoosterRewardHelper.Selection.GENERATION, CompatConfig.rustlingSelection("snow"));
    }

    @Test
    void malformedMissingAndUnsupportedMappingsFallBackSafely() {
        CobblemonCardsConfig.rustlingSpotsFamilyBoosters = Arrays.asList(null, "bad_entry", "grass=does_not_exist", "water=themed");
        assertEquals(BoosterRewardHelper.Selection.CLASSIC, CompatConfig.rustlingSelection("grass"));
        assertEquals(BoosterRewardHelper.Selection.THEMED, CompatConfig.rustlingSelection("water"));
        assertEquals(BoosterRewardHelper.Selection.CLASSIC, CompatConfig.rustlingSelection("custom_family"));
        assertEquals(BoosterRewardHelper.Selection.CLASSIC, CompatConfig.rustlingSelection(null));
        CobblemonCardsConfig.rustlingSpotsFamilyBoosters = null;
        assertEquals(BoosterRewardHelper.Selection.CLASSIC, CompatConfig.rustlingSelection("grass"));
    }
}
