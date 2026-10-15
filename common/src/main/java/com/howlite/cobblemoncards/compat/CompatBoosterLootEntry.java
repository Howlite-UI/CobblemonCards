package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.function.Consumer;

/** Fabric pool entry uses the same live configuration and selection as the NeoForge modifier. */
public final class CompatBoosterLootEntry extends LootPoolSingletonContainer {
    public static final MapCodec<CompatBoosterLootEntry> CODEC = RecordCodecBuilder.mapCodec(instance ->
            singletonFields(instance).and(Codec.STRING.fieldOf("reward").forGetter(entry -> entry.rewardId))
                    .apply(instance, CompatBoosterLootEntry::new));
    public static final LootPoolEntryType TYPE = new LootPoolEntryType(CODEC);
    private final String rewardId;

    private CompatBoosterLootEntry(int weight, int quality, List<LootItemCondition> conditions,
                                   List<LootItemFunction> functions, String rewardId) {
        super(weight, quality, conditions, functions);
        this.rewardId = rewardId;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobblemonCards.MOD_ID, "optional_booster"), TYPE);
    }

    public static LootPool.Builder pool(CompatReward reward) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(simpleBuilder(
                (weight, quality, conditions, functions) -> new CompatBoosterLootEntry(
                        weight, quality, conditions, functions, reward.id())));
    }

    @Override public LootPoolEntryType getType() { return TYPE; }

    @Override protected void createItemStack(Consumer<ItemStack> output, LootContext context) {
        CompatReward reward = CompatManager.reward(rewardId);
        if (reward == null) return;
        ItemStack stack = BoosterRewardHelper.roll(reward, context.getRandom());
        if (!stack.isEmpty()) output.accept(stack);
    }
}
