package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.lang.reflect.Method;

/** Public RCT 0.19.2 API, resolved only after rctmod detection; no optional compile dependency. */
public final class RctCompat {
    private static Class<?> trainerType;
    private static Method instance, manager, trainerData, lootTable;
    private static boolean attempted, ready, failureLogged;

    private RctCompat() {}

    public static synchronized void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            ClassLoader loader = RctCompat.class.getClassLoader();
            Class<?> api = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod", false, loader);
            trainerType = Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob", false, loader);
            Class<?> service = Class.forName("com.gitlab.srcmc.rctmod.api.service.TrainerManager", false, loader);
            Class<?> data = Class.forName("com.gitlab.srcmc.rctmod.api.data.pack.TrainerMobData", false, loader);
            instance = api.getMethod("getInstance");
            manager = api.getMethod("getTrainerManager");
            trainerData = service.getMethod("getData", trainerType);
            lootTable = data.getMethod("getLootTableResource");
            if (lootTable.getReturnType() != ResourceLocation.class) throw new NoSuchMethodException("RCT loot table type");
            ready = true;
            CobblemonCards.LOGGER.info("Optional RCT victory loot API verified");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            fail(failure);
        }
    }

    public static boolean isRootReward(LootContext context, ResourceLocation table) {
        if (!ready || !(context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER) instanceof ServerPlayer)) return false;
        Object trainer = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        if (!trainerType.isInstance(trainer)) return false;
        try {
            Object data = trainerData.invoke(manager.invoke(instance.invoke(null)), trainer);
            // Single-trainer tables often nest group tables. Only the trainer's actual root gets a roll.
            return matchesRoot(table, (ResourceLocation) lootTable.invoke(data));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            fail(failure);
            return false;
        }
    }

    static boolean matchesRoot(ResourceLocation current, ResourceLocation root) {
        return CompatManager.isRctTable(current) && current.equals(root);
    }

    private static void fail(Throwable failure) {
        ready = false;
        if (!failureLogged) {
            failureLogged = true;
            CobblemonCards.LOGGER.warn("Optional RCT booster rewards unavailable: incompatible public loot API", failure);
        }
    }
}
