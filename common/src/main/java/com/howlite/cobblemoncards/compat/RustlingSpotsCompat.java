package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Optional adapter for the public server interaction API shipped by Rustling Spots 5.0.1.
 * Invoke registration only after the loader has detected {@code rustlingspots}.
 */
public final class RustlingSpotsCompat {
    private static final String API_PACKAGE = "net.levelscraft7.rustlingspots.api.";
    private static final Map<Object, Boolean> ATTEMPTED_SPOTS = new WeakHashMap<>();
    private static boolean registrationAttempted;
    private static boolean callbackFailureLogged;
    private static Object callback;

    private RustlingSpotsCompat() {
    }

    public static synchronized boolean register() {
        if (registrationAttempted) {
            return callback != null;
        }
        registrationAttempted = true;

        try {
            ClassLoader loader = RustlingSpotsCompat.class.getClassLoader();
            Class<?> api = Class.forName(API_PACKAGE + "RustlingSpotAddonApi", false, loader);
            Class<?> callbackType = Class.forName(API_PACKAGE + "RustlingSpotInteractionCallback", false, loader);
            Class<?> eventType = Class.forName(API_PACKAGE + "RustlingSpotInteractionEvent", false, loader);
            Class<?> resultType = Class.forName(API_PACKAGE + "RustlingSpotInteractionResult", false, loader);
            if (!callbackType.isInterface() || !resultType.isEnum()) {
                throw new NoSuchMethodException("Unsupported Rustling Spots interaction API types");
            }

            Method register = api.getMethod("registerInteractionCallback", callbackType);
            Method interaction = callbackType.getMethod("onInteract", eventType);
            if (!Modifier.isStatic(register.getModifiers()) || register.getReturnType() != void.class
                    || interaction.getReturnType() != resultType) {
                throw new NoSuchMethodException("Unsupported Rustling Spots interaction API signatures");
            }
            for (Method method : callbackType.getMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) && !method.equals(interaction)) {
                    throw new NoSuchMethodException("Unsupported Rustling Spots callback methods");
                }
            }

            Method player = accessor(eventType, "player", ServerPlayer.class);
            Method level = accessor(eventType, "level", ServerLevel.class);
            Method random = accessor(eventType, "random", RandomSource.class);
            Method spot = eventType.getMethod("spot");
            Method family = eventType.getMethod("family");
            Class<?> spotType = spot.getReturnType();
            if (!spotType.getName().equals("net.levelscraft7.rustlingspots.spot.RustlingSpot")
                    || spotType.getMethod("equals", Object.class).getDeclaringClass() != Object.class
                    || spotType.getMethod("hashCode").getDeclaringClass() != Object.class
                    || !family.getReturnType().isEnum()) {
                throw new NoSuchMethodException("Unsupported Rustling Spots reward context");
            }
            Object pass = resultType.getField("PASS").get(null);

            Object adapter = Proxy.newProxyInstance(loader, new Class<?>[]{callbackType}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "CobblemonCards Rustling Spots reward callback";
                        default -> null;
                    };
                }
                if (method.equals(interaction) && args != null && args.length == 1) {
                    try {
                        reward(args[0], player, level, random, spot, family);
                    } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                        logCallbackFailure(failure);
                    }
                }
                // The addon owns normal rewards and spot removal, including other addon handlers.
                return pass;
            });
            register.invoke(null, adapter);
            callback = adapter;
            CobblemonCards.LOGGER.info("[CobblemonCards] Registered optional Rustling Spots booster rewards");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            CobblemonCards.LOGGER.warn(
                    "[CobblemonCards] Rustling Spots booster rewards unavailable: public interaction API is incompatible",
                    failure);
            return false;
        }
    }

    private static Method accessor(Class<?> eventType, String name, Class<?> expectedType)
            throws NoSuchMethodException {
        Method method = eventType.getMethod(name);
        if (method.getReturnType() != expectedType) {
            throw new NoSuchMethodException("Unsupported Rustling Spots accessor: " + name);
        }
        return method;
    }

    private static void reward(Object event, Method playerAccessor, Method levelAccessor,
                               Method randomAccessor, Method spotAccessor, Method familyAccessor)
            throws ReflectiveOperationException {
        if (!CompatConfig.rustlingEnabled()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) playerAccessor.invoke(event);
        ServerLevel level = (ServerLevel) levelAccessor.invoke(event);
        MinecraftServer server = player.getServer();
        if (server == null || !server.isSameThread() || player.isRemoved() || player.serverLevel() != level) {
            return;
        }

        Object spot = spotAccessor.invoke(event);
        if (spot == null || ATTEMPTED_SPOTS.putIfAbsent(spot, Boolean.TRUE) != null) {
            return;
        }
        // RustlingSpot uses identity equality. A weak key lives while the addon retains that spot,
        // preventing a reroll if normal reward resolution throws before spot removal.
        String family = ((Enum<?>) familyAccessor.invoke(event)).name().toLowerCase(Locale.ROOT);
        RandomSource random = (RandomSource) randomAccessor.invoke(event);
        ItemStack reward = BoosterRewardHelper.roll(
                CompatConfig.rustlingChance(), CompatConfig.rustlingSelection(family), random);
        BoosterRewardHelper.give(player, reward);
    }

    private static void logCallbackFailure(Throwable failure) {
        if (!callbackFailureLogged) {
            callbackFailureLogged = true;
            CobblemonCards.LOGGER.warn(
                    "[CobblemonCards] Rustling Spots booster callback failed; preserving normal spot rewards",
                    failure);
        }
    }
}
