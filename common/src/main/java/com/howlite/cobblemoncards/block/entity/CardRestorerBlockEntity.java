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

    // Grade cible sÃ©lectionnÃ© par le joueur (stockÃ© cÃ´tÃ© serveur)
    private int targetGrade = 0;

    // Server-owned process, saved with the machine rather than with its open GUI.
    private int restoreProgress = 0;
    private int restoreDuration = 0;
    private int restoreDustCost = 0;
    private ItemStack restoreInput = ItemStack.EMPTY;
    public static final int DATA_COUNT = 12;

    // RÃ©servoir interne de Card Dust (jusqu'Ã  10,000 dusts)
    private int storedDust = 0;
    public static final int MAX_STORED_DUST = 10000;

    /**
     * Menu data: grade, target, cost low bits, stored dust, capacity,
     * progress low/high, duration low/high, expected duration low/high, cost high bits.
     * Vanilla menu data packets carry signed shorts, so large values use two slots.
     */
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getCurrentCardGrade();
                case 1 -> CardRestorerBlockEntity.this.targetGrade;
                case 2 -> calculateDustCost() & 0xFFFF;
                case 3 -> CardRestorerBlockEntity.this.storedDust;
                case 4 -> MAX_STORED_DUST;
                case 5 -> restoreProgress & 0xFFFF;
                case 6 -> restoreProgress >>> 16;
                case 7 -> restoreDuration & 0xFFFF;
                case 8 -> restoreDuration >>> 16;
                case 9 -> getExpectedRestoreDuration() & 0xFFFF;
                case 10 -> getExpectedRestoreDuration() >>> 16;
                case 11 -> calculateDustCost() >>> 16;
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
            return DATA_COUNT;
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
    // Server Ticking - Absorption automatique de la Dust dans le rÃ©servoir
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, CardRestorerBlockEntity blockEntity) {
        if (level == null || level.isClientSide) return;

        boolean changed = false;

        // Absorber la dust depuis les slots 1 Ã  4 vers le rÃ©servoir interne
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

        // This runs even when nobody has the menu open, just like a furnace.
        if (blockEntity.isRestoring()) {
            if (!blockEntity.hasRestoreInput()) {
                blockEntity.resetRestore();
                changed = true;
            } else if (blockEntity.getTotalDustAvailable() >= blockEntity.restoreDustCost) {
                blockEntity.restoreProgress++;
                blockEntity.setChanged();
                if (blockEntity.restoreProgress >= blockEntity.restoreDuration) {
                    blockEntity.finishRestore();
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.updateBlockState();
            blockEntity.setChanged();
            blockEntity.sync();
        }
    }

    public void updateBlockState() {
        if (this.level != null && !this.level.isClientSide) {
            BlockState currentState = getBlockState();
            if (currentState.getBlock() instanceof com.howlite.cobblemoncards.block.CardRestorerBlock) {
                int expectedLevel = com.howlite.cobblemoncards.block.CardRestorerBlock.getDustLevel(this.storedDust, MAX_STORED_DUST);
                if (currentState.getValue(com.howlite.cobblemoncards.block.CardRestorerBlock.DUST_LEVEL) != expectedLevel) {
                    this.level.setBlock(this.worldPosition, currentState.setValue(com.howlite.cobblemoncards.block.CardRestorerBlock.DUST_LEVEL, expectedLevel), 3);
                }
            }
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
        if (isRestoring()) return restoreDustCost;
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

    public boolean isRestoring() {
        return restoreDuration > 0;
    }

    public int getExpectedRestoreDuration() {
        if (isRestoring()) return restoreDuration;
        int currentGrade = getCurrentCardGrade();
        return currentGrade > 0 && targetGrade > currentGrade
                ? CobblemonCardsConfig.getRestorerProcessTime(targetGrade) : 0;
    }

    public boolean startRestore(Player player) {
        if (level == null || level.isClientSide || isRestoring() || !stillValid(player)) return false;

        ItemStack cardStack = getItem(0);
        CardData data = cardStack.get(ModDataComponents.CARD_DATA);
        if (data == null || cardStack.getCount() != 1 || data.grade() <= 0
                || targetGrade <= data.grade() || targetGrade > 10) return false;

        int dustCost = calculateDustCost();
        if (dustCost <= 0 || getTotalDustAvailable() < dustCost) return false;

        restoreInput = cardStack.copy();
        restoreDustCost = dustCost;
        restoreDuration = getExpectedRestoreDuration();
        restoreProgress = 0;
        setChanged();
        sync();
        return true;
    }

    private boolean hasRestoreInput() {
        return !restoreInput.isEmpty() && ItemStack.matches(restoreInput, getItem(0));
    }

    private void resetRestore() {
        restoreProgress = 0;
        restoreDuration = 0;
        restoreDustCost = 0;
        restoreInput = ItemStack.EMPTY;
    }

    private void finishRestore() {
        ItemStack cardStack = getItem(0);
        CardData data = cardStack.get(ModDataComponents.CARD_DATA);
        if (data == null || !hasRestoreInput()) {
            resetRestore();
            return;
        }
        int currentGrade = data.grade();
        int target = targetGrade;

        // Charge only at completion; removing the input cancels without spending dust.
        consumeDust(restoreDustCost);

        // Upgrader la carte (recalcul du bonus stat)
        float currentBonus = currentGrade * 0.03f;
        float targetBonus = target * 0.03f;
        float baseStatValue = currentGrade > 0
                ? data.statValue() / (1f + currentBonus)
                : data.statValue();
        float newStatValue = baseStatValue * (1f + targetBonus);

        // La stat de base de la carte n'est JAMAIS Ã©crasÃ©e !
        // Si la carte atteint le grade 9+, elle peut dÃ©bloquer une stat Trainer SUPPLÃ‰MENTAIRE.
        java.util.Optional<CardStat> finalTrainerStat = data.trainerStat();
        java.util.Optional<Float> finalTrainerStatValue = data.trainerStatValue();

        if (finalTrainerStat.isPresent() && finalTrainerStatValue.isPresent()) {
            float baseTrainer = currentGrade > 0
                    ? finalTrainerStatValue.get() / (1f + currentBonus)
                    : finalTrainerStatValue.get();
            finalTrainerStatValue = java.util.Optional.of(baseTrainer * (1f + targetBonus));
        } else if (currentGrade < CobblemonCardsConfig.trainerStatMinGrade
                && target >= CobblemonCardsConfig.trainerStatMinGrade
                && !com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId())) {
            CardStat trainerStat = com.howlite.cobblemoncards.util.CardStatUtil
                    .rollTrainerStatForGrade(target, RANDOM);
            if (trainerStat != null) {
                finalTrainerStat = java.util.Optional.of(trainerStat);
                finalTrainerStatValue = java.util.Optional.of(newStatValue * CobblemonCardsConfig.trainerStatLuckyValueMultiplier);
            }
        }

        CardData newData = new CardData(
                data.pokemonId(),
                data.isShiny(),
                data.rarity(),
                data.stat(),
                newStatValue,
                target,
                data.background(),
                data.effect(),
                finalTrainerStat,
                finalTrainerStatValue
        );
        cardStack.set(ModDataComponents.CARD_DATA, newData);

        targetGrade = 0;
        resetRestore();

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                    35, 0.3, 0.4, 0.3, 0.8);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                    20, 0.2, 0.2, 0.2, 0.1);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                    12, 0.15, 0.2, 0.15, 0.05);
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
            level.playSound(null, worldPosition,
                    net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
        }

    }

    private void consumeDust(int amount) {
        int fromReservoir = Math.min(storedDust, amount);
        storedDust -= fromReservoir;
        int remaining = amount - fromReservoir;
        for (int i = 1; i <= 4 && remaining > 0; i++) {
            ItemStack stack = getItem(i);
            int unitValue = stack.is(ModItems.CARD_DUST) ? 1
                    : stack.is(ModItems.CARD_DUST_POUCH) ? 64
                    : stack.is(ModBlocks.CARD_DUST_SACK.asItem()) ? 576 : 0;
            if (stack.isEmpty() || unitValue == 0) continue;

            int units = Math.min(stack.getCount(), (remaining + unitValue - 1) / unitValue);
            int dust = units * unitValue;
            stack.shrink(units);
            if (dust > remaining) {
                storedDust += dust - remaining; // Keep change from a pouch or sack.
                remaining = 0;
            } else {
                remaining -= dust;
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
        if (level == null || level.isClientSide || isRestoring() || grade < 0 || grade > 10) return;
        this.targetGrade = grade;
        setChanged();
        sync();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ImplementedInventory.super.setItem(slot, stack);
        if (slot == 0) checkRestoreInput();
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ImplementedInventory.super.removeItem(slot, count);
        if (slot == 0) checkRestoreInput();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ImplementedInventory.super.removeItemNoUpdate(slot);
        if (slot == 0) checkRestoreInput();
        return removed;
    }

    private void checkRestoreInput() {
        if (level != null && !level.isClientSide && isRestoring() && !hasRestoreInput()) {
            resetRestore();
            setChanged();
            sync();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
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
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.targetGrade = tag.getInt("TargetGrade");
        this.storedDust = tag.getInt("StoredDust");
        this.restoreDuration = Math.clamp(tag.getInt("RestoreDuration"), 0, 648000);
        this.restoreProgress = Math.clamp(tag.getInt("RestoreProgress"), 0, restoreDuration);
        this.restoreDustCost = Math.max(0, tag.getInt("RestoreDustCost"));
        this.restoreInput = ItemStack.parseOptional(registries, tag.getCompound("RestoreInput"));
        if (isRestoring() && (restoreDustCost <= 0 || targetGrade < 2 || targetGrade > 10
                || targetGrade <= getCurrentCardGrade() || !hasRestoreInput())) {
            resetRestore();
        }
        updateBlockState();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("TargetGrade", this.targetGrade);
        tag.putInt("StoredDust", this.storedDust);
        tag.putInt("RestoreProgress", this.restoreProgress);
        tag.putInt("RestoreDuration", this.restoreDuration);
        tag.putInt("RestoreDustCost", this.restoreDustCost);
        if (!this.restoreInput.isEmpty()) {
            tag.put("RestoreInput", this.restoreInput.save(registries));
        }
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
