package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import java.util.Locale;

/** Read the existing MidnightConfig fields on the server; no second config file or library. */
public final class CompatConfig {
    private CompatConfig() {}

    public enum Integration {
        RUSTLING_SPOTS("rustlingspots"), RAID_DENS("cobblemonraiddens"),
        COBBLED_GACHA("cobbledgacha"), FISHING_LOOT("cobblemonfishingloot");

        private final String modId;
        Integration(String modId) { this.modId = modId; }
        public String modId() { return modId; }
    }

    public static boolean enabled(Integration integration) {
        return switch (integration) {
            case RUSTLING_SPOTS -> rustlingEnabled();
            case RAID_DENS -> CobblemonCardsConfig.raidDensEnabled;
            case COBBLED_GACHA -> CobblemonCardsConfig.cobbledGachaEnabled;
            case FISHING_LOOT -> CobblemonCardsConfig.fishingLootEnabled;
        };
    }

    public static boolean rustlingEnabled() { return CobblemonCardsConfig.rustlingSpotsEnabled; }
    public static double rustlingChance() { return CobblemonCardsConfig.rustlingSpotsBoosterChance; }

    public static BoosterRewardHelper.Selection rustlingSelection(String family) {
        if (family != null && CobblemonCardsConfig.rustlingSpotsFamilyBoosters != null) {
            for (String mapping : CobblemonCardsConfig.rustlingSpotsFamilyBoosters) {
                if (mapping == null) continue;
                String[] parts = mapping.split("=", 2);
                if (parts.length == 2 && parts[0].trim().toUpperCase(Locale.ROOT)
                        .equals(family.toUpperCase(Locale.ROOT))) {
                    return BoosterRewardHelper.Selection.fromToken(parts[1].trim());
                }
            }
        }
        return BoosterRewardHelper.Selection.CLASSIC;
    }

    public static double chance(String reward) {
        return switch (reward) {
            case "raid_1" -> CobblemonCardsConfig.raidDensTier1Chance;
            case "raid_2" -> CobblemonCardsConfig.raidDensTier2Chance;
            case "raid_3" -> CobblemonCardsConfig.raidDensTier3Chance;
            case "raid_4" -> CobblemonCardsConfig.raidDensTier4Chance;
            case "raid_5" -> CobblemonCardsConfig.raidDensTier5Chance;
            case "raid_6" -> CobblemonCardsConfig.raidDensTier6Chance;
            case "raid_7" -> CobblemonCardsConfig.raidDensTier7Chance;
            case "gacha_poke" -> CobblemonCardsConfig.cobbledGachaPokeChance;
            case "gacha_great" -> CobblemonCardsConfig.cobbledGachaGreatChance;
            case "gacha_ultra" -> CobblemonCardsConfig.cobbledGachaUltraChance;
            case "gacha_master" -> CobblemonCardsConfig.cobbledGachaMasterChance;
            case "gacha_cherish" -> CobblemonCardsConfig.cobbledGachaCherishChance;
            case "fishing_water" -> CobblemonCardsConfig.fishingLootWaterChance;
            case "fishing_classic" -> CobblemonCardsConfig.fishingLootClassicChance;
            default -> 0.0;
        };
    }
}
