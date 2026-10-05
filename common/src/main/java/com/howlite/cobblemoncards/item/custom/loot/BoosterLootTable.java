package com.howlite.cobblemoncards.item.custom.loot;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.util.FakemonCardRegistry;
import com.howlite.cobblemoncards.util.SpeciesRarityManager;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class BoosterLootTable {

    // Lista dinamica generada a partir del registro Cobblemon
    private static List<String> cachedPokemonIds = null;
    // Tracks the last allowFakemonCards value used to build the cache.
    // If the config changes, the cache is automatically invalidated.
    private static boolean cachedAllowFakemon = false;
    // Tracks the whitelist size at cache build time. If the whitelist changes
    // (datapack reload), the cache is also invalidated automatically.
    private static int cachedWhitelistSize = -1;

    /**
     * Récupère la liste des IDs de Pokémon disponibles.
     * Utilise le registre PokemonSpecies de Cobblemon pour générer la liste dynamiquement.
     * Le résultat est mis en cache après le premier appel.
     */
    public static List<String> getPokemonIds() {
        // Auto-invalidate if the config option or the datapack whitelist changed.
        int currentWhitelistSize = FakemonCardRegistry.getWhitelistedIds().size();
        if (cachedPokemonIds != null
                && (cachedAllowFakemon != CobblemonCardsConfig.allowFakemonCards
                    || cachedWhitelistSize != currentWhitelistSize)) {
            CobblemonCards.LOGGER.info(
                    "[CobblemonCards] Species cache invalidated (allowFakemon={}, whitelistSize={}).",
                    CobblemonCardsConfig.allowFakemonCards, currentWhitelistSize);
            invalidateCache();
        }

        if (cachedPokemonIds == null || cachedPokemonIds.isEmpty()) {
            cachedAllowFakemon = CobblemonCardsConfig.allowFakemonCards;
            cachedWhitelistSize = FakemonCardRegistry.getWhitelistedIds().size();
            try {
                List<String> loadedIds = PokemonSpecies.getImplemented().stream()
                        .filter(species -> {
                            if (CobblemonCardsConfig.allowFakemonCards) return true;
                            // Allow officially registered Cobblemon species OR explicitly whitelisted Fakemon.
                            // Namespace-based check is more reliable than dex numbers because some
                            // Fakemon addons reuse dex numbers within the 1-1025 range.
                            return isOfficialSpecies(species)
                                    || FakemonCardRegistry.isWhitelisted(species.getName());
                        })
                        .map(species -> species.getName().toLowerCase())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                if (!loadedIds.isEmpty()) {
                    cachedPokemonIds = loadedIds;
                    CobblemonCards.LOGGER.info(
                            "[CobblemonCards] Liste dynamique chargée : {} espèces disponibles (allowFakemon={}, whitelist={}).",
                            cachedPokemonIds.size(), CobblemonCardsConfig.allowFakemonCards, cachedWhitelistSize);
                } else {
                    // Si la liste est vide (chargement trop précoce), on retourne une liste temporaire de secours sans la mettre en cache définitivement
                    return List.of("pikachu", "charizard", "mewtwo", "lucario", "greninja", "gengar", "eevee", "bulbasaur", "squirtle", "rayquaza");
                }
            } catch (Exception e) {
                CobblemonCards.LOGGER.warn("[CobblemonCards] Impossible de charger les espèces Cobblemon, utilisation d'une liste de secours.", e);
                cachedPokemonIds = new ArrayList<>(List.of("pikachu", "charizard", "mewtwo", "lucario", "greninja", "gengar", "eevee", "bulbasaur", "squirtle", "rayquaza"));
            }
        }
        return cachedPokemonIds;
    }

    private static final java.util.Map<String, List<String>> FILTERED_POKEMON_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Force le rechargement de la liste des Pokémon.
     * Utile si des datapacks ajoutent de nouvelles espèces en cours de partie.
     */
    public static void invalidateCache() {
        cachedPokemonIds = null;
        cachedAllowFakemon = false;
        FILTERED_POKEMON_CACHE.clear();
        SpeciesRarityManager.invalidateCaches();
        CobblemonCards.LOGGER.info("[CobblemonCards] Cache des espèces Pokémon invalidé, rechargement au prochain appel.");
    }

    /**
     * Returns true if the given species belongs to the official Cobblemon registry
     * (namespace "cobblemon"), meaning it is not a Fakemon added by an addon mod.
     * Falls back to the dex-number range [1, 1025] if no ResourceIdentifier is available.
     */
    private static boolean isOfficialSpecies(Species species) {
        if (species.getResourceIdentifier() != null) {
            return "cobblemon".equals(species.getResourceIdentifier().getNamespace());
        }
        // Fallback: trust the dex number if no resource identifier is available
        int dex = species.getNationalPokedexNumber();
        return dex >= 1 && dex <= 1025;
    }


    private static final List<String> BACKGROUNDS = Arrays.asList(
            "skybg2", "skybg3", "skybg4", "skybg5", "rockbg1", "grassbg1", "grassbg2", 
            "swampbg1", "waterbg1", "forestbg1", "water_anim", "lava_anim", "balatro_swirl", 
            "geometric_pulse", "plasma_bg", "starfield_anim", "cloud_scroll", "neon_grid", "toxic_sludge",
            "matrix_code", "fire_embers", "crystal_cave", "sandstorm",
            "aurora_borealis", "deep_ocean", "void_rift", "golden_sunset",
            "cherry_blossom_wind", "cyber_city", "ancient_ruins", "frozen_tundra",
            "rainbow_highway", "plasma_storm", "galactic_supernova", "water2",
            "mega_energy", "alola_beach", "hisui_ancient", "galar_industrial", "paldea_crystal",
            "distortion_rift", "dreamscape", "magma_chamber",
            "stained_glass", "fluid_marble", "fossilized_amber"
    );
    
    private static final List<String> EFFECTS = Arrays.asList(
            "flow", "glint", "noise", "foil_stars", 
            "holo_lines", "holo_pulse", "holo_rainbow", "holo_sparkle",
            "holo_runes", "holo_circuit", "holo_bubbles",
            "holo_shatter", "holo_ripple", "holo_scanline",
            "holo_prism", "holo_aurora", "holo_vortex", "holo_lightning",
            "holo_galaxy", "holo_sakura", "holo_plasma_arc", "holo_diamond",
            "holo_aura", "holo_neon_pulse", "holo_constellation", "holo_cyber_dust", "holo_magical_wind",
            "holo_mega", "holo_regional",
            "holo_time_gears", "holo_spatial_crack", "holo_prism_stars"
    );

    private static final Random RANDOM = new Random();

    // Méthodes pour arrière-compatibilité
    public static ItemStack getRandomReward() {
        return getRandomReward("common", "base");
    }

    public static ItemStack getRandomReward(String rarity) {
        return getRandomReward(rarity, "base");
    }

    public static ItemStack getGodPackReward() {
        return getGodPackReward("base");
    }

    public static ItemStack getRandomRewardAbove(String minRarity) {
        return getRandomRewardAbove(minRarity, "base");
    }

    // Nouvelles méthodes avec boosterType
    public static ItemStack getRandomReward(String rarity, String boosterType) {
        return createCard(rarity, false, boosterType);
    }

    public static ItemStack getGodPackReward(String boosterType) {
        String rarity = RANDOM.nextBoolean() ? "epic" : "legendary";
        return createCard(rarity, true, boosterType);
    }

    public static ItemStack getRandomRewardAbove(String minRarity, String boosterType) {
        String rarity = "rare";
        int roll = RANDOM.nextInt(100);

        if ("rare".equals(minRarity)) {
            if (roll < 5) rarity = "legendary"; // 5%
            else if (roll < 20) rarity = "epic"; // 15%
            else rarity = "rare";               // 80%
        }

        return createCard(rarity, false, boosterType);
    }

    /**
     * Rolls a random card rarity based on standard probability tiers.
     */
    public static String getRandomRarity() {
        int roll = RANDOM.nextInt(10000);
        if (roll < 10) return "mythic";       // 0.1%
        if (roll < 110) return "legendary";    // 1%
        if (roll < 610) return "epic";         // 5%
        if (roll < 2110) return "rare";        // 15%
        if (roll < 5110) return "uncommon";    // 30%
        return "common";                       // ~49%
    }

    /**
     * Species-aware overload for capture/faint drops.
     * When the captured species is known, the rarity roll is influenced by
     * SpeciesRarityManager so that, for example, catching a legendary leans
     * toward a higher-rarity card drop.
     */
    public static String getRandomRarity(Species species) {
        String weightedRarity = SpeciesRarityManager.rollCaptureRarity(species, RANDOM);
        return weightedRarity != null ? weightedRarity : getRandomRarity();
    }

    private static ItemStack createCard(String rarity, boolean forceGodPack, String boosterType) {
        List<String> pokemonIds = getFilteredPokemonIds(boosterType);
        if (pokemonIds.isEmpty()) {
            pokemonIds = getPokemonIds();
        }
        // Use species-aware weighted selection instead of uniform random
        String basePokemonId = SpeciesRarityManager.selectSpecies(pokemonIds, rarity, RANDOM);
        boolean isShiny = forceGodPack || RANDOM.nextInt(64) == 0; // 1 chance sur 64 d'être shiny, ou forcé si God Pack

        // Détection et surclassement Régional / Méga
        boolean isMega = false;
        boolean isRegional = false;
        String regionalType = "none";
        String pokemonId = basePokemonId;

        java.util.Set<String> alolan = java.util.Set.of("vulpix", "ninetales", "sandshrew", "sandslash", "raichu", "meowth", "persian", "geodude", "graveler", "golem", "grimer", "muk", "exeggutor", "marowak");
        java.util.Set<String> galarian = java.util.Set.of("zigzagoon", "linoone", "ponyta", "rapidash", "farfetchd", "weezing", "mr_mime", "corsola", "darumaka", "darmanitan", "yamask", "stunfisk", "slowpoke", "slowbro", "slowking", "articuno", "zapdos", "moltres");
        java.util.Set<String> hisuian = java.util.Set.of("growlithe", "arcanine", "voltorb", "electrode", "typhlosion", "qwilfish", "sneasel", "zorua", "zoroark", "braviary", "sliggoo", "goodra", "avalugg", "decidueye", "samurott", "lilligant", "basculin");
        java.util.Set<String> mega = java.util.Set.of("venusaur", "charizard", "blastoise", "alakazam", "gengar", "kangaskhan", "pinsir", "gyarados", "aerodactyl", "mewtwo", "ampharos", "scizor", "heracross", "tyranitar", "blaziken", "gardevoir", "mawile", "aggron", "medicham", "manectric", "banette", "absol", "garchomp", "lucario", "abomasnow", "beedrill", "pidgeot", "steelix", "sceptile", "swampert", "sableye", "sharpedo", "camerupt", "altaria", "glalie", "salamence", "metagross", "latias", "latios", "rayquaza", "lopunny", "gallade", "audino", "diancie");

        String pIdLower = basePokemonId.toLowerCase();
        if (mega.contains(pIdLower) && (rarity.equals("rare") || rarity.equals("epic") || rarity.equals("legendary") || rarity.equals("mythic")) && RANDOM.nextFloat() < 0.16f) {
            isMega = true;
            if (pIdLower.equals("charizard") || pIdLower.equals("mewtwo")) {
                pokemonId = pIdLower + (RANDOM.nextBoolean() ? "_mega_x" : "_mega_y");
            } else {
                pokemonId = pIdLower + "_mega";
            }
        } else if (RANDOM.nextFloat() < 0.12f) {
            if (alolan.contains(pIdLower)) {
                isRegional = true;
                regionalType = "alolan";
                pokemonId = pIdLower + "_alolan";
            } else if (galarian.contains(pIdLower)) {
                isRegional = true;
                regionalType = "galarian";
                pokemonId = pIdLower + "_galarian";
            } else if (hisuian.contains(pIdLower)) {
                isRegional = true;
                regionalType = "hisuian";
                pokemonId = pIdLower + "_hisuian";
            }
        }

        // Sélection de la statistique
        CardStat stat = com.howlite.cobblemoncards.util.CardStatUtil.randomStat(RANDOM);
        float statValue = 0f;

        // Probabilités pour Background et Effect
        float bgChance = 0f;
        float effChance = 0f;

        // Calcul de la valeur selon la rareté — valeurs équilibrées & progressives
        // finalValue = statValue × globalStatMultiplier (défaut 10)
        switch (rarity) {
            case "common":
                // ×10 → +0.05 à +0.10 (quasi cosmétique, encourage la progression)
                statValue = 0.005f + RANDOM.nextFloat() * 0.005f;
                bgChance = 0.05f; // 5%
                effChance = 0.02f; // 2%
                break;
            case "uncommon":
                // ×10 → +0.15 à +0.30 (notable mais faible)
                statValue = 0.015f + RANDOM.nextFloat() * 0.015f;
                bgChance = 0.20f; // 20%
                effChance = 0.10f; // 10%
                break;
            case "rare":
                // ×10 → +0.40 à +0.70 (solide, vaut la peine d'équiper)
                statValue = 0.04f + RANDOM.nextFloat() * 0.03f;
                bgChance = 0.50f; // 50%
                effChance = 0.40f; // 40%
                break;
            case "epic":
                // ×10 → +0.80 à +1.20 (vraiment différent des Rare!)
                statValue = 0.08f + RANDOM.nextFloat() * 0.04f;
                bgChance = 0.90f; // 90%
                effChance = 0.85f; // 85%
                break;
            case "legendary":
                // ×10 → +1.20 à +1.80 (pièce de collection, forte impact)
                statValue = 0.12f + RANDOM.nextFloat() * 0.06f;
                bgChance = 1.00f; // 100%
                effChance = 1.00f; // 100%
                break;
            case "mythic":
                // ×10 → +2.00 à +2.50 (rarissime, max de ce qu'on veut permettre)
                statValue = 0.20f + RANDOM.nextFloat() * 0.05f;
                bgChance = 1.00f; // 100%
                effChance = 1.00f; // 100%
                break;
            default:
                statValue = 0.005f;
        }

        // Bonus Shiny — réduit pour rester raisonnable
        if (isShiny) {
            statValue += 0.03f; // ×10 → +0.3 bonus shiny (au lieu de +0.5 avant)
            bgChance = Math.max(bgChance, 0.8f);
            effChance = Math.max(effChance, 0.8f);
        }

        Optional<String> effect;
        Optional<String> background;

        if (isMega) {
            background = Optional.of("mega_energy");
            effect = Optional.of("holo_mega");
            statValue *= 1.20f; // +20% de boost de stat !
        } else if (isRegional) {
            effect = Optional.of("holo_regional");
            statValue *= 1.20f; // +20% de boost de stat !
            background = switch (regionalType) {
                case "alolan" -> Optional.of("alola_beach");
                case "galarian" -> Optional.of("galar_industrial");
                case "hisuian" -> Optional.of("hisui_ancient");
                default -> Optional.of("paldea_crystal");
            };
        } else {
            effect = RANDOM.nextFloat() < effChance 
                ? Optional.of(EFFECTS.get(RANDOM.nextInt(EFFECTS.size()))) 
                : Optional.empty();

            background = RANDOM.nextFloat() < bgChance 
                ? Optional.of(BACKGROUNDS.get(RANDOM.nextInt(BACKGROUNDS.size())))
                : Optional.empty();

            if (effect.isPresent() && background.isEmpty()) {
                background = Optional.of(com.howlite.cobblemoncards.util.CardUtil.getDefaultBackground(pokemonId));
            }
        }

        com.howlite.cobblemoncards.util.CardStatUtil.RolledStat rolled =
                com.howlite.cobblemoncards.util.CardStatUtil.applyLuckyTrainerStat(
                        stat, statValue, rarity, isShiny, RANDOM);
        stat = rolled.stat();
        statValue = rolled.value();

        ItemStack cardStack = new ItemStack(ModItems.CARD);
        CardData data = new CardData(pokemonId, isShiny, rarity, stat, statValue, 0, background, effect);
        cardStack.set(ModDataComponents.CARD_DATA, data);

        return cardStack;
    }

    // Système de filtrage par booster thématique
    public static List<String> getFilteredPokemonIds(String boosterType) {
        if ("base".equals(boosterType)) {
            return getPokemonIds();
        }
        return FILTERED_POKEMON_CACHE.computeIfAbsent(boosterType, type -> {
            List<String> allIds = getPokemonIds();
            List<String> filtered = new ArrayList<>();
            for (String id : allIds) {
                try {
                    String cleanId = id.replace("’", "").replace("'", "");
                    com.cobblemon.mod.common.pokemon.Species species = com.howlite.cobblemoncards.util.CardUtil.getSpecies(cleanId);
                    if (species != null) {
                        if (matchesFilter(species, type)) {
                            filtered.add(id);
                        }
                    }
                } catch (Exception e) {
                    // Suppress any ResourceLocationException or other errors for special names
                }
            }
            return filtered;
        });
    }

    private static boolean matchesFilter(com.cobblemon.mod.common.pokemon.Species species, String boosterType) {
        switch (boosterType) {
            case "gen1":
                return species.getNationalPokedexNumber() <= 151;
            case "gen2":
                return species.getNationalPokedexNumber() >= 152 && species.getNationalPokedexNumber() <= 251;
            case "gen3":
                return species.getNationalPokedexNumber() >= 252 && species.getNationalPokedexNumber() <= 386;
            case "gen4":
                return species.getNationalPokedexNumber() >= 387 && species.getNationalPokedexNumber() <= 493;
            case "gen5":
                return species.getNationalPokedexNumber() >= 494 && species.getNationalPokedexNumber() <= 649;
            case "gen6":
                return species.getNationalPokedexNumber() >= 650 && species.getNationalPokedexNumber() <= 721;
            case "gen7":
                return species.getNationalPokedexNumber() >= 722 && species.getNationalPokedexNumber() <= 809;
            case "gen8":
                return species.getNationalPokedexNumber() >= 810 && species.getNationalPokedexNumber() <= 898;
            case "gen9":
                return species.getNationalPokedexNumber() >= 899 && species.getNationalPokedexNumber() <= 1025;
            case "fire":
            case "water":
            case "grass":
            case "electric":
            case "ice":
            case "fighting":
            case "poison":
            case "ground":
            case "flying":
            case "psychic":
            case "bug":
            case "rock":
            case "ghost":
            case "dragon":
            case "steel":
            case "fairy":
            case "dark":
            case "normal":
                boolean hasPrimary = species.getPrimaryType() != null && species.getPrimaryType().getName().equalsIgnoreCase(boosterType);
                boolean hasSecondary = species.getSecondaryType() != null && species.getSecondaryType().getName().equalsIgnoreCase(boosterType);
                return hasPrimary || hasSecondary;
            default:
                return true;
        }
    }
}
