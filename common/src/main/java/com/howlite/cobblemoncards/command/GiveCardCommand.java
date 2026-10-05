package com.howlite.cobblemoncards.command;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class GiveCardCommand {

    private static final List<String> RARITIES = Arrays.asList("common", "uncommon", "rare", "epic", "legendary");
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
            "stained_glass", "fluid_marble", "fossilized_amber", "none");
    private static final List<String> EFFECTS = Arrays.asList(
            "flow", "glint", "noise", "foil_stars",
            "holo_lines", "holo_pulse", "holo_rainbow", "holo_sparkle",
            "holo_runes", "holo_circuit", "holo_bubbles",
            "holo_shatter", "holo_ripple", "holo_scanline",
            "holo_prism", "holo_aurora", "holo_vortex", "holo_lightning",
            "holo_galaxy", "holo_sakura", "holo_plasma_arc", "holo_diamond",
            "holo_aura", "holo_neon_pulse", "holo_constellation", "holo_cyber_dust", "holo_magical_wind",
            "holo_mega", "holo_regional",
            "holo_time_gears", "holo_spatial_crack", "holo_prism_stars", "none");

    private static final SuggestionProvider<CommandSourceStack> POKEMON_SUGGESTIONS = (context, builder) -> {
        java.util.Set<String> alolan = java.util.Set.of("vulpix", "ninetales", "sandshrew", "sandslash", "raichu",
                "meowth", "persian", "geodude", "graveler", "golem", "grimer", "muk", "exeggutor", "marowak");
        java.util.Set<String> galarian = java.util.Set.of("zigzagoon", "linoone", "ponyta", "rapidash", "farfetchd",
                "weezing", "mr_mime", "corsola", "darumaka", "darmanitan", "yamask", "stunfisk", "slowpoke", "slowbro",
                "slowking", "articuno", "zapdos", "moltres");
        java.util.Set<String> hisuian = java.util.Set.of("growlithe", "arcanine", "voltorb", "electrode", "typhlosion",
                "qwilfish", "sneasel", "zorua", "zoroark", "braviary", "sliggoo", "goodra", "avalugg", "decidueye",
                "samurott", "lilligant", "basculin");
        java.util.Set<String> paldean = java.util.Set.of("wooper", "tauros");
        java.util.Set<String> mega = java.util.Set.of("venusaur", "charizard", "blastoise", "alakazam", "gengar", "kangaskhan", "pinsir", "gyarados", "aerodactyl", "mewtwo", "ampharos", "scizor", "heracross", "tyranitar", "blaziken", "gardevoir", "mawile", "aggron", "medicham", "manectric", "banette", "absol",
                "garchomp", "lucario", "abomasnow", "beedrill", "pidgeot", "steelix", "sceptile", "swampert", "sableye",
                "sharpedo", "camerupt", "altaria", "glalie", "salamence", "metagross", "latias", "latios", "rayquaza",
                "lopunny", "gallade", "audino", "diancie");

        List<String> baseNames = PokemonSpecies.getSpecies().stream()
                .map(s -> s.getName().toLowerCase())
                .distinct()
                .toList();

        java.util.List<String> names = new java.util.ArrayList<>();
        for (String base : baseNames) {
            names.add(base);
            if (alolan.contains(base))
                names.add(base + "_alolan");
            if (galarian.contains(base))
                names.add(base + "_galarian");
            if (hisuian.contains(base))
                names.add(base + "_hisuian");
            if (paldean.contains(base)) {
                if (base.equals("tauros")) {
                    names.add(base + "_paldean_combat");
                    names.add(base + "_paldean_blaze");
                    names.add(base + "_paldean_aqua");
                } else if (base.equals("wooper")) {
                    names.add(base + "_paldean");
                }
            }
            if (mega.contains(base)) {
                if (base.equals("charizard") || base.equals("mewtwo")) {
                    names.add(base + "_mega_x");
                    names.add(base + "_mega_y");
                } else {
                    names.add(base + "_mega");
                }
            }
        }
        names.sort(String::compareTo);
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> RARITY_SUGGESTIONS = (context,
            builder) -> SharedSuggestionProvider.suggest(RARITIES, builder);

    private static final SuggestionProvider<CommandSourceStack> STAT_SUGGESTIONS = (context,
            builder) -> SharedSuggestionProvider
                    .suggest(Arrays.stream(CardStat.values()).map(CardStat::getSerializedName), builder);

    private static final SuggestionProvider<CommandSourceStack> BACKGROUND_SUGGESTIONS = (context,
            builder) -> SharedSuggestionProvider.suggest(BACKGROUNDS, builder);

    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS = (context,
            builder) -> SharedSuggestionProvider.suggest(EFFECTS, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cobblecard")
                .requires(source -> source.hasPermission(2))
                // ─── /cobblecard render ─────────────────────────────────────────────
                // Ouvre un écran cinématique de mise en scène (showcase) pour screenshots.
                // Syntaxe : /cobblecard render <pokemon> <shiny> <rarity> [<background>
                // [<effect>]]
                // + optionnel : more <pokemon2> <shiny2> <rarity2> [<background2> [<effect2>]]
                // (répétable jusqu'à 5 cartes au total)
                .then(Commands.literal("render")
                        .then(Commands.argument("pokemon", StringArgumentType.string())
                                .suggests(POKEMON_SUGGESTIONS)
                                .then(Commands.argument("shiny", BoolArgumentType.bool())
                                        .then(Commands.argument("rarity", StringArgumentType.string())
                                                .suggests(RARITY_SUGGESTIONS)
                                                // 1 carte, sans background ni effect
                                                .executes(context -> executeRender(context, 1))
                                                .then(Commands.argument("background", StringArgumentType.string())
                                                        .suggests(BACKGROUND_SUGGESTIONS)
                                                        // 1 carte avec background
                                                        .executes(context -> executeRender(context, 1))
                                                        .then(Commands.argument("effect", StringArgumentType.string())
                                                                .suggests(EFFECT_SUGGESTIONS)
                                                                // 1 carte avec background + effect
                                                                .executes(context -> executeRender(context, 1))
                                                                // ── Carte 2 ──
                                                                .then(Commands.literal("more")
                                                                        .then(Commands
                                                                                .argument("pokemon2",
                                                                                        StringArgumentType.string())
                                                                                .suggests(POKEMON_SUGGESTIONS)
                                                                                .then(Commands
                                                                                        .argument("shiny2",
                                                                                                BoolArgumentType.bool())
                                                                                        .then(Commands.argument(
                                                                                                "rarity2",
                                                                                                StringArgumentType
                                                                                                        .string())
                                                                                                .suggests(
                                                                                                        RARITY_SUGGESTIONS)
                                                                                                .executes(
                                                                                                        context -> executeRender(
                                                                                                                context,
                                                                                                                2))
                                                                                                .then(Commands.argument(
                                                                                                        "background2",
                                                                                                        StringArgumentType
                                                                                                                .string())
                                                                                                        .suggests(
                                                                                                                BACKGROUND_SUGGESTIONS)
                                                                                                        .executes(
                                                                                                                context -> executeRender(
                                                                                                                        context,
                                                                                                                        2))
                                                                                                        .then(Commands
                                                                                                                .argument(
                                                                                                                        "effect2",
                                                                                                                        StringArgumentType
                                                                                                                                .string())
                                                                                                                .suggests(
                                                                                                                        EFFECT_SUGGESTIONS)
                                                                                                                .executes(
                                                                                                                        context -> executeRender(
                                                                                                                                context,
                                                                                                                                2))
                                                                                                                // ──
                                                                                                                // Carte
                                                                                                                // 3 ──
                                                                                                                .then(Commands
                                                                                                                        .literal(
                                                                                                                                "more")
                                                                                                                        .then(Commands
                                                                                                                                .argument(
                                                                                                                                        "pokemon3",
                                                                                                                                        StringArgumentType
                                                                                                                                                .string())
                                                                                                                                .suggests(
                                                                                                                                        POKEMON_SUGGESTIONS)
                                                                                                                                .then(Commands
                                                                                                                                        .argument(
                                                                                                                                                "shiny3",
                                                                                                                                                BoolArgumentType
                                                                                                                                                        .bool())
                                                                                                                                        .then(Commands
                                                                                                                                                .argument(
                                                                                                                                                        "rarity3",
                                                                                                                                                        StringArgumentType
                                                                                                                                                                .string())
                                                                                                                                                .suggests(
                                                                                                                                                        RARITY_SUGGESTIONS)
                                                                                                                                                .executes(
                                                                                                                                                        context -> executeRender(
                                                                                                                                                                context,
                                                                                                                                                                3))
                                                                                                                                                .then(Commands
                                                                                                                                                        .argument(
                                                                                                                                                                "background3",
                                                                                                                                                                StringArgumentType
                                                                                                                                                                        .string())
                                                                                                                                                        .suggests(
                                                                                                                                                                BACKGROUND_SUGGESTIONS)
                                                                                                                                                        .executes(
                                                                                                                                                                context -> executeRender(
                                                                                                                                                                        context,
                                                                                                                                                                        3))
                                                                                                                                                        .then(Commands
                                                                                                                                                                .argument(
                                                                                                                                                                        "effect3",
                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                .string())
                                                                                                                                                                .suggests(
                                                                                                                                                                        EFFECT_SUGGESTIONS)
                                                                                                                                                                .executes(
                                                                                                                                                                        context -> executeRender(
                                                                                                                                                                                context,
                                                                                                                                                                                3))
                                                                                                                                                                // ──
                                                                                                                                                                // Carte
                                                                                                                                                                // 4
                                                                                                                                                                // ──
                                                                                                                                                                .then(Commands
                                                                                                                                                                        .literal(
                                                                                                                                                                                "more")
                                                                                                                                                                        .then(Commands
                                                                                                                                                                                .argument(
                                                                                                                                                                                        "pokemon4",
                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                .string())
                                                                                                                                                                                .suggests(
                                                                                                                                                                                        POKEMON_SUGGESTIONS)
                                                                                                                                                                                .then(Commands
                                                                                                                                                                                        .argument(
                                                                                                                                                                                                "shiny4",
                                                                                                                                                                                                BoolArgumentType
                                                                                                                                                                                                        .bool())
                                                                                                                                                                                        .then(Commands
                                                                                                                                                                                                .argument(
                                                                                                                                                                                                        "rarity4",
                                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                                .string())
                                                                                                                                                                                                .suggests(
                                                                                                                                                                                                        RARITY_SUGGESTIONS)
                                                                                                                                                                                                .executes(
                                                                                                                                                                                                        context -> executeRender(
                                                                                                                                                                                                                context,
                                                                                                                                                                                                                4))
                                                                                                                                                                                                .then(Commands
                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                "background4",
                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                        .string())
                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                BACKGROUND_SUGGESTIONS)
                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                context -> executeRender(
                                                                                                                                                                                                                        context,
                                                                                                                                                                                                                        4))
                                                                                                                                                                                                        .then(Commands
                                                                                                                                                                                                                .argument(
                                                                                                                                                                                                                        "effect4",
                                                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                                                .string())
                                                                                                                                                                                                                .suggests(
                                                                                                                                                                                                                        EFFECT_SUGGESTIONS)
                                                                                                                                                                                                                .executes(
                                                                                                                                                                                                                        context -> executeRender(
                                                                                                                                                                                                                                context,
                                                                                                                                                                                                                                4))
                                                                                                                                                                                                                // ──
                                                                                                                                                                                                                // Carte
                                                                                                                                                                                                                // 5
                                                                                                                                                                                                                // ──
                                                                                                                                                                                                                .then(Commands
                                                                                                                                                                                                                        .literal(
                                                                                                                                                                                                                                "more")
                                                                                                                                                                                                                        .then(Commands
                                                                                                                                                                                                                                .argument(
                                                                                                                                                                                                                                        "pokemon5",
                                                                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                                                                .string())
                                                                                                                                                                                                                                .suggests(
                                                                                                                                                                                                                                        POKEMON_SUGGESTIONS)
                                                                                                                                                                                                                                .then(Commands
                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                "shiny5",
                                                                                                                                                                                                                                                BoolArgumentType
                                                                                                                                                                                                                                                        .bool())
                                                                                                                                                                                                                                        .then(Commands
                                                                                                                                                                                                                                                .argument(
                                                                                                                                                                                                                                                        "rarity5",
                                                                                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                                                                                .string())
                                                                                                                                                                                                                                                .suggests(
                                                                                                                                                                                                                                                        RARITY_SUGGESTIONS)
                                                                                                                                                                                                                                                .executes(
                                                                                                                                                                                                                                                        context -> executeRender(
                                                                                                                                                                                                                                                                context,
                                                                                                                                                                                                                                                                5))
                                                                                                                                                                                                                                                .then(Commands
                                                                                                                                                                                                                                                        .argument(
                                                                                                                                                                                                                                                                "background5",
                                                                                                                                                                                                                                                                StringArgumentType
                                                                                                                                                                                                                                                                        .string())
                                                                                                                                                                                                                                                        .suggests(
                                                                                                                                                                                                                                                                BACKGROUND_SUGGESTIONS)
                                                                                                                                                                                                                                                        .executes(
                                                                                                                                                                                                                                                                context -> executeRender(
                                                                                                                                                                                                                                                                        context,
                                                                                                                                                                                                                                                                        5))
                                                                                                                                                                                                                                                        .then(Commands
                                                                                                                                                                                                                                                                .argument(
                                                                                                                                                                                                                                                                        "effect5",
                                                                                                                                                                                                                                                                        StringArgumentType
                                                                                                                                                                                                                                                                                .string())
                                                                                                                                                                                                                                                                .suggests(
                                                                                                                                                                                                                                                                        EFFECT_SUGGESTIONS)
                                                                                                                                                                                                                                                                .executes(
                                                                                                                                                                                                                                                                        context -> executeRender(
                                                                                                                                                                                                                                                                                context,
                                                                                                                                                                                                                                                                                5))))))))))))))))))))))))))))))))
                .then(Commands.literal("workshop")
                        .executes(context -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.sendToPlayer(player,
                                        new com.howlite.cobblemoncards.network.OpenWorkshopPayload());
                                return 1;
                            } catch (Exception e) {
                                context.getSource().sendFailure(
                                        Component.translatable("command.cobblemon-cards.workshop.player_only"));
                                return 0;
                            }
                        }))
                .then(Commands.literal("carddex")
                        .then(Commands.literal("fill")
                                .executes(context -> {
                                    try {
                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                        java.util.List<String> all = com.cobblemon.mod.common.api.pokemon.PokemonSpecies
                                                .getSpecies().stream()
                                                .map(s -> s.getName().toLowerCase())
                                                .distinct()
                                                .toList();
                                        java.util.List<String> attachment = new java.util.ArrayList<>(
                                                com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.getDiscoveredCards(player));
                                        attachment.clear();
                                        attachment.addAll(all);
                                        com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.setDiscoveredCards(
                                                player,
                                                attachment);
                                        com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.sendToPlayer(player,
                                                new com.howlite.cobblemoncards.network.SyncDiscoveredCardsPayload(
                                                        attachment));
                                        context.getSource()
                                                .sendSuccess(() -> Component
                                                        .translatable("command.cobblemon-cards.carddex.fill_success"),
                                                         true);
                                        return 1;
                                    } catch (Exception e) {
                                        context.getSource().sendFailure(Component.translatable(
                                                "command.cobblemon-cards.carddex.fill_error", e.getMessage()));
                                        return 0;
                                    }
                                })))
                .then(Commands.literal("fill_cabinet")
                        .executes(context -> {
                            try {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                net.minecraft.world.phys.BlockHitResult hitResult = (net.minecraft.world.phys.BlockHitResult) player
                                        .pick(5.0D, 0.0F, false);
                                net.minecraft.core.BlockPos pos = hitResult.getBlockPos();
                                net.minecraft.world.level.block.entity.BlockEntity be = player.level()
                                        .getBlockEntity(pos);
                                if (be instanceof com.howlite.cobblemoncards.block.entity.CardCabinetBlockEntity cabinetBE) {
                                    ItemStack dummyCard = new ItemStack(ModItems.CARD);
                                    com.howlite.cobblemoncards.component.CardData dummyData = new com.howlite.cobblemoncards.component.CardData(
                                            "pikachu", false, "common",
                                            com.howlite.cobblemoncards.component.CardStat.MOVEMENT_SPEED, 0.0f, 0,
                                            java.util.Optional.empty(), java.util.Optional.empty());
                                    dummyCard.set(ModDataComponents.CARD_DATA, dummyData);
                                    for (int i = 0; i < cabinetBE.getItems().size(); i++) {
                                        cabinetBE.getItems().set(i, dummyCard.copy());
                                    }
                                    cabinetBE.setChanged();
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.cobblemon-cards.fill_cabinet.success"),
                                                    true);
                                    return 1;
                                } else {
                                    context.getSource().sendFailure(Component
                                            .translatable("command.cobblemon-cards.fill_cabinet.not_looking_at"));
                                    return 0;
                                }
                            } catch (Exception e) {
                                context.getSource()
                                        .sendFailure(Component.translatable("command.cobblemon-cards.error"));
                                return 0;
                            }
                        }))
                .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(Commands.argument("pokemon", StringArgumentType.string())
                                        .suggests(POKEMON_SUGGESTIONS)
                                        .then(Commands.argument("shiny", BoolArgumentType.bool())
                                                .then(Commands.argument("rarity", StringArgumentType.string())
                                                        .suggests(RARITY_SUGGESTIONS)
                                                        .then(Commands.argument("stat", StringArgumentType.string())
                                                                .suggests(STAT_SUGGESTIONS)
                                                                .then(Commands
                                                                        .argument("stat_value",
                                                                                FloatArgumentType.floatArg(0f))
                                                                        .then(Commands
                                                                                .argument("background",
                                                                                        StringArgumentType.string())
                                                                                .suggests(BACKGROUND_SUGGESTIONS)
                                                                                .then(Commands
                                                                                        .argument("effect",
                                                                                                StringArgumentType
                                                                                                        .string())
                                                                                        .suggests(EFFECT_SUGGESTIONS)
                                                                                        .executes(
                                                                                                GiveCardCommand::execute))
                                                                                .executes(context -> executeInternal(
                                                                                        context, false, true)))
                                                                        .executes(context -> executeInternal(context,
                                                                                false, false))))
                                                        .executes(
                                                                context -> executeInternal(context, true, false))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        return executeInternal(context, false, false);
    }

    /**
     * Exécute /cobblecard render : construit les stacks de cartes et envoie
     * RenderCardPayload au joueur.
     * 
     * @param cardCount nombre de cartes à inclure (1 à 5)
     */
    private static int executeRender(CommandContext<CommandSourceStack> context, int cardCount) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<ItemStack> cards = new ArrayList<>();

            String[] suffixes = { "", "2", "3", "4", "5" };

            for (int i = 0; i < cardCount; i++) {
                String suf = suffixes[i];
                String pokemonId = StringArgumentType.getString(context, "pokemon" + suf).toLowerCase();
                boolean isShiny = BoolArgumentType.getBool(context, "shiny" + suf);
                String rarity = StringArgumentType.getString(context, "rarity" + suf).toLowerCase();

                // Background (optionnel)
                Optional<String> background = Optional.empty();
                try {
                    String bg = StringArgumentType.getString(context, "background" + suf);
                    if (!bg.equals("none"))
                        background = Optional.of(bg);
                } catch (IllegalArgumentException ignored) {
                }

                // Effect (optionnel)
                Optional<String> effect = Optional.empty();
                try {
                    String eff = StringArgumentType.getString(context, "effect" + suf);
                    if (!eff.equals("none"))
                        effect = Optional.of(eff);
                } catch (IllegalArgumentException ignored) {
                }

                // Si un effet est présent sans background, on attribue le background par défaut
                if (effect.isPresent() && background.isEmpty()) {
                    background = Optional.of(com.howlite.cobblemoncards.util.CardUtil.getDefaultBackground(pokemonId));
                }

                // Vérification de l'espèce
                String baseName = getBaseSpeciesName(pokemonId);
                com.cobblemon.mod.common.pokemon.Species species = com.howlite.cobblemoncards.util.CardUtil
                        .getSpecies(baseName);
                if (species == null) {
                    context.getSource().sendFailure(
                            Component.translatable("command.cobblemon-cards.invalid_pokemon", pokemonId));
                    return 0;
                }

                ItemStack cardStack = new ItemStack(com.howlite.cobblemoncards.item.ModItems.CARD);
                com.howlite.cobblemoncards.component.CardData data = new com.howlite.cobblemoncards.component.CardData(
                        pokemonId, isShiny, rarity,
                        com.howlite.cobblemoncards.component.CardStat.MOVEMENT_SPEED,
                        0.50f, 0, background, effect);
                cardStack.set(com.howlite.cobblemoncards.component.ModDataComponents.CARD_DATA, data);
                cards.add(cardStack);
            }

            com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.sendToPlayer(player, new com.howlite.cobblemoncards.network.RenderCardPayload(cards));
            context.getSource().sendSuccess(
                    () -> Component.translatable("command.cobblemon-cards.render.success", cardCount), false);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.cobblemon-cards.error"));
            return 0;
        }
    }

    private static int executeInternal(CommandContext<CommandSourceStack> context, boolean defaults,
            boolean backgroundOnly) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
            String pokemonId = StringArgumentType.getString(context, "pokemon").toLowerCase();
            boolean isShiny = BoolArgumentType.getBool(context, "shiny");
            String rarity = StringArgumentType.getString(context, "rarity").toLowerCase();

            CardStat stat = CardStat.MOVEMENT_SPEED;
            float statValue = 0.50f;
            Optional<String> background = Optional.empty();
            Optional<String> effect = Optional.empty();

            if (!defaults) {
                String statName = StringArgumentType.getString(context, "stat");
                stat = Arrays.stream(CardStat.values())
                        .filter(s -> s.getSerializedName().equalsIgnoreCase(statName))
                        .findFirst()
                        .orElse(CardStat.MOVEMENT_SPEED);
                statValue = FloatArgumentType.getFloat(context, "stat_value");

                try {
                    String bg = StringArgumentType.getString(context, "background");
                    if (!bg.equals("none"))
                        background = Optional.of(bg);
                } catch (IllegalArgumentException ignored) {
                }

                if (!backgroundOnly) {
                    try {
                        String eff = StringArgumentType.getString(context, "effect");
                        if (!eff.equals("none"))
                            effect = Optional.of(eff);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (effect.isPresent() && background.isEmpty()) {
                    background = Optional.of(com.howlite.cobblemoncards.util.CardUtil.getDefaultBackground(pokemonId));
                }
            }

            // Vérification de l'espèce
            String baseName = getBaseSpeciesName(pokemonId);
            Species species = com.howlite.cobblemoncards.util.CardUtil.getSpecies(baseName);
            if (species == null) {
                context.getSource()
                        .sendFailure(Component.translatable("command.cobblemon-cards.invalid_pokemon", pokemonId));
                return 0;
            }

            for (ServerPlayer player : targets) {
                ItemStack cardStack = new ItemStack(ModItems.CARD);
                CardData data = new CardData(pokemonId, isShiny, rarity, stat, statValue, 0, background, effect);
                cardStack.set(ModDataComponents.CARD_DATA, data);

                if (!player.getInventory().add(cardStack)) {
                    player.drop(cardStack, false);
                }
                com.howlite.cobblemoncards.util.CardAdvancementManager.checkAdvancements(player);
            }

            context.getSource().sendSuccess(() -> Component.translatable("command.cobblemon-cards.give.success"), true);
            return 1;

        } catch (Exception e) {
            context.getSource().sendFailure(Component.translatable("command.cobblemon-cards.error"));
            return 0;
        }
    }

    private static String getBaseSpeciesName(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("eternamax_"))
            return "eternatus";
        if (lower.endsWith("_rapidstrike_gmax"))
            return "urshifu";
        if (lower.endsWith("_alolan"))
            return lower.substring(0, lower.length() - "_alolan".length());
        if (lower.endsWith("_galarian"))
            return lower.substring(0, lower.length() - "_galarian".length());
        if (lower.endsWith("_hisuian"))
            return lower.substring(0, lower.length() - "_hisuian".length());
        if (lower.endsWith("_paldean_combat"))
            return "tauros";
        if (lower.endsWith("_paldean_blaze"))
            return "tauros";
        if (lower.endsWith("_paldean_aqua"))
            return "tauros";
        if (lower.endsWith("_paldean"))
            return lower.substring(0, lower.length() - "_paldean".length());
        if (lower.endsWith("_mega_x"))
            return lower.substring(0, lower.length() - "_mega_x".length());
        if (lower.endsWith("_mega_y"))
            return lower.substring(0, lower.length() - "_mega_y".length());
        if (lower.endsWith("_mega"))
            return lower.substring(0, lower.length() - "_mega".length());
        if (lower.endsWith("_gmax"))
            return lower.substring(0, lower.length() - "_gmax".length());
        if (lower.endsWith("_gigantamax"))
            return lower.substring(0, lower.length() - "_gigantamax".length());
        return lower;
    }
}