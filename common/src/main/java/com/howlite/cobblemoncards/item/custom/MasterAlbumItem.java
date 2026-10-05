package com.howlite.cobblemoncards.item.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MasterAlbumItem extends BinderItem {
    public MasterAlbumItem(Properties properties) {
        super(BinderTier.MASTER, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Brillance activée par défaut pour le Master Album
    }

    public static boolean isCollectionComplete(Player player) {
        return false;
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(net.minecraft.network.chat.Component.translatable("tooltip.cobblemon-cards.master_album.description").withStyle(net.minecraft.ChatFormatting.GRAY));
        if (com.howlite.cobblemoncards.CobblemonCardsConfig.masterAlbumGivesStats) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        } else {
            tooltipComponents.add(net.minecraft.network.chat.Component.translatable("gui.cobblemon-cards.binder.pages", getTier().getPages()).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}