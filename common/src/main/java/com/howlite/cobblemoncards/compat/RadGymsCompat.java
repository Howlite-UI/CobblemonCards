package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.lang.reflect.Method;

/** Reads Rad Gyms 0.5.0's public active-gym context without linking its Kotlin classes. */
public final class RadGymsCompat {
    private static Method activeGym, gymType;
    private static boolean attempted, ready, failureLogged;

    private RadGymsCompat() {}

    public static synchronized void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            ClassLoader loader = RadGymsCompat.class.getClassLoader();
            Class<?> state = Class.forName("lol.gito.radgyms.common.world.state.RadGymsState", false, loader);
            Class<?> gym = Class.forName("lol.gito.radgyms.common.api.dto.gym.Gym", false, loader);
            activeGym = state.getMethod("getGymForPlayer", ServerPlayer.class);
            gymType = gym.getMethod("getType");
            if (activeGym.getReturnType() != gym || gymType.getReturnType() != String.class) {
                throw new NoSuchMethodException("Rad Gyms active-gym context");
            }
            ready = true;
            CobblemonCards.LOGGER.info("Optional Rad Gyms reward context API verified");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            fail(failure);
        }
    }

    public static BoosterRewardHelper.Selection selection(LootContext context) {
        if (!ready) return null;
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return null;
        try {
            Object gym = activeGym.invoke(null, player);
            return gym == null ? BoosterRewardHelper.Selection.CLASSIC
                    : BoosterRewardHelper.Selection.fromToken((String) gymType.invoke(gym));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            fail(failure);
            return null;
        }
    }

    private static void fail(Throwable failure) {
        ready = false;
        if (!failureLogged) {
            failureLogged = true;
            CobblemonCards.LOGGER.warn("Optional Rad Gyms booster rewards unavailable: incompatible public gym API", failure);
        }
    }
}
