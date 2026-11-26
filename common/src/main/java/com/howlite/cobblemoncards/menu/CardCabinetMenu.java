package com.howlite.cobblemoncards.menu;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.custom.CardItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CardCabinetMenu extends AbstractContainerMenu {
    public static final int SLOTS_PER_PAGE = 12; // 4x3
    public static final int MAX_PAGES = 1000;

    private final Container cabinetContainer;
    private int currentPage = 0;

    public CardCabinetMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(12000));
    }

    public CardCabinetMenu(int containerId, Inventory playerInventory, Container cabinetContainer) {
        super(ModMenuTypes.CARD_CABINET_MENU, containerId);
        this.cabinetContainer = cabinetContainer;

        // Only register the 12 visible slots for the active page (48 slots total with player inventory)
        // instead of 12,000 slots — eliminates severe FPS drops and server TPS drops in broadcastChanges().
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 4; ++col) {
                int localIndex = col + row * 4;
                int xPos = 46 + col * 50;
                int yPos = 35 + row * 65;
                this.addSlot(new PagedCardSlot(this.cabinetContainer, localIndex, xPos, yPos, this));
            }
        }

        // Inventaire du joueur
        int invY = 229;
        int invX = 49;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
        }
    }

    public Container getContainer() {
        return this.cabinetContainer;
    }

    public void setPage(int page) {
        if (page >= 0 && page < MAX_PAGES) {
            this.currentPage = page;
            this.broadcastChanges();
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
        int totalSlots = MAX_PAGES * SLOTS_PER_PAGE;
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = this.cabinetContainer.getItem(i);
            if (!stack.isEmpty()) {
                cards.add(stack);
                this.cabinetContainer.setItem(i, ItemStack.EMPTY);
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
                case 4 -> {
                    boolean c1 = com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(d1.pokemonId()) || d1.stat() == null;
                    boolean c2 = com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(d2.pokemonId()) || d2.stat() == null;
                    if (c1 && c2) yield d1.pokemonId().compareTo(d2.pokemonId());
                    if (c1) yield 1;
                    if (c2) yield -1;
                    int cmp = d1.stat().getSerializedName().compareTo(d2.stat().getSerializedName());
                    if (cmp != 0) yield cmp;
                    yield Float.compare(d2.statValue(), d1.statValue());
                }
                default -> 0;
            };
        };

        cards.sort(comparator);

        for (int i = 0; i < cards.size() && i < totalSlots; i++) {
            this.cabinetContainer.setItem(i, cards.get(i));
        }
        this.broadcastChanges();
    }

    public int getCurrentPage() { return currentPage; }
    public int getMaxPages() { return MAX_PAGES; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            newStack = originalStack.copy();

            if (index < SLOTS_PER_PAGE) {
                if (!this.moveItemStackTo(originalStack, SLOTS_PER_PAGE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (originalStack.getItem() instanceof CardItem) {
                    // Try current page first, then fall back to any empty slot in the cabinet.
                    boolean moved = this.moveItemStackTo(originalStack, 0, SLOTS_PER_PAGE, false);
                    if (!originalStack.isEmpty()) {
                        int totalCabinetSlots = MAX_PAGES * SLOTS_PER_PAGE;
                        for (int i = 0; i < totalCabinetSlots && !originalStack.isEmpty(); i++) {
                            if (this.cabinetContainer.getItem(i).isEmpty()) {
                                int maxCount = Math.min(this.cabinetContainer.getMaxStackSize(), originalStack.getMaxStackSize());
                                this.cabinetContainer.setItem(i, originalStack.split(Math.min(originalStack.getCount(), maxCount)));
                                moved = true;
                            }
                        }
                        if (moved) {
                            this.cabinetContainer.setChanged();
                        }
                    }
                    if (!moved) {
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
        return this.cabinetContainer.stillValid(player);
    }

    private static class PagedCardSlot extends Slot {
        private final int localIndex;
        private final CardCabinetMenu menu;

        public PagedCardSlot(Container container, int localIndex, int x, int y, CardCabinetMenu menu) {
            super(container, localIndex, x, y);
            this.localIndex = localIndex;
            this.menu = menu;
        }

        private int getBackingIndex() {
            return this.menu.getCurrentPage() * SLOTS_PER_PAGE + this.localIndex;
        }

        @Override
        public ItemStack getItem() {
            int idx = getBackingIndex();
            return idx < this.container.getContainerSize() ? this.container.getItem(idx) : ItemStack.EMPTY;
        }

        @Override
        public void set(ItemStack stack) {
            int idx = getBackingIndex();
            if (idx < this.container.getContainerSize()) {
                this.container.setItem(idx, stack);
                this.setChanged();
            }
        }

        @Override
        public ItemStack remove(int amount) {
            int idx = getBackingIndex();
            return idx < this.container.getContainerSize() ? this.container.removeItem(idx, amount) : ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof CardItem;
        }
    }
}
