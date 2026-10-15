package com.howlite.cobblemoncards.fabric.compat;

import com.howlite.cobblemoncards.compat.CompatBoosterLootEntry;
import com.howlite.cobblemoncards.compat.CompatManager;
import com.howlite.cobblemoncards.compat.CompatReward;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** Adds independent pools to verified addon reward tables, including datapack overrides. */
public final class FabricLootCompat {
    private static boolean registered;

    private FabricLootCompat() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CompatBoosterLootEntry.register();
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            for (CompatReward reward : CompatManager.rewardsForTable(key.location())) {
                // Enablement and chance are evaluated when the server actually rolls loot.
                if (CompatManager.isDetected(reward.integration())) {
                    tableBuilder.pool(CompatBoosterLootEntry.pool(reward).build());
                }
            }
        });
    }
}
