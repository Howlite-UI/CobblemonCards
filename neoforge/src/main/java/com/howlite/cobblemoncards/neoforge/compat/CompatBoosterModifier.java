package com.howlite.cobblemoncards.neoforge.compat;

import com.howlite.cobblemoncards.compat.BoosterRewardHelper;
import com.howlite.cobblemoncards.compat.CompatManager;
import com.howlite.cobblemoncards.compat.CompatReward;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Appends boosters to the exact queried addon tables without modifying their original loot. */
public final class CompatBoosterModifier extends LootModifier {
    public static final MapCodec<CompatBoosterModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, CompatBoosterModifier::new));

    private CompatBoosterModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (CompatReward reward : CompatManager.rewardsForTable(context.getQueriedLootTableId())) {
            ItemStack booster = BoosterRewardHelper.roll(reward, context);
            if (!booster.isEmpty()) {
                generatedLoot.add(booster);
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
