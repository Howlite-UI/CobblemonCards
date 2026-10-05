package com.howlite.cobblemoncards.event;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Species;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.util.CardStatUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spawning influence applied to every player-driven Cobblemon spawner.
 *
 * <p>Spawn details whose species matches an elemental type boosted by the player's equipped binder
 * become more likely to be selected. Registered through
 * {@code PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders()}.</p>
 *
 * <p>Cobblemon evaluates spawn weights on the server thread, so no synchronisation is needed here.</p>
 */
public class BinderSpawnModifier implements SpawningInfluence {
    private static final Logger LOGGER = LoggerFactory.getLogger("cobblemon-cards");

    /** Verbose logging of binder boosts on every rescan. */
    private static final boolean DEBUG = false;

    /** How often (in ticks) the equipped binder is re-scanned. */
    private static final int REFRESH_INTERVAL_TICKS = 40;

    /** Guards against the influence builder being registered by more than one loader entrypoint. */
    private static boolean influenceRegistered = false;

    private final ServerPlayer player;

    /** Per-type weight multipliers derived from the binder contents, refreshed on a timer. */
    private final Map<ElementalType, Float> typeMultipliers = new HashMap<>();
    /** Per-egg-group weight multipliers, refreshed alongside {@link #typeMultipliers}. */
    private final Map<EggGroup, Float> eggGroupMultipliers = new HashMap<>();
    /** Per-EV-yield weight multipliers, refreshed alongside {@link #typeMultipliers}. */
    private final Map<Stat, Float> evYieldMultipliers = new HashMap<>();
    private long lastRefreshTick = -REFRESH_INTERVAL_TICKS;

    public BinderSpawnModifier(@NotNull ServerPlayer player) {
        this.player = player;
    }

    /**
     * Hooks the binder influence into Cobblemon's per-player spawner weighting pipeline.
     *
     * <p>Safe to call from every loader entrypoint: the guard makes sure the builder is only ever
     * added once, since a double registration would square every boost multiplier.</p>
     */
    public static void registerSpawnInfluence() {
        if (influenceRegistered) {
            return;
        }
        influenceRegistered = true;
        PlayerSpawnerFactory.INSTANCE.getInfluenceBuilders().add(BinderSpawnModifier::new);
        LOGGER.info("[BinderSpawn] Registered binder spawn influence");
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail detail, @NotNull SpawnablePosition spawnablePosition, float weight) {
        if (!CobblemonCardsConfig.enableCardStats || !CobblemonCardsConfig.enableSpawnBoostStats) {
            return weight;
        }
        if (!(detail instanceof PokemonSpawnDetail pokemonDetail)) {
            return weight;
        }

        refreshIfNeeded();
        if (typeMultipliers.isEmpty() && eggGroupMultipliers.isEmpty() && evYieldMultipliers.isEmpty()) {
            return weight;
        }

        Species species = resolveSpecies(pokemonDetail);
        if (species == null) {
            return weight;
        }

        float multiplier = Math.max(
                multiplierFor(species.getPrimaryType()),
                multiplierFor(species.getSecondaryType()));

        // Egg group and EV yield are independent boost families, so they stack multiplicatively with
        // the elemental-type boost. Within each family we take the max rather than the product, so a
        // species in two boosted egg groups isn't double-counted.
        multiplier *= eggGroupMultiplier(species);
        multiplier *= evYieldMultiplier(species);

        return multiplier == 1.0f ? weight : weight * multiplier;
    }

    /** Highest boost among the egg groups this species belongs to, or 1.0 if none are boosted. */
    private float eggGroupMultiplier(Species species) {
        if (eggGroupMultipliers.isEmpty()) {
            return 1.0f;
        }
        float best = 1.0f;
        for (EggGroup group : species.getEggGroups()) {
            Float mult = eggGroupMultipliers.get(group);
            if (mult != null && mult > best) {
                best = mult;
            }
        }
        return best;
    }

    /** Highest boost among the EV yields this species actually gives, or 1.0 if none are boosted. */
    private float evYieldMultiplier(Species species) {
        if (evYieldMultipliers.isEmpty()) {
            return 1.0f;
        }
        float best = 1.0f;
        for (Map.Entry<Stat, Float> entry : evYieldMultipliers.entrySet()) {
            Integer yield = species.getEvYield().get(entry.getKey());
            if (yield != null && yield > 0 && entry.getValue() > best) {
                best = entry.getValue();
            }
        }
        return best;
    }

    @Override
    public boolean isExpired() {
        return player.isRemoved();
    }

    private float multiplierFor(ElementalType type) {
        return type == null ? 1.0f : typeMultipliers.getOrDefault(type, 1.0f);
    }

    // ------------------------------------------------------------------
    // Binder scanning
    // ------------------------------------------------------------------

    private void refreshIfNeeded() {
        long now = player.level().getGameTime();
        if (now - lastRefreshTick < REFRESH_INTERVAL_TICKS) {
            return;
        }
        lastRefreshTick = now;

        // Shared equipped-binder scan (handles slot ids, master-album gating and cosmetic cards).
        Map<CardStat, Float> spawnStats =
                CardStatUtil.collectEquippedStats(player, CobblemonCardsConfig::isSpawnBoostStat);

        typeMultipliers.clear();
        eggGroupMultipliers.clear();
        evYieldMultipliers.clear();

        for (Map.Entry<CardStat, Float> entry : spawnStats.entrySet()) {
            CardStat stat = entry.getKey();

            // Single source of truth for stat -> percentage (config multipliers applied exactly once).
            float percent = CardStatUtil.getEffectiveValue(stat, entry.getValue());
            if (percent <= 0f) {
                // Never reduce weights: a non-positive bonus simply means "no boost for this stat".
                continue;
            }

            float multiplier = Math.min(CobblemonCardsConfig.maxSpawnBoostMultiplier, 1.0f + (percent / 100.0f));

            if (CobblemonCardsConfig.isEggGroupStat(stat)) {
                EggGroup group = stat.getEggGroup();
                if (group != null) {
                    eggGroupMultipliers.merge(group, multiplier, Math::max);
                }
            } else if (CobblemonCardsConfig.isEvYieldStat(stat)) {
                Stat evStat = stat.getEvYieldStat();
                if (evStat != null) {
                    evYieldMultipliers.merge(evStat, multiplier, Math::max);
                }
            } else {
                ElementalType type = getElementalType(stat);
                if (type != null) {
                    typeMultipliers.merge(type, multiplier, Math::max);
                }
            }
        }

        if (DEBUG) {
            LOGGER.info("[BinderSpawn] {} binder boosts: types={} eggGroups={} evYields={}",
                    player.getName().getString(),
                    describe(typeMultipliers, ElementalType::getName),
                    describe(eggGroupMultipliers, EggGroup::name),
                    describe(evYieldMultipliers, Stat::getShowdownId));
        }
    }

    private static <K> String describe(Map<K, Float> multipliers, java.util.function.Function<K, Object> naming) {
        if (multipliers.isEmpty()) {
            return "none";
        }
        return multipliers.entrySet().stream()
                .map(e -> naming.apply(e.getKey()) + " x" + String.format(Locale.ROOT, "%.2f", e.getValue()))
                .collect(Collectors.joining(", "));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Species resolveSpecies(PokemonSpawnDetail detail) {
        PokemonProperties properties = detail.getPokemon();
        String name = properties == null ? null : properties.getSpecies();
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            return id == null ? null : PokemonSpecies.getByIdentifier(id);
        }
        return PokemonSpecies.getByName(name.toLowerCase(Locale.ROOT));
    }

    private static ElementalType getElementalType(CardStat stat) {
        return ElementalTypes.get(stat.getSerializedName().replace("_spawn", ""));
    }
}
