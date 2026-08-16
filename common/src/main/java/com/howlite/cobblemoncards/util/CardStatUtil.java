package com.howlite.cobblemoncards.util;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CardStatUtil {

    // ------------------------------------------------------------------
    // Binder contents
    // ------------------------------------------------------------------

    /**
     * Single source of truth for reading the cards stored in a binder-like stack.
     * Prefers the unlimited {@link ModDataComponents#BINDER_CONTENTS} component and falls back to the
     * vanilla {@link DataComponents#CONTAINER} component for saves that predate the migration.
     */
    public static Iterable<ItemStack> getBinderContents(ItemStack binderStack) {
        if (binderStack == null) {
            return Collections.emptyList();
        }
        List<ItemStack> binderItems = binderStack.get(ModDataComponents.BINDER_CONTENTS);
        if (binderItems != null) {
            return binderItems.stream().filter(stack -> !stack.isEmpty()).toList();
        }
        return binderStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems();
    }

    /**
     * Sums the <em>raw</em> stat values of every non-cosmetic card in the binder. Cosmetic cards never
     * contribute stats. Feed the totals through {@link #getEffectiveValue(CardStat, float)} before
     * displaying or applying them.
     *
     * @param filter optional stat filter, e.g. {@code CobblemonCardsConfig::isSpawnStat}
     */
    public static Map<CardStat, Float> collectStats(ItemStack binderStack, Predicate<CardStat> filter) {
        Map<CardStat, Float> totals = new EnumMap<>(CardStat.class);
        collectStats(binderStack, filter, totals);
        return totals;
    }

    /** See {@link #collectStats(ItemStack, Predicate)}; collects every stat. */
    public static Map<CardStat, Float> collectStats(ItemStack binderStack) {
        return collectStats(binderStack, null);
    }

    /** Accumulating variant of {@link #collectStats(ItemStack, Predicate)}, for summing several binders. */
    public static void collectStats(ItemStack binderStack, Predicate<CardStat> filter, Map<CardStat, Float> out) {
        for (ItemStack contentStack : getBinderContents(binderStack)) {
            CardData cardData = contentStack.get(ModDataComponents.CARD_DATA);
            if (cardData == null || cardData.stat() == null || CardUtil.isCosmeticCard(cardData.pokemonId())) {
                continue;
            }
            if (filter != null && !filter.test(cardData.stat())) {
                continue;
            }
            out.merge(cardData.stat(), cardData.statValue(), Float::sum);
        }
    }

    /**
     * How a stat is applied in game.
     * <ul>
     *     <li>{@link #FLAT} - added as a raw value to the attribute ({@link AttributeModifier.Operation#ADD_VALUE}),
     *     e.g. +2 armor. Displayed without a percent sign.</li>
     *     <li>{@link #PERCENT} - added as a fraction of the base value
     *     ({@link AttributeModifier.Operation#ADD_MULTIPLIED_BASE}), e.g. +5% movement speed.</li>
     * </ul>
     */
    public enum StatApplication {
        FLAT,
        PERCENT
    }

    /** Returns how the given stat is applied to the player / world. */
    public static StatApplication getApplication(CardStat stat) {
        if (stat == null) return StatApplication.PERCENT;
        return switch (stat) {
            case MAX_HEALTH, ARMOR, LUCK, MINING_SPEED -> StatApplication.FLAT;
            default -> StatApplication.PERCENT;
        };
    }

    /** @return true if the stat is applied as a flat (additive) value rather than a percentage. */
    public static boolean isFlat(CardStat stat) {
        return getApplication(stat) == StatApplication.FLAT;
    }

    /** @return the {@link AttributeModifier.Operation} matching the stat's application mode. */
    public static AttributeModifier.Operation getAttributeOperation(CardStat stat) {
        return isFlat(stat)
                ? AttributeModifier.Operation.ADD_VALUE
                : AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    }

    /**
     * Single source of truth for converting a raw stat value (as stored on a card, e.g. 0.05)
     * into the effective bonus displayed/applied in game.
     * <p>
     * {@link CobblemonCardsConfig#getStatMultiplier(CardStat)} is applied EXACTLY ONCE here, so every
     * caller (tooltips, GUIs, attribute modifiers, spawn boosts) must go through this method and must
     * not multiply by the config multiplier or by 100 again.
     * <p>
     * For {@link StatApplication#PERCENT} stats the result is expressed in percent (e.g. {@code 5.0f == +5%}).
     * For {@link StatApplication#FLAT} stats the result is the raw amount added to the attribute
     * (e.g. {@code 5.0f == +5 armor}).
     *
     * @param stat           the stat the value belongs to
     * @param totalStatValue the summed raw stat value(s)
     * @return the effective bonus
     */
    public static float getEffectiveValue(CardStat stat, float totalStatValue) {
        if (stat == null) return 0f;
        return totalStatValue * CobblemonCardsConfig.getStatMultiplier(stat);
    }

    /** Convenience overload for a single card. See {@link #getEffectiveValue(CardStat, float)}. */
    public static float getEffectiveValue(CardData data) {
        if (data == null) return 0f;
        return getEffectiveValue(data.stat(), data.statValue());
    }

    /**
     * Value fed to an {@link AttributeModifier}, matching {@link #getAttributeOperation(CardStat)}.
     * Percentage stats are converted from percent (e.g. 5.0) to a fraction (0.05) since
     * {@code ADD_MULTIPLIED_BASE} expects a fraction; flat stats are used as-is.
     */
    public static float getAttributeModifierValue(CardStat stat, float totalStatValue) {
        float value = getEffectiveValue(stat, totalStatValue);
        return isFlat(stat) ? value : value / 100.0f;
    }

    /**
     * Formats the effective bonus for display, taking the application mode into account:
     * percentage stats are rendered as {@code +5.0%} while flat stats are rendered as {@code +5.0}.
     */
    public static String formatValue(CardStat stat, float totalStatValue) {
        float value = getEffectiveValue(stat, totalStatValue);
        String sign = value >= 0 ? "+" : "";
        return isFlat(stat)
                ? String.format(java.util.Locale.ROOT, "%s%.1f", sign, value)
                : String.format(java.util.Locale.ROOT, "%s%.1f%%", sign, value);
    }

    /** Same as {@link #formatValue(CardStat, float)} but as a {@link Component}. */
    public static MutableComponent formatValueComponent(CardStat stat, float totalStatValue) {
        return Component.literal(formatValue(stat, totalStatValue));
    }

    public static float getPlayerDropBonus(ServerPlayer player) {
        float totalBonus = 0f;
        
        List<EquippedAccessory> accessories = PlatformHelper.INSTANCE.getEquippedAccessories(player);
        com.howlite.cobblemoncards.CobblemonCards.LOGGER.info("[CobblemonCards] Checking player drop bonus for " + player.getName().getString() + ". Accessories found: " + accessories.size());
        
        // Check Platform Accessories (Binders equipped in accessory slots)
        for (EquippedAccessory equipped : accessories) {
            float bonus = getBonusFromStack(equipped.stack());
            com.howlite.cobblemoncards.CobblemonCards.LOGGER.info("[CobblemonCards] Equipped accessory in slot '" + equipped.slotName() + "' stack: " + equipped.stack() + ", bonus: " + bonus);
            totalBonus += bonus;
        }
        
        // Also check main hand and offhand just in case
        float mainHandBonus = getBonusFromStack(player.getItemInHand(InteractionHand.MAIN_HAND));
        if (mainHandBonus > 0f) {
            com.howlite.cobblemoncards.CobblemonCards.LOGGER.info("[CobblemonCards] Main hand has binder drop bonus: " + mainHandBonus);
            totalBonus += mainHandBonus;
        }
        
        float offHandBonus = getBonusFromStack(player.getItemInHand(InteractionHand.OFF_HAND));
        if (offHandBonus > 0f) {
            com.howlite.cobblemoncards.CobblemonCards.LOGGER.info("[CobblemonCards] Off hand has binder drop bonus: " + offHandBonus);
            totalBonus += offHandBonus;
        }
        
        // Apply the stat multiplier from config (exactly once, via the shared helper)
        float finalBonus = getEffectiveValue(CardStat.CARD_DROP_CHANCE, totalBonus);
        com.howlite.cobblemoncards.CobblemonCards.LOGGER.info("[CobblemonCards] Final calculated drop bonus: " + finalBonus);
        return finalBonus;
    }
    
    private static float getBonusFromStack(ItemStack stack) {
        return collectStats(stack, s -> s == CardStat.CARD_DROP_CHANCE)
                .getOrDefault(CardStat.CARD_DROP_CHANCE, 0f);
    }
}
