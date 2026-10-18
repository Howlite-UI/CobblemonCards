package com.howlite.cobblemoncards.loot;

import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoosterChestLootConditionTest {
    @Test
    void containerContextAcceptsItsOriginAndOptionalPlayer() {
        assertTrue(BoosterChestLootCondition.isContainerContext(Set.of(LootContextParams.ORIGIN)::contains));
        assertTrue(BoosterChestLootCondition.isContainerContext(
                Set.of(LootContextParams.ORIGIN, LootContextParams.THIS_ENTITY)::contains));
        // NeoForge also supplies the container minecart through ATTACKING_ENTITY.
        assertTrue(BoosterChestLootCondition.isContainerContext(
                Set.of(LootContextParams.ORIGIN, LootContextParams.ATTACKING_ENTITY)::contains));
        assertFalse(BoosterChestLootCondition.isContainerContext(Set.of(LootContextParams.ORIGIN,
                LootContextParams.ATTACKING_ENTITY, LootContextParams.DAMAGE_SOURCE)::contains));
        assertFalse(BoosterChestLootCondition.isContainerContext(ignored -> false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "minecraft:blocks/chest", "minecraft:blocks/trapped_chest", "minecraft:blocks/ender_chest",
            "minecraft:blocks/barrel", "minecraft:blocks/dispenser", "minecraft:blocks/decorated_pot"
    })
    void breakingAnOrdinaryContainerCannotSatisfyTheBoosterCondition(String blockLootTable) {
        // These are the parameters of ordinary block drops, regardless of a referenced table's ID/type.
        Set<LootContextParam<?>> blockContext = Set.of(
                LootContextParams.ORIGIN, LootContextParams.BLOCK_STATE,
                LootContextParams.BLOCK_ENTITY, LootContextParams.TOOL);
        assertFalse(BoosterChestLootCondition.isContainerContext(blockContext::contains), blockLootTable);
    }

    @Test
    void everyBlockEntityFishingAndEnchantmentMarkerRejectsEvenAContainerNamedTable() {
        for (LootContextParam<?> marker : List.of(
                LootContextParams.BLOCK_STATE, LootContextParams.BLOCK_ENTITY, LootContextParams.TOOL,
                LootContextParams.EXPLOSION_RADIUS, LootContextParams.DAMAGE_SOURCE,
                LootContextParams.DIRECT_ATTACKING_ENTITY,
                LootContextParams.LAST_DAMAGE_PLAYER, LootContextParams.ENCHANTMENT_LEVEL,
                LootContextParams.ENCHANTMENT_ACTIVE)) {
            Set<LootContextParam<?>> parameters = new HashSet<>();
            parameters.add(LootContextParams.ORIGIN);
            parameters.add(marker);
            assertFalse(BoosterChestLootCondition.isContainerContext(parameters::contains), marker.toString());
        }
        // Fishing supplies TOOL even though THIS_ENTITY may also be a player like a chest opener.
        assertFalse(BoosterChestLootCondition.isContainerContext(
                Set.of(LootContextParams.ORIGIN, LootContextParams.THIS_ENTITY, LootContextParams.TOOL)::contains));
    }
}
