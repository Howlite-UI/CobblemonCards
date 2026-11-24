package com.howlite.cobblemoncards.fabric.item;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.item.custom.BinderItem;
import com.howlite.cobblemoncards.item.custom.BinderTier;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

@SuppressWarnings("null")
public class FabricBinderItem extends BinderItem implements Accessory {

    public FabricBinderItem(BinderTier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void getDynamicModifiers(ItemStack stack, SlotReference reference, AccessoryAttributeBuilder builder) {
        // Shared reader: BINDER_CONTENTS with a fallback to vanilla CONTAINER for unmigrated saves.
        Map<CardStat, Float> statTotals = com.howlite.cobblemoncards.util.CardStatUtil.collectStats(stack);

        for (Map.Entry<CardStat, Float> entry : statTotals.entrySet()) {
            CardStat stat = entry.getKey();
            float totalValue = entry.getValue();
            Holder<Attribute> attribute = getVanillaAttribute(stat);
            if (attribute != null && totalValue != 0) {
                // Config multiplier + application mode (flat vs percent) resolved in one place.
                float val = com.howlite.cobblemoncards.util.CardStatUtil.getAttributeModifierValue(stat, totalValue);
                if (val == 0.0f) continue;
                AttributeModifier.Operation operation =
                        com.howlite.cobblemoncards.util.CardStatUtil.getAttributeOperation(stat);

                String path = "binder_modifier_" + reference.slotName() + "_" + reference.slot() + "_" + stat.getSerializedName();
                builder.addExclusive(
                        attribute,
                        ResourceLocation.fromNamespaceAndPath("cobblemon-cards", path),
                        val,
                        operation
                );
            }
        }
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference reference) {
        return reference.slotName().equals("binder");
    }

    @Override
    public DropRule getDropRule(ItemStack stack, SlotReference reference, DamageSource source) {
        return CobblemonCardsConfig.keepBinderOnDeath ? DropRule.KEEP : DropRule.DEFAULT;
    }
}
