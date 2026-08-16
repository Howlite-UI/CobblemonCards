package com.howlite.cobblemoncards.item.custom;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;

import com.howlite.cobblemoncards.util.BinderLocator;
import net.minecraft.server.level.ServerPlayer;

public class BinderItem extends Item {
    private final BinderTier tier;

    public BinderItem(BinderTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public BinderTier getTier() {
        return tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!com.howlite.cobblemoncards.CobblemonCardsConfig.isBinderTierEnabled(tier)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                    Component.translatable("message.cobblemon-cards.binder_tier_disabled")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(itemStack);
        }

        if (player.isShiftKeyDown()) {
            if (PlatformHelper.INSTANCE.equipItem(player, itemStack)) {
                return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
            }
            return InteractionResultHolder.pass(itemStack);
        }

        // Bruit d'ouverture (On utilise le son du livre pour faire "classeur")
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);

        if (!level.isClientSide()) {
            PlatformHelper.INSTANCE.openBinderMenu((ServerPlayer) player, BinderLocator.hand(hand), itemStack.getHoverName());
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    public Holder<Attribute> getVanillaAttribute(CardStat stat) {
        return switch (stat) {
            case MINING_SPEED -> Attributes.MINING_EFFICIENCY;
            case MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case LUCK -> Attributes.LUCK;
            case ARMOR -> Attributes.ARMOR;
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            default -> null;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Affichage du nombre de pages
        tooltipComponents.add(Component.translatable("gui.cobblemon-cards.binder.pages", tier.getPages()).withStyle(ChatFormatting.GRAY));

        // Master tier binders do not provide stats due to their insane space limits.
        // If you want stats from your cards, use a netherite binder
        // There is a config option to allow master binders to provide stats if desired, but it is not recommended.
        if (tier == BinderTier.MASTER && !CobblemonCardsConfig.doesMasterBinderProvideStats){
          super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
          return;
        }

        // Shared reader: BINDER_CONTENTS with a fallback to vanilla CONTAINER for unmigrated saves.
        Map<CardStat, Float> statTotals = com.howlite.cobblemoncards.util.CardStatUtil.collectStats(stack);

        boolean hasHeader = false;
        for (Map.Entry<CardStat, Float> entry : statTotals.entrySet()) {
            CardStat stat = entry.getKey();
            float totalValue = entry.getValue();

            if (getVanillaAttribute(stat) == null && totalValue > 0) {
                if (!hasHeader) {
                    tooltipComponents.add(Component.empty());
                    tooltipComponents.add(Component.translatable("gui.cobblemon-cards.binder.stats_bonus").withStyle(ChatFormatting.GRAY));
                    hasHeader = true;
                }

                float finalValue = com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(stat, totalValue);
                if (finalValue <= 0) continue;
                String formattedValue = com.howlite.cobblemoncards.util.CardStatUtil.formatValue(stat, totalValue);

                tooltipComponents.add(Component.literal(formattedValue + " ").append(stat.getTranslatedName())
                        .withStyle(ChatFormatting.AQUA));
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
