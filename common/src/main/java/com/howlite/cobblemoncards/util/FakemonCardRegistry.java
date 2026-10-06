package com.howlite.cobblemoncards.util;

import com.howlite.cobblemoncards.CobblemonCards;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Server-side whitelist of Fakemon species IDs that are allowed to generate
 * card drops and appear in booster packs, even when
 * {@code CobblemonCardsConfig.allowFakemonCards} is {@code false}.
 *
 * <p>Populated by {@link FakemonWhitelistReloader} from datapack JSON files at
 * {@code data/cobblemon-cards/fakemon_cards/*.json}.
 */
public final class FakemonCardRegistry {

    private static final Set<String> WHITELISTED = new HashSet<>();
    private static long revision;

    private FakemonCardRegistry() {}

    /** Replaces the current whitelist with the provided collection. */
    public static synchronized void reload(Collection<String> ids) {
        WHITELISTED.clear();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                WHITELISTED.add(id.trim().toLowerCase());
            }
        }
        revision++;
        CobblemonCards.LOGGER.info(
                "[CobblemonCards] Fakemon whitelist reloaded - {} species whitelisted.", WHITELISTED.size());
    }

    /**
     * Returns true if the given species ID is whitelisted via a datapack,
     * authorising card drops even when allowFakemonCards is false.
     */
    public static boolean isWhitelisted(String speciesId) {
        if (speciesId == null) return false;
        return WHITELISTED.contains(speciesId.toLowerCase());
    }

    /** Unmodifiable view of the current whitelist (useful for logging/commands). */
    public static Set<String> getWhitelistedIds() {
        return Collections.unmodifiableSet(WHITELISTED);
    }

    /** Monotonic revision used to invalidate derived caches after every datapack reload. */
    public static synchronized long getRevision() {
        return revision;
    }
}
