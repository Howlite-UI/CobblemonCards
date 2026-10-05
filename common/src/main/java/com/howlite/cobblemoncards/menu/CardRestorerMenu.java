package com.howlite.cobblemoncards.menu;

import com.howlite.cobblemoncards.block.entity.CardRestorerBlockEntity;
import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CardRestorerMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data;
    private CardRestorerBlockEntity blockEntity;

    // Index des slots :
    // 0        : slot de carte (input, au centre du cadre)
    // 1-4      : slots de card dust input (grille 2x2 à droite)
    // 5-31     : inventaire joueur principal (3x9)
    // 32-40    : hotbar joueur (1x9)

    public CardRestorerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(5), new SimpleContainerData(CardRestorerBlockEntity.DATA_COUNT));
    }

    public CardRestorerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.CARD_RESTORER_MENU, containerId);
        checkContainerSize(container, 5);
        checkContainerDataCount(data, CardRestorerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;

        if (container instanceof CardRestorerBlockEntity be) {
            this.blockEntity = be;
        }

        // Slot 0: Carte insérée (Centrée dans le cadre X=108, Y=41)
        this.addSlot(new CardSlot(container, 0, 108, 41));

        // Slots 1-4: Card Dust Input (Grille 2x2 alignée sur les 4 cases à droite X=188/206, Y=51/69 ajusté +7px X)
        this.addSlot(new DustSlot(container, 1, 188, 51));
        this.addSlot(new DustSlot(container, 2, 206, 51));
        this.addSlot(new DustSlot(container, 3, 188, 69));
        this.addSlot(new DustSlot(container, 4, 206, 69));

        addDataSlots(data);

        // Inventaire joueur (3 rangées × 9 colonnes), remonté de 2px (Y=120)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 36 + j * 18, 120 + i * 18));
            }
        }

        // Hotbar joueur (Y=178)
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 36 + i * 18, 178));
        }
    }

    private static class CardSlot extends Slot {
        public CardSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.has(ModDataComponents.CARD_DATA);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class DustSlot extends Slot {
        public DustSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModItems.CARD_DUST) || stack.is(ModItems.CARD_DUST_POUCH) || stack.is(ModBlocks.CARD_DUST_SACK.asItem());
        }
    }

    // -------------------------------------------------------------------------
    // Data accessors
    // -------------------------------------------------------------------------

    public int getCurrentCardGrade() {
        return this.data.get(0);
    }

    public int getTargetGrade() {
        return this.data.get(1);
    }

    public int getDustCost() {
        return getSyncedInt(2, 11);
    }

    public int getStoredDust() {
        return this.data.get(3);
    }

    public int getMaxStoredDust() {
        int max = this.data.get(4);
        return max > 0 ? max : 10000;
    }

    private int getSyncedInt(int lowIndex, int highIndex) {
        return (this.data.get(lowIndex) & 0xFFFF) | ((this.data.get(highIndex) & 0xFFFF) << 16);
    }

    public int getRestoreProgress() {
        return getSyncedInt(5, 6);
    }

    public int getRestoreDuration() {
        return getSyncedInt(7, 8);
    }

    public int getExpectedRestoreDuration() {
        return getSyncedInt(9, 10);
    }

    public int getRemainingRestoreTicks() {
        return Math.max(0, getRestoreDuration() - getRestoreProgress());
    }

    public boolean isRestoring() {
        return getRestoreDuration() > 0;
    }

    public float getRestoreProgressFraction() {
        int duration = getRestoreDuration();
        return duration > 0 ? Math.clamp((float) getRestoreProgress() / duration, 0.0f, 1.0f) : 0.0f;
    }

    public boolean canRestore() {
        return !isRestoring() && hasEnoughDustForRestore();
    }

    public boolean hasEnoughDustForRestore() {
        int currentGrade = getCurrentCardGrade();
        int target = getTargetGrade();
        int cost = getDustCost();
        int available = getStoredDust();

        for (int i = 1; i <= 4; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.is(ModItems.CARD_DUST)) available += stack.getCount();
            else if (stack.is(ModItems.CARD_DUST_POUCH)) available += stack.getCount() * 64;
            else if (stack.is(ModBlocks.CARD_DUST_SACK.asItem())) available += stack.getCount() * 576;
        }

        return currentGrade > 0 && target > currentGrade && target <= 10
                && cost > 0 && available >= cost;
    }

    // -------------------------------------------------------------------------
    // Quick move (Shift-click)
    // -------------------------------------------------------------------------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 5) {
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (itemstack1.has(ModDataComponents.CARD_DATA)) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemstack1.is(ModItems.CARD_DUST) || itemstack1.is(ModItems.CARD_DUST_POUCH) || itemstack1.is(ModBlocks.CARD_DUST_SACK.asItem())) {
                    if (!this.moveItemStackTo(itemstack1, 1, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (index < 32) {
                        if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public CardRestorerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public Container getContainer() {
        return container;
    }
}
