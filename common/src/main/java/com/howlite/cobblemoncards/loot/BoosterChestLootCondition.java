package com.howlite.cobblemoncards.loot;

import com.howlite.cobblemoncards.CobblemonCards;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Defense in depth when an eligible table is referenced by block/entity/fishing loot.
 * A container-like context still cannot prove that a container was naturally generated.
 */
public final class BoosterChestLootCondition implements LootItemCondition {
    public static final BoosterChestLootCondition INSTANCE = new BoosterChestLootCondition();
    public static final MapCodec<BoosterChestLootCondition> CODEC = MapCodec.unit(INSTANCE);
    public static final LootItemConditionType TYPE = new LootItemConditionType(CODEC);

    private static final Set<LootContextParam<?>> NON_CONTAINER_PARAMS = Set.of(
            LootContextParams.BLOCK_STATE, LootContextParams.BLOCK_ENTITY, LootContextParams.TOOL,
            LootContextParams.EXPLOSION_RADIUS, LootContextParams.DAMAGE_SOURCE,
            LootContextParams.DIRECT_ATTACKING_ENTITY,
            LootContextParams.LAST_DAMAGE_PLAYER, LootContextParams.ENCHANTMENT_LEVEL,
            LootContextParams.ENCHANTMENT_ACTIVE);

    private BoosterChestLootCondition() {}

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE,
                ResourceLocation.fromNamespaceAndPath(CobblemonCards.MOD_ID, "booster_chest_context"), TYPE);
    }

    static boolean isContainerContext(Predicate<LootContextParam<?>> hasParam) {
        // NeoForge chest minecarts include ATTACKING_ENTITY = the container vehicle.
        return hasParam.test(LootContextParams.ORIGIN)
                && NON_CONTAINER_PARAMS.stream().noneMatch(hasParam);
    }

    @Override
    public boolean test(LootContext context) {
        if (!isContainerContext(context::hasParam)) return false;
        // Vanilla vaults reuse chests/trial_chambers/reward and have the same parameter
        // keys as chest containers. Reject the verified gameplay source, while keeping
        // that shared table eligible when a natural chest/barrel actually generates it.
        BlockState source = context.getLevel().getBlockState(
                BlockPos.containing(context.getParam(LootContextParams.ORIGIN)));
        return !source.is(Blocks.VAULT) && !source.is(Blocks.TRIAL_SPAWNER)
                // Adding a second stack to a one-slot pot can discard the original loot.
                && !source.is(Blocks.DECORATED_POT);
    }

    @Override
    public LootItemConditionType getType() {
        return TYPE;
    }
}
