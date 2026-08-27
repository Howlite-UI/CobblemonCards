package com.howlite.cobblemoncards.block.entity;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.menu.CardRestorerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CardRestorerBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider {

    private static final java.util.Random RANDOM = new java.util.Random();

    // Slots: 0 = card input, 1-4 = card dust input
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);

    // Grade cible sélectionné par le joueur (stocké côté serveur)
    private int targetGrade = 0;

    // Réservoir interne de Card Dust (jusqu'à 10,000 dusts)
    private int storedDust = 0;
    public static final int MAX_STORED_DUST = 10000;

    /**
     * ContainerData synchronisé avec le client :
     * [0] = grade actuel de la carte (0 si slot vide)
     * [1] = grade cible sélectionné
     * [2] = coût en card dust
     * [3] = card dust stockée dans le réservoir interne
     * [4] = capacité maximale du réservoir (10000)
     */
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getCurrentCardGrade();
                case 1 -> CardRestorerBlockEntity.this.targetGrade;
                case 2 -> calculateDustCost();
                case 3 -> CardRestorerBlockEntity.this.storedDust;
                case 4 -> MAX_STORED_DUST;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) {
                CardRestorerBlockEntity.this.targetGrade = value;
            } else if (index == 3) {
                CardRestorerBlockEntity.this.storedDust = value;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CardRestorerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARD_RESTORER_BE, pos, state);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    // -------------------------------------------------------------------------
    // Server Ticking - Absorption automatique de la Dust dans le réservoir
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CardRestorerBlockEntity blockEntity) {
        if (level == null || level.isClientSide) return;

        boolean changed = false;

        // Absorber la dust depuis les slots 1 à 4 vers le réservoir interne
        if (blockEntity.storedDust < MAX_STORED_DUST) {
            for (int i = 1; i <= 4; i++) {
                ItemStack stack = blockEntity.getItem(i);
                if (stack.isEmpty()) continue;

                int spaceLeft = MAX_STORED_DUST - blockEntity.storedDust;
                if (spaceLeft <= 0) break;

                if (stack.is(ModItems.CARD_DUST)) {
                    int toAbsorb = Math.min(stack.getCount(), spaceLeft);
                    blockEntity.storedDust += toAbsorb;
                    stack.shrink(toAbsorb);
                    changed = true;
                } else if (stack.is(ModItems.CARD_DUST_POUCH) && spaceLeft >= 64) {
                    blockEntity.storedDust += 64;
                    stack.shrink(1);
                    changed = true;
                } else if (stack.is(ModBlocks.CARD_DUST_SACK.asItem()) && spaceLeft >= 576) {
                    blockEntity.storedDust += 576;
                    stack.shrink(1);
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.setChanged();
            blockEntity.sync();
        }
    }

    // -------------------------------------------------------------------------
    // Grade & Cost Logic
    // -------------------------------------------------------------------------

    public int getCurrentCardGrade() {
        ItemStack cardSlot = getItem(0);
        if (cardSlot.isEmpty()) return 0;
        CardData data = cardSlot.get(ModDataComponents.CARD_DATA);
        if (data == null) return 0;
        return data.grade();
    }

    public int calculateDustCost() {
        int currentGrade = getCurrentCardGrade();
        int target = targetGrade;
        if (target <= currentGrade || target > 10 || currentGrade <= 0) return 0;

        int diff = target - currentGrade;
        int baseCost = target * CobblemonCardsConfig.restorerBaseCost;
        long cost = (long) baseCost * (1L << (diff - 1));
        return (int) Math.min(cost, Integer.MAX_VALUE);
    }

    public int getStoredDust() {
        return storedDust;
    }

    public int getTotalDustAvailable() {
        int total = storedDust;
        for (int i = 1; i <= 4; i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.CARD_DUST)) {
                    total += stack.getCount();
                } else if (stack.is(ModItems.CARD_DUST_POUCH)) {
                    total += stack.getCount() * 64;
                } else if (stack.is(ModBlocks.CARD_DUST_SACK.asItem())) {
                    total += stack.getCount() * 576;
                }
            }
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // Restore Logic
    // -------------------------------------------------------------------------

    public boolean performRestore(Player player) {
        if (level == null || level.isClientSide) return false;

        ItemStack cardStack = getItem(0);
        if (cardStack.isEmpty()) return false;

        CardData data = cardStack.get(ModDataComponents.CARD_DATA);
        if (data == null) return false;

        int currentGrade = data.grade();
        if (currentGrade <= 0) return false;

        int target = targetGrade;
        if (target <= currentGrade || target > 10) return false;

        int dustCost = calculateDustCost();
        if (dustCost <= 0) return false;

        int dustAvailable = getTotalDustAvailable();
        if (dustAvailable < dustCost) return false;

        // Déduire le coût en priorité du réservoir
        int remainingCost = dustCost;
        if (storedDust >= remainingCost) {
            storedDust -= remainingCost;
            remainingCost = 0;
        } else {
            remainingCost -= storedDust;
            storedDust = 0;
        }

        // Si besoin, consommer le reste dans les slots 1 à 4
        if (remainingCost > 0) {
            consumeDustFromSlots(remainingCost);
        }

        // Upgrader la carte (recalcul du bonus stat)
        float currentBonus = currentGrade * 0.03f;
        float targetBonus = target * 0.03f;
        float baseStatValue = currentGrade > 0
                ? data.statValue() / (1f + currentBonus)
                : data.statValue();
        float newStatValue = baseStatValue * (1f + targetBonus);

        // Restoring can lift a card to grade 9+, which is the main way to earn a "trainer" stat.
        // Without this, a card restored to grade 10 could never obtain one (the Grading Station only
        // ever runs on grade-0 cards). Gated on newly CROSSING the threshold, so repeatedly restoring
        // an already-9 card to 10 isn't a cheap re-roll.
        CardStat finalStat = data.stat();
        if (currentGrade < CobblemonCardsConfig.trainerStatMinGrade
                && target >= CobblemonCardsConfig.trainerStatMinGrade
                && !com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId())) {
            CardStat trainerStat = com.howlite.cobblemoncards.util.CardStatUtil
                    .rollTrainerStatForGrade(target, RANDOM);
            if (trainerStat != null) {
                finalStat = trainerStat;
            }
        }

        CardData newData = new CardData(
                data.pokemonId(),
                data.isShiny(),
                data.rarity(),
                finalStat,
                newStatValue,
                target,
                data.background(),
                data.effect()
        );
        cardStack.set(ModDataComponents.CARD_DATA, newData);

        targetGrade = 0;

        if (level != null) {
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
        }

        setChanged();
        sync();
        return true;
    }

    private void consumeDustFromSlots(int amount) {
        int remaining = amount;
        for (int i = 1; i <= 4 && remaining > 0; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.CARD_DUST)) {
                int count = stack.getCount();
                if (count >= remaining) {
                    stack.shrink(remaining);
                    remaining = 0;
                } else {
                    remaining -= count;
                    setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Target grade management
    // -------------------------------------------------------------------------

    public int getTargetGrade() {
        return targetGrade;
    }

    public void setTargetGrade(int grade) {
        this.targetGrade = grade;
        setChanged();
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) {
            return stack.has(ModDataComponents.CARD_DATA);
        } else if (slot >= 1 && slot <= 4) {
            return stack.is(ModItems.CARD_DUST) || stack.is(ModItems.CARD_DUST_POUCH) || stack.is(ModBlocks.CARD_DUST_SACK.asItem());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // NBT Save / Load
    // -------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.targetGrade = tag.getInt("TargetGrade");
        this.storedDust = tag.getInt("StoredDust");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("TargetGrade", this.targetGrade);
        tag.putInt("StoredDust", this.storedDust);
    }

    // -------------------------------------------------------------------------
    // Network sync
    // -------------------------------------------------------------------------

    public void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    // -------------------------------------------------------------------------
    // MenuProvider
    // -------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cobblemon-cards.card_restorer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CardRestorerMenu(containerId, playerInventory, this, this.dataAccess);
    }
}
