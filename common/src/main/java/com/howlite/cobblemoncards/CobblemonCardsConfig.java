package com.howlite.cobblemoncards;

import com.howlite.cobblemoncards.component.CardStat;
import eu.midnightdust.lib.config.MidnightConfig;

public class CobblemonCardsConfig extends MidnightConfig {
    // --- General ---

    @Comment(centered = true, category = "general")
    public static String generalStats;

    @Entry(category = "general")
    public static boolean enableCardStats = true;

    @Entry(category = "general", min = 0.0f, max = 100.0f)
    public static float globalStatMultiplier = 10.0f;

    @Entry(category = "general")
    public static boolean displayPercentStatOnCards = true;

    @Comment(centered = true, category = "general")
    public static String cardSources;

    @Entry(category = "general", min = 0.0f, max = 100.0f)
    public static float cardDropChance = 1.0f;

    @Entry(category = "general", min = 0.0f, max = 100.0f)
    public static float godPackTicketChance = 1.0f;

    /**
     * When false (default), species whose National Pokédex number is outside [1, 1025]
     * (i.e. Fakemon added by addon mods) are excluded from card drops and booster packs.
     * Set to true to allow cards for any registered Cobblemon species.
     */
    @Entry(category = "general")
    public static boolean allowFakemonCards = false;

    @Entry(category = "general")
    public static boolean enableBoosterChestSpawn = true;

    @Entry(category = "general", min = 0.0f, max = 100.0f)
    public static float boosterChestSpawnChance = 2.0f;

    // --- Player ---

    @Comment(centered = true, category = "player")
    public static String playerActivation;

    @Entry(category = "player")
    public static boolean enablePlayerStats = true;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float playerStatMultiplier = 1.0f;

    @Comment(centered = true, category = "player")
    public static String playerMultipliers;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float miningSpeedStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float movementSpeedStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float attackDamageStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float attackSpeedStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float luckStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float armorStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float maxHealthStatMultiplier = 1.0f;

    @Entry(category = "player", min = 0.0f, max = 100.0f)
    public static float cardDropChanceStatMultiplier = 1.0f;

    // --- Spawning ---

    @Comment(centered = true, category = "spawning")
    public static String spawnActivation;

    @Entry(category = "spawning")
    public static boolean enableSpawnBoostStats = true;

    /** Single toggle for all 15 egg-group spawn influences. Also requires {@link #enableSpawnBoostStats}. */
    @Entry(category = "spawning")
    public static boolean enableEggGroupStats = false;

    /** Single toggle for all 6 EV-yield spawn influences. Also requires {@link #enableSpawnBoostStats}. */
    @Entry(category = "spawning")
    public static boolean enableEvYieldStats = false;

    @Comment(centered = true, category = "spawning")
    public static String spawnMultipliers;

    @Entry(category = "spawning", min = 0.0f, max = 100.0f)
    public static float spawnBoostStatMultiplier = 1.0f;

    @Entry(category = "spawning", min = 0.0f, max = 10000.0f)
    public static float maxSpawnBoostMultiplier = 100.0f;

    // --- Trainer ---

    @Comment(centered = true, category = "trainer")
    public static String trainerActivation;

    /** Master toggle for the Exp / Catch / Shiny "trainer" stats. */
    @Entry(category = "trainer")
    public static boolean enableTrainerStats = true;

    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float trainerStatMultiplier = 1.0f;

    @Comment(centered = true, category = "trainer")
    public static String trainerMultipliers;

    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float expBoostStatMultiplier = 1.0f;

    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float catchBoostStatMultiplier = 1.0f;

    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float shinyChanceStatMultiplier = 1.0f;

    @Comment(centered = true, category = "trainer")
    public static String trainerLimits;

    @Entry(category = "trainer", min = 1.0f, max = 100.0f)
    public static float maxExpBoostMultiplier = 5.0f;

    @Entry(category = "trainer", min = 1.0f, max = 100.0f)
    public static float maxCatchBoostMultiplier = 5.0f;

    /** Shiny rate is "1-in-N", so a boost divides it. This caps the divisor. */
    @Entry(category = "trainer", min = 1.0f, max = 100.0f)
    public static float maxShinyBoostDivisor = 10.0f;

    @Comment(centered = true, category = "trainer")
    public static String trainerAcquisition;

    @Entry(category = "trainer")
    public static boolean enableTrainerStatGrading = true;

    /** Minimum grade that can earn a trainer stat. */
    @Entry(category = "trainer", min = 1, max = 10)
    public static int trainerStatMinGrade = 9;

    /** Chance (%) that a card graded at or above {@link #trainerStatMinGrade} converts to a trainer stat. */
    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float trainerStatGradeChance = 50.0f;

    /** Chance (%) that a Legendary / Mythic / Shiny card rolls a trainer stat on drop. */
    @Entry(category = "trainer", min = 0.0f, max = 100.0f)
    public static float trainerStatLuckyChance = 5.0f;

    /** Lucky trainer stats roll at this fraction of the normal rarity-based value. */
    @Entry(category = "trainer", min = 0.0f, max = 1.0f)
    public static float trainerStatLuckyValueMultiplier = 0.25f;

    // --- Machines ---

    @Comment(centered = true, category = "machines")
    public static String recycling;

    @Entry(category = "machines", min = 1, max = 1200)
    public static int recyclerProcessTime = 40;

    @Comment(centered = true, category = "machines")
    public static String grading;

    @Entry(category = "machines", min = 1, max = 12000)
    public static int gradingStationProcessTime = 100;

    @Entry(category = "machines", min = 0, max = 64)
    public static int gradingStationDustCost = 5;

    @Comment(centered = true, category = "machines")
    public static String restoration;

    @Entry(category = "machines", min = 1, max = 1000)
    public static int restorerBaseCost = 5;

    /** Duration in ticks for restoring to grade 2 (20 ticks = 1 second). */
    @Entry(category = "machines", min = 1, max = 72000)
    public static int restorerBaseProcessTime = 60;

    /** Extra ticks for each target grade above 2. Set to 0 for a fixed duration. */
    @Entry(category = "machines", min = 0, max = 72000)
    public static int restorerProcessTimePerGrade = 60;

    // --- Binders ---

    @Comment(centered = true, category = "binders")
    public static String binderAvailability;

    @Entry(category = "binders")
    public static boolean enableLeatherBinder = true;

    @Entry(category = "binders")
    public static boolean enableIronBinder = true;

    @Entry(category = "binders")
    public static boolean enableGoldBinder = true;

    @Entry(category = "binders")
    public static boolean enableDiamondBinder = true;

    @Entry(category = "binders")
    public static boolean enableNetheriteBinder = true;

    @Entry(category = "binders")
    public static boolean enableMasterAlbum = true;

    @Entry(category = "binders")
    public static boolean masterAlbumGivesStats = false;

    @Comment(centered = true, category = "binders")
    public static String binderCapacity;

    @Entry(category = "binders", min = 1, max = 1000)
    public static int leatherBinderPages = 1;

    @Entry(category = "binders", min = 1, max = 1000)
    public static int ironBinderPages = 2;

    @Entry(category = "binders", min = 1, max = 1000)
    public static int goldBinderPages = 3;

    @Entry(category = "binders", min = 1, max = 1000)
    public static int diamondBinderPages = 6;

    @Entry(category = "binders", min = 1, max = 1000)
    public static int netheriteBinderPages = 10;

    @Entry(category = "binders", min = 1, max = 2000)
    public static int masterAlbumPages = 1000;

    public static int getRestorerProcessTime(int targetGrade) {
        if (targetGrade < 2 || targetGrade > 10) return 0;
        int base = Math.clamp(restorerBaseProcessTime, 1, 72000);
        int perGrade = Math.clamp(restorerProcessTimePerGrade, 0, 72000);
        return base + (targetGrade - 2) * perGrade;
    }

    public static float getStatMultiplier(CardStat stat) {
        if (stat == null || !enableCardStats
                || (!enablePlayerStats && isPlayerStat(stat))
                || (!enableSpawnBoostStats && isSpawnBoostStat(stat))
                || (!enableTrainerStats && isTrainerStat(stat))
                || (!enableEggGroupStats && isEggGroupStat(stat))
                || (!enableEvYieldStats && isEvYieldStat(stat))) {
            return 0.0f;
        }

        if (isPlayerStat(stat)) {
            return globalStatMultiplier * playerStatMultiplier * getPerStatMultiplier(stat);
        }
        if (isTrainerStat(stat)) {
            return globalStatMultiplier * trainerStatMultiplier * getPerStatMultiplier(stat);
        }
        // Elemental-type, egg-group and EV-yield boosts all feed the same spawn weighting pipeline.
        if (isSpawnBoostStat(stat)) {
            return globalStatMultiplier * spawnBoostStatMultiplier;
        }

        // Fallback return, shouldn't get hit unless a stat does not count as either a player or spawn boost stat
        return globalStatMultiplier;
    }

    /**
     * Per-stat multiplier for the "vanilla attribute" / player stats.
     * Returns 1.0f for any stat without a dedicated config entry.
     */
    public static float getPerStatMultiplier(CardStat stat) {
        if (stat == null) return 1.0f;
        return switch (stat) {
            case MINING_SPEED -> miningSpeedStatMultiplier;
            case MOVEMENT_SPEED -> movementSpeedStatMultiplier;
            case ATTACK_DAMAGE -> attackDamageStatMultiplier;
            case ATTACK_SPEED -> attackSpeedStatMultiplier;
            case LUCK -> luckStatMultiplier;
            case ARMOR -> armorStatMultiplier;
            case MAX_HEALTH -> maxHealthStatMultiplier;
            case CARD_DROP_CHANCE -> cardDropChanceStatMultiplier;
            case EXP_BOOST -> expBoostStatMultiplier;
            case CATCH_BOOST -> catchBoostStatMultiplier;
            case SHINY_CHANCE -> shinyChanceStatMultiplier;
            default -> 1.0f;
        };
    }

    public static boolean isPlayerStat(CardStat stat) {
        return stat == CardStat.MINING_SPEED || stat == CardStat.MOVEMENT_SPEED
                || stat == CardStat.ATTACK_DAMAGE || stat == CardStat.ATTACK_SPEED
                || stat == CardStat.LUCK || stat == CardStat.ARMOR
                || stat == CardStat.MAX_HEALTH || stat == CardStat.CARD_DROP_CHANCE;
    }

    /** Elemental-type spawn boosts only (the original {@code *_spawn} stats). */
    public static boolean isSpawnStat(CardStat stat) {
        return stat != null && stat.getSerializedName().endsWith("_spawn");
    }

    /** The Exp / Catch / Shiny global player boosts. */
    public static boolean isTrainerStat(CardStat stat) {
        return stat == CardStat.EXP_BOOST || stat == CardStat.CATCH_BOOST || stat == CardStat.SHINY_CHANCE;
    }

    /** Egg-group spawn influences ({@code *_egg}). */
    public static boolean isEggGroupStat(CardStat stat) {
        return stat != null && stat.getSerializedName().endsWith("_egg");
    }

    /** EV-yield spawn influences ({@code *_yield}). */
    public static boolean isEvYieldStat(CardStat stat) {
        return stat != null && stat.getSerializedName().endsWith("_yield");
    }

    /**
     * Any stat that feeds Cobblemon's spawn weighting: elemental type, egg group or EV yield.
     * All three are gated by {@link #enableSpawnBoostStats} so the master toggle stays meaningful.
     */
    public static boolean isSpawnBoostStat(CardStat stat) {
        return isSpawnStat(stat) || isEggGroupStat(stat) || isEvYieldStat(stat);
    }

    public static int getBinderPages(com.howlite.cobblemoncards.item.custom.BinderTier tier, int defaultPages) {
        if (tier == null) return defaultPages;
        return switch (tier) {
            case LEATHER -> leatherBinderPages;
            case IRON -> ironBinderPages;
            case GOLD -> goldBinderPages;
            case DIAMOND -> diamondBinderPages;
            case NETHERITE -> netheriteBinderPages;
            case MASTER -> masterAlbumPages;
        };
    }

    public static boolean isBinderTierEnabled(com.howlite.cobblemoncards.item.custom.BinderTier tier) {
        if (tier == null) return true;
        return switch (tier) {
            case LEATHER -> enableLeatherBinder;
            case IRON -> enableIronBinder;
            case GOLD -> enableGoldBinder;
            case DIAMOND -> enableDiamondBinder;
            case NETHERITE -> enableNetheriteBinder;
            case MASTER -> enableMasterAlbum;
        };
    }
}
