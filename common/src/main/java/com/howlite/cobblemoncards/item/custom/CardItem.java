package com.howlite.cobblemoncards.item.custom;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.network.InspectCardPayload;
import com.howlite.cobblemoncards.util.ClientAccess;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CardItem extends Item {

    public CardItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CardData data = stack.get(ModDataComponents.CARD_DATA);

        if (data != null) {
            String key = data.isShiny() ? "item.cobblemon-cards.card.shiny" : "item.cobblemon-cards.card.normal";
            Object nameArg;
            if (com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId()) && !data.pokemonId().startsWith("player_")) {
                nameArg = Component.translatable("cobblemon.species." + data.pokemonId());
            } else {
                nameArg = getFormattedName(data.pokemonId());
            }
            return Component.translatable(key, nameArg)
                    .withStyle(data.isShiny() ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        }

        return Component.translatable("item.cobblemon-cards.card").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // The right-clicking player sees the full inspection GUI
            PlatformHelper.INSTANCE.sendToPlayer(serverPlayer, new InspectCardPayload(stack.copy()));
            // Nearby players see the card rendered large in the world next to the holder
            com.howlite.cobblemoncards.network.ShowCardPayload showPayload =
                    new com.howlite.cobblemoncards.network.ShowCardPayload(
                            serverPlayer.getUUID(), stack.copy());
            serverPlayer.serverLevel().players().stream()
                    .filter(p -> p != serverPlayer && p.distanceTo(serverPlayer) <= 16f)
                    .forEach(p -> PlatformHelper.INSTANCE.sendToPlayer(p, showPayload));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CardData data = stack.get(ModDataComponents.CARD_DATA);

        if (data != null) {
            if (ClientAccess.isShiftDown()) {
                ChatFormatting rarityColor = getRarityColor(data);

                if (com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId())) {
                    // Séparateur décoratif
                    tooltipComponents.add(Component.literal("─────────────────").withStyle(ChatFormatting.DARK_GRAY));
                    tooltipComponents.add(Component.translatable("tooltip.cobblemon-cards.cosmetic_card")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    tooltipComponents.add(Component.empty());
                } else {
                    // 1. Statistique (mise en avant avec couleur de rareté)
                    boolean hasMainStat = com.howlite.cobblemoncards.CobblemonCardsConfig.enableCardStats
                            && data.stat() != null
                            && com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(data.stat(), data.statValue()) > 0f;
                    boolean hasTrainerStat = com.howlite.cobblemoncards.CobblemonCardsConfig.enableCardStats
                            && data.trainerStat() != null && data.trainerStat().isPresent()
                            && data.trainerStatValue() != null && data.trainerStatValue().isPresent()
                            && com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(data.trainerStat().get(), data.trainerStatValue().get()) > 0f;

                    // Séparateur décoratif supérieur
                    tooltipComponents.add(Component.literal("─────────────────").withStyle(ChatFormatting.DARK_GRAY));

                    if (hasMainStat || hasTrainerStat) {
                        if (hasMainStat) {
                            String formattedValue = com.howlite.cobblemoncards.CobblemonCardsConfig.displayPercentStatOnCards
                                    ? com.howlite.cobblemoncards.util.CardStatUtil.formatValue(data.stat(), data.statValue())
                                    : String.valueOf(data.statValue());

                            tooltipComponents.add(
                                Component.literal("  " + formattedValue + " ")
                                    .withStyle(Style.EMPTY.withColor(rarityColor).withBold(true))
                                    .append(data.stat().getTranslatedName()
                                        .copy().withStyle(Style.EMPTY.withColor(rarityColor).withBold(false)))
                            );
                        }

                        if (hasTrainerStat) {
                            CardStat tStat = data.trainerStat().get();
                            float tVal = data.trainerStatValue().get();
                            String formattedTValue = com.howlite.cobblemoncards.CobblemonCardsConfig.displayPercentStatOnCards
                                    ? com.howlite.cobblemoncards.util.CardStatUtil.formatValue(tStat, tVal)
                                    : String.valueOf(tVal);
                            tooltipComponents.add(
                                Component.literal("  ★ " + formattedTValue + " ")
                                    .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true))
                                    .append(tStat.getTranslatedName()
                                        .copy().withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(false)))
                            );
                        }

                        tooltipComponents.add(Component.empty());
                    }
                }

                // 2. Pokémon (Label + Valeur WHITE)
                tooltipComponents.add(
                    Component.literal("  ")
                        .append(Component.translatable("tooltip.cobblemon-cards.label.pokemon")
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" " + getFormattedName(data.pokemonId()))
                            .withStyle(ChatFormatting.WHITE))
                );

                // 3. Rareté (Label + Valeur colorée selon rareté)
                tooltipComponents.add(
                    Component.literal("  ")
                        .append(Component.translatable("tooltip.cobblemon-cards.label.rarity")
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" "))
                        .append(Component.translatable("rarity.cobblemon-cards." + data.rarity().toLowerCase())
                            .withStyle(rarityColor))
                );

                // 4. Effet Holo (Label GRAY | Valeur LIGHT_PURPLE)
                data.effect().ifPresent(effect -> {
                    String holoKey = "holo.cobblemon-cards." + effect;
                    tooltipComponents.add(
                        Component.literal("  ")
                            .append(Component.translatable("tooltip.cobblemon-cards.label.holo")
                                .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(" "))
                            .append(Component.translatable(holoKey)
                                .withStyle(ChatFormatting.LIGHT_PURPLE))
                    );
                });

                // 5. Arrière-plan (Label GRAY | Valeur YELLOW)
                data.background().ifPresent(bg -> {
                    tooltipComponents.add(
                        Component.literal("  ")
                            .append(Component.translatable("tooltip.cobblemon-cards.label.background")
                                .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(" " + capitalize(bg))
                                .withStyle(ChatFormatting.YELLOW))
                    );
                });

                // 6. Shiny (Label GRAY | Valeur GOLD BOLD si vrai, GRAY si faux)
                Component shinyValue = data.isShiny()
                    ? Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("tooltip.cobblemon-cards.yes")
                            .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)))
                    : Component.translatable("tooltip.cobblemon-cards.no")
                        .withStyle(ChatFormatting.DARK_GRAY);

                tooltipComponents.add(
                    Component.literal("  ")
                        .append(Component.translatable("tooltip.cobblemon-cards.label.shiny")
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" "))
                        .append(shinyValue)
                );

                // 7. Grade (Label GRAY | Valeur WHITE)
                if (data.grade() > 0) {
                    tooltipComponents.add(
                        Component.literal("  ")
                            .append(Component.translatable("tooltip.cobblemon-cards.label.grade")
                                .withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(" " + data.grade())
                                .withStyle(ChatFormatting.WHITE))
                    );
                }

                // Séparateur décoratif inférieur
                tooltipComponents.add(Component.literal("─────────────────").withStyle(ChatFormatting.DARK_GRAY));

            } else {
                // Hint "Press SHIFT" avec icône clé
                tooltipComponents.add(
                    Component.literal("⬆ ").withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.translatable("tooltip.cobblemon-cards.press_shift")
                            .withStyle(ChatFormatting.DARK_GRAY))
                );
                // Hint to right-click for card inspection
                tooltipComponents.add(
                    Component.literal("▶ ").withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.translatable("tooltip.cobblemon-cards.right_click_inspect")
                            .withStyle(ChatFormatting.DARK_GRAY))
                );
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * Retourne la couleur de ChatFormatting correspondant à la rareté de la carte.
     */
    private ChatFormatting getRarityColor(CardData data) {
        if (data == null) return ChatFormatting.GRAY;
        if (data.isShiny()) return ChatFormatting.GOLD;
        return switch (data.rarity().toLowerCase()) {
            case "uncommon"  -> ChatFormatting.GREEN;
            case "rare"      -> ChatFormatting.AQUA;
            case "epic"      -> ChatFormatting.LIGHT_PURPLE;
            case "legendary" -> ChatFormatting.GOLD;
            case "mythic"    -> ChatFormatting.RED;
            default          -> ChatFormatting.WHITE;
        };
    }

    private String getFormattedName(String id) {
        if (id == null) return "";
        if (id.startsWith("player_")) {
            String name = id.substring("player_".length());
            int underscoreIdx = name.indexOf('_');
            if (underscoreIdx != -1) {
                name = name.substring(underscoreIdx + 1);
            }
            if (name.isEmpty()) return "";
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return capitalize(id);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
