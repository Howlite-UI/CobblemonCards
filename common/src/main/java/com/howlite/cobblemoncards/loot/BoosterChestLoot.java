package com.howlite.cobblemoncards.loot;

import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Shared server policy and unchanged chest booster pool; loaders only supply their mutation API. */
public final class BoosterChestLoot {
    private static final BoosterPoolInjection INJECTION = new BoosterPoolInjection();
    private static List<String> additional;
    private static List<String> excluded;
    private static BoosterLootTableMatcher matcher;

    private BoosterChestLoot() {}

    private static synchronized BoosterLootTableMatcher matcher() {
        List<String> currentAdditional = copy(CobblemonCardsConfig.additionalBoosterLootTables);
        List<String> currentExcluded = copy(CobblemonCardsConfig.excludedBoosterLootTables);
        if (matcher == null || !currentAdditional.equals(additional) || !currentExcluded.equals(excluded)) {
            matcher = BoosterLootTableMatcher.create(currentAdditional, currentExcluded,
                    warning -> CobblemonCards.LOGGER.warn("[Booster chest loot] {}", warning));
            additional = currentAdditional;
            excluded = currentExcluded;
        }
        return matcher;
    }

    private static List<String> copy(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    public static void inject(ResourceLocation id, LootContextParamSet paramSet, Object target,
                              Consumer<LootPool> addPool) {
        BoosterLootTableMatcher.Decision decision = matcher().match(id, paramSet);
        float chance = CobblemonCardsConfig.boosterChestSpawnChance / 100.0f;
        boolean enabled = CobblemonCardsConfig.enableBoosterChestSpawn;
        boolean injected = false;
        String result = "ineligible";
        if (decision.eligible()) {
            if (!enabled) {
                result = "disabled";
            } else if (!Float.isFinite(chance) || chance <= 0.0f) {
                result = "zero_or_invalid_chance";
            } else {
                injected = INJECTION.injectOnce(target, () -> addPool.accept(pool(chance).build()));
                result = injected ? "injected" : "already_injected";
            }
        }
        if (CobblemonCardsConfig.debugBoosterLootTables) {
            CobblemonCards.LOGGER.info(
                    "[Booster chest loot] table={} eligible={} reason={} enabled={} chancePercent={} injected={} result={}",
                    id, decision.eligible(), decision.reason(), enabled,
                    CobblemonCardsConfig.boosterChestSpawnChance, injected, result);
        }
    }

    private static LootPool.Builder pool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK).setWeight(10))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN1).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN2).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN3).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN4).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN5).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN6).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN7).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN8).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN9).setWeight(3))
                .when(() -> BoosterChestLootCondition.INSTANCE)
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }
}
