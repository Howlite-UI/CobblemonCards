package com.howlite.cobblemoncards.neoforge.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class NeoForgeLootCompat {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, CobblemonCards.MOD_ID);

    static {
        MODIFIERS.register("optional_boosters", () -> CompatBoosterModifier.CODEC);
    }

    private NeoForgeLootCompat() {
    }

    public static void register(IEventBus modEventBus) {
        MODIFIERS.register(modEventBus);
    }
}
