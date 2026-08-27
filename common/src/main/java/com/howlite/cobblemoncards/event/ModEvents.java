package com.howlite.cobblemoncards.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.util.FakemonCardRegistry;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import com.howlite.cobblemoncards.item.custom.loot.BoosterLootTable;
import com.howlite.cobblemoncards.util.CardStatUtil;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.Random;

public class ModEvents {
    private static final Random RANDOM = new Random();

    public static void registerEvents() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            handlePokemonDrop(event.getPlayer(), event.getPokemon());
            return Unit.INSTANCE;
        });

        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL, event -> {
            if (event.getKilled().getEntity() != null) {
                Pokemon pokemon = event.getKilled().getEntity().getPokemon();
                // Check if it doesn't belong to a player
                if (!pokemon.isPlayerOwned()) {
                    event.getBattle().getPlayers().forEach(player -> {
                        if (player instanceof ServerPlayer serverPlayer) {
                            handlePokemonDrop(serverPlayer, pokemon);
                        }
                    });
                }
            }
            return Unit.INSTANCE;
        });

        registerTrainerStatEvents();
    }

    /**
     * Hooks the Exp / Catch / Shiny "trainer" stats into Cobblemon.
     *
     * <p>Each handler reads the bonus from the player's equipped binder via
     * {@link CardStatUtil#getEquippedBonus}, which returns {@code 0} when the stat's config toggle is
     * off — so {@code enableTrainerStats} gates all three without extra checks.</p>
     *
     * <p>All three fire on the server thread.</p>
     */
    private static void registerTrainerStatEvents() {
        // --- Exp boost ---
        // Pre is cancelable, but we only ever adjust the amount and never cancel.
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.NORMAL, event -> {
            ServerPlayer owner = event.getPokemon().getOwnerPlayer();
            if (owner != null) {
                float percent = CardStatUtil.getEquippedBonus(owner, CardStat.EXP_BOOST);
                if (percent > 0f) {
                    float multiplier = Math.min(CobblemonCardsConfig.maxExpBoostMultiplier,
                            1.0f + (percent / 100.0f));
                    // Keep the multiplication in floating point and round at the end. Casting the
                    // multiplier to an int first would floor a 1.5x boost to 1x, silently doing nothing.
                    event.setExperience(Math.round(event.getExperience() * multiplier));
                }
            }
            return Unit.INSTANCE;
        });

        // --- Catch rate boost ---
        CobblemonEvents.POKEMON_CATCH_RATE.subscribe(Priority.NORMAL, event -> {
            if (event.getThrower() instanceof ServerPlayer thrower) {
                float percent = CardStatUtil.getEquippedBonus(thrower, CardStat.CATCH_BOOST);
                if (percent > 0f) {
                    float multiplier = Math.min(CobblemonCardsConfig.maxCatchBoostMultiplier,
                            1.0f + (percent / 100.0f));
                    event.setCatchRate(event.getCatchRate() * multiplier);
                }
            }
            return Unit.INSTANCE;
        });

        // --- Shiny chance boost ---
        // The rate is a "1 in N" value, so boosting it means DIVIDING: a smaller N is more likely.
        CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe(Priority.NORMAL, event -> {
            event.addModificationFunction((currentRate, player, pokemon) -> {
                if (player == null) {
                    return currentRate;
                }
                float percent = CardStatUtil.getEquippedBonus(player, CardStat.SHINY_CHANCE);
                if (percent <= 0f) {
                    return currentRate;
                }
                // Clamped to >= 1 so we can never divide by zero, and never make shinies rarer.
                float divisor = Math.max(1.0f, Math.min(CobblemonCardsConfig.maxShinyBoostDivisor,
                        1.0f + (percent / 100.0f)));
                return currentRate / divisor;
            });
            return Unit.INSTANCE;
        });
    }

    private static void handlePokemonDrop(ServerPlayer player, Pokemon pokemon) {
        // If Fakemon cards are globally disabled, skip species that are not from the
        // official Cobblemon namespace — UNLESS the species is explicitly whitelisted
        // via a datapack. Namespace-based detection is more reliable than dex numbers
        // because some Fakemon addons reuse dex slots within the 1-1025 range.
        if (!CobblemonCardsConfig.allowFakemonCards) {
            if (!isOfficialSpecies(pokemon.getSpecies())) {
                String speciesName = pokemon.getSpecies().getName();
                if (!FakemonCardRegistry.isWhitelisted(speciesName)) {
                    CobblemonCards.LOGGER.debug(
                            "[CobblemonCards] Skipping card drop for Fakemon '{}' (not whitelisted).",
                            speciesName);
                    return;
                }
                CobblemonCards.LOGGER.debug(
                        "[CobblemonCards] Fakemon '{}' is whitelisted — allowing drop.", speciesName);
            }
        }

        float dropBonus = CardStatUtil.getPlayerDropBonus(player);
        // Base chance is defined in the config (default 1.0f)
        float totalChance = CobblemonCardsConfig.cardDropChance + dropBonus;
        
        CobblemonCards.LOGGER.info("Calculated drop chance for player {}: {} (Base: {}, Bonus: {})", 
                player.getName().getString(), totalChance, CobblemonCardsConfig.cardDropChance, dropBonus);
        
        if (RANDOM.nextFloat() * 100f <= totalChance) {
            CobblemonCards.LOGGER.info("Drop successful! Generating card for {}", pokemon.getSpecies().getName());
            
            String pokemonId = pokemon.getSpecies().getName().toLowerCase();
            boolean isShiny = pokemon.getShiny();
            // Species-aware rarity: catching a legendary leans toward higher-rarity drops
            String rarity = BoosterLootTable.getRandomRarity(pokemon.getSpecies());
            
            // Config-aware pool: never mints a disabled stat, and never a trainer stat.
            CardStat randomStat = CardStatUtil.randomStat(RANDOM);

            // Stat value cohérente avec la rareté (même barème que les Boosters)
            float statValue = switch (rarity) {
                case "mythic"    -> 0.20f + RANDOM.nextFloat() * 0.05f;
                case "legendary" -> 0.12f + RANDOM.nextFloat() * 0.06f;
                case "epic"      -> 0.08f + RANDOM.nextFloat() * 0.04f;
                case "rare"      -> 0.04f + RANDOM.nextFloat() * 0.03f;
                case "uncommon"  -> 0.015f + RANDOM.nextFloat() * 0.015f;
                default          -> 0.005f + RANDOM.nextFloat() * 0.005f; // common
            };
            if (isShiny) statValue += 0.03f;

            // Small chance for Legendary / Mythic / Shiny cards to carry a trainer stat instead,
            // at a reduced value. Otherwise trainer stats are only earned by grading to 9+.
            CardStatUtil.RolledStat rolled =
                    CardStatUtil.applyLuckyTrainerStat(randomStat, statValue, rarity, isShiny, RANDOM);
            randomStat = rolled.stat();
            statValue = rolled.value();

            ItemStack cardStack = new ItemStack(ModItems.CARD);
            CardData cardData = new CardData(
                    pokemonId,
                    isShiny,
                    rarity,
                    randomStat,
                    statValue,
                    0,
                    Optional.empty(),
                    Optional.empty()
            );
            
            cardStack.set(ModDataComponents.CARD_DATA, cardData);
            
            if (!player.getInventory().add(cardStack)) {
                ItemEntity itemEntity = new ItemEntity(player.serverLevel(), player.getX(), player.getY(), player.getZ(), cardStack);
                player.serverLevel().addFreshEntity(itemEntity);
            }
            
            com.howlite.cobblemoncards.util.CardAdvancementManager.checkAdvancements(player);
            
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 2.0f);
        } else {
            CobblemonCards.LOGGER.info("Drop failed.");
        }
    }

    /**
     * Returns true if the given species belongs to the official Cobblemon registry
     * (namespace "cobblemon"). Falls back to the dex-number range [1, 1025] if no
     * ResourceIdentifier is available.
     */
    private static boolean isOfficialSpecies(Species species) {
        if (species.getResourceIdentifier() != null) {
            return "cobblemon".equals(species.getResourceIdentifier().getNamespace());
        }
        int dex = species.getNationalPokedexNumber();
        return dex >= 1 && dex <= 1025;
    }
}