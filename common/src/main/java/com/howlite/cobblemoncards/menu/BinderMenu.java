package com.howlite.cobblemoncards.menu;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.custom.CardItem;
import com.howlite.cobblemoncards.item.custom.BinderItem;
import com.howlite.cobblemoncards.item.custom.BinderTier;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.howlite.cobblemoncards.util.BinderLocator;

public class BinderMenu extends AbstractContainerMenu {
    public static final int SLOTS_PER_PAGE = 12; // 4x3
    private final ItemStack binderStack;
    private final Container binderContainer;
    private int currentPage = 0;
    private final BinderTier tier;

    public BinderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ItemStack.EMPTY);
    }

    public BinderMenu(int containerId, Inventory playerInventory, BinderLocator locator) {
        this(containerId, playerInventory, locator != null ? locator.findItem(playerInventory.player) : ItemStack.EMPTY);
    }

    public static BinderLocator findActiveBinderLocator(Player player) {
        // 1. Check accessories
        var equipped = PlatformHelper.INSTANCE.getEquippedAccessories(player);
        for (var acc : equipped) {
            if (acc.stack().getItem() instanceof BinderItem) {
                return BinderLocator.accessory(acc.slotName(), acc.index());
            }
        }
        // 2. Check main hand
        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (mainHand.getItem() instanceof BinderItem) {
            return BinderLocator.hand(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        // 3. Check off hand
        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (offHand.getItem() instanceof BinderItem) {
            return BinderLocator.hand(net.minecraft.world.InteractionHand.OFF_HAND);
        }
        // 4. Check entire inventory
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() instanceof BinderItem) {
                return BinderLocator.inventory(i);
            }
        }
        return null;
    }

    public static ItemStack findActiveBinder(Player player) {
        // 1. Check accessories
        for (var acc : PlatformHelper.INSTANCE.getEquippedAccessories(player)) {
            if (acc.stack().getItem() instanceof BinderItem) {
                return acc.stack();
            }
        }
        // 2. Check main hand
        ItemStack mainHand = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (mainHand.getItem() instanceof BinderItem) {
            return mainHand;
        }
        // 3. Check off hand
        ItemStack offHand = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (offHand.getItem() instanceof BinderItem) {
            return offHand;
        }
        // 4. Check entire inventory
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BinderItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public BinderMenu(int containerId, Inventory playerInventory, ItemStack binderStack) {
        super(ModMenuTypes.BINDER_MENU, containerId);
        this.binderStack = binderStack;
        this.tier = (binderStack.getItem() instanceof BinderItem binder) ? binder.getTier() : BinderTier.LEATHER;
        
        int totalSlots = tier.getMaxSlots(SLOTS_PER_PAGE);
        this.binderContainer = new SimpleContainer(totalSlots);

        // Read from BINDER_CONTENTS (new, unlimited). Fall back to vanilla CONTAINER for
        // backward-compatibility with saves created before the 256-item fix.
        List<ItemStack> savedItems = binderStack.get(ModDataComponents.BINDER_CONTENTS);
        if (savedItems == null) {
            // Migration path: old binders stored data in DataComponents.CONTAINER (max 256).
            NonNullList<ItemStack> legacy = NonNullList.withSize(Math.min(totalSlots, 256), ItemStack.EMPTY);
            binderStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(legacy);
            savedItems = legacy;
        }
        for (int i = 0; i < Math.min(savedItems.size(), totalSlots); i++) {
            this.binderContainer.setItem(i, savedItems.get(i));
        }

        for (int p = 0; p < tier.getPages(); p++) {
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 4; ++col) {
                    int index = col + row * 4 + p * SLOTS_PER_PAGE;
                    int xPos = 46 + col * 50;
                    int yPos = 35 + row * 65;
                    this.addSlot(new CardSlot(this.binderContainer, index, xPos, yPos, p, this));
                }
            }
        }

        // Inventaire du joueur
        int invY = 229;
        int invX = 49;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new LockedSlot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new LockedSlot(playerInventory, col, invX + col * 18, invY + 58));
        }
    }

    public void setPage(int page) {
        if (page >= 0 && page < tier.getPages()) {
            this.currentPage = page;
        }
    }
    
    private int getRarityWeight(String rarity) {
        if (rarity == null) return 0;
        return switch (rarity.toLowerCase()) {
            case "common" -> 1;
            case "uncommon" -> 2;
            case "rare" -> 3;
            case "epic" -> 4;
            case "legendary" -> 5;
            case "mythic" -> 6;
            default -> 0;
        };
    }
    
    public void sort(int sortMode) {
        List<ItemStack> cards = new ArrayList<>();
        int totalSlots = tier.getMaxSlots(SLOTS_PER_PAGE);
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = this.binderContainer.getItem(i);
            if (!stack.isEmpty()) {
                cards.add(stack);
                this.binderContainer.setItem(i, ItemStack.EMPTY);
            }
        }
        
        Comparator<ItemStack> comparator = (s1, s2) -> {
            CardData d1 = s1.get(ModDataComponents.CARD_DATA);
            CardData d2 = s2.get(ModDataComponents.CARD_DATA);
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            
            return switch (sortMode) {
                case 1 -> d1.pokemonId().compareTo(d2.pokemonId());
                case 2 -> Integer.compare(getRarityWeight(d2.rarity()), getRarityWeight(d1.rarity()));
                case 3 -> Integer.compare(d2.grade(), d1.grade());
                default -> 0;
            };
        };
        
        cards.sort(comparator);
        
        for (int i = 0; i < cards.size() && i < totalSlots; i++) {
            this.binderContainer.setItem(i, cards.get(i));
        }
    }

    public int getCurrentPage() { return currentPage; }
    public int getMaxPages() { return tier.getPages(); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            int lastNonEmpty = -1;
            for (int i = binderContainer.getContainerSize() - 1; i >= 0; i--) {
                if (!this.binderContainer.getItem(i).isEmpty()) {
                    lastNonEmpty = i;
                    break;
                }
            }
            List<ItemStack> items = new ArrayList<>(lastNonEmpty + 1);
            for (int i = 0; i <= lastNonEmpty; i++) {
                items.add(this.binderContainer.getItem(i));
            }
            // Save to the unlimited BINDER_CONTENTS component.
            binderStack.set(ModDataComponents.BINDER_CONTENTS, Collections.unmodifiableList(items));
            // Clear the old vanilla CONTAINER key to avoid confusion on legacy reads.
            binderStack.remove(DataComponents.CONTAINER);
            PlatformHelper.INSTANCE.refreshEquippedModifiers(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int totalBinderSlots = tier.getMaxSlots(SLOTS_PER_PAGE);
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (index < totalBinderSlots) {
                if (!this.moveItemStackTo(originalStack, totalBinderSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof CardItem) {
                    if (!this.moveItemStackTo(originalStack, 0, totalBinderSlots, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !binderStack.isEmpty() && binderStack.getItem() instanceof BinderItem;
    }

    private static class CardSlot extends Slot {
        private final int page;
        private final BinderMenu menu;

        public CardSlot(Container container, int index, int x, int y, int page, BinderMenu menu) {
            super(container, index, x, y);
            this.page = page;
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CardItem;
        }

        @Override
        public boolean isActive() {
            return this.menu.getCurrentPage() == this.page;
        }
    }

    private class LockedSlot extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }
        @Override
        public boolean mayPickup(Player player) {
            return !this.getItem().equals(binderStack);
        }
        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.equals(binderStack);
        }
    }
}
