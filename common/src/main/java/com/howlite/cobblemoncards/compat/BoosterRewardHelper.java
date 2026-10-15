package com.howlite.cobblemoncards.compat;

import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.item.custom.BoosterPackItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** One server-owned roll, using only already registered booster items. */
public final class BoosterRewardHelper {
    private BoosterRewardHelper() {}

    public record Selection(List<String> boosterTypes) {
        public static final Selection CLASSIC = new Selection(List.of("classic"));
        public static final Selection GENERATION = new Selection(
                IntStream.rangeClosed(1, 9).mapToObj(i -> "gen" + i).toList());
        public static final Selection THEMED = new Selection(List.of("bug", "dark", "dragon",
                "electric", "fairy", "fighting", "fire", "flying", "ghost", "grass", "ground",
                "ice", "normal", "poison", "psychic", "rock", "steel", "water"));
        public static final Selection CLASSIC_OR_GENERATION = combine(CLASSIC, GENERATION);
        public static final Selection GENERATION_OR_THEMED = combine(GENERATION, THEMED);

        public Selection { boosterTypes = List.copyOf(boosterTypes); }

        public static Selection fromToken(String token) {
            if (token == null) return CLASSIC;
            String type = token.toLowerCase(Locale.ROOT);
            return switch (type) {
                case "classic", "base" -> CLASSIC;
                case "generation" -> GENERATION;
                case "themed" -> THEMED;
                default -> GENERATION.boosterTypes.contains(type) || THEMED.boosterTypes.contains(type)
                        ? new Selection(List.of(type)) : CLASSIC;
            };
        }

        private static Selection combine(Selection first, Selection second) {
            List<String> types = new ArrayList<>(first.boosterTypes);
            types.addAll(second.boosterTypes);
            return new Selection(types);
        }
    }

    public static ItemStack roll(CompatReward reward, RandomSource random) {
        return reward.isEnabled() ? roll(reward.chance(), reward.selection(), random) : ItemStack.EMPTY;
    }

    public static ItemStack roll(double percentage, Selection selection, RandomSource random) {
        double chance = RewardProbability.clamp(percentage);
        if (chance <= 0.0 || !RewardProbability.succeeds(chance, random.nextDouble())) return ItemStack.EMPTY;
        List<Item> items = available(selection, type -> BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(CobblemonCards.MOD_ID,
                        type.equals("classic") ? "booster_pack" : "booster_pack_" + type)),
                item -> item instanceof BoosterPackItem);
        return items.isEmpty() ? ItemStack.EMPTY : new ItemStack(items.get(random.nextInt(items.size())));
    }

    // Kept generic so sparse/invalid registries can be tested without bootstrapping Minecraft.
    static <T> List<T> available(Selection selection, Function<String, T> lookup, Predicate<T> isBooster) {
        LinkedHashSet<T> items = new LinkedHashSet<>();
        for (String type : selection.boosterTypes) {
            T item = lookup.apply(type);
            if (item != null && isBooster.test(item)) items.add(item);
        }
        return List.copyOf(items);
    }

    public static void give(ServerPlayer player, ItemStack reward) {
        if (reward.isEmpty() || player.isRemoved() || !player.isAlive()) return;
        if (player.getServer() == null || !player.getServer().isSameThread()) {
            throw new IllegalStateException("Optional booster rewards must be delivered on the server thread");
        }
        if (!player.getInventory().add(reward)) {
            ItemEntity dropped = player.drop(reward, false);
            if (dropped != null) {
                dropped.setNoPickUpDelay();
                dropped.setTarget(player.getUUID());
            }
        }
    }
}
