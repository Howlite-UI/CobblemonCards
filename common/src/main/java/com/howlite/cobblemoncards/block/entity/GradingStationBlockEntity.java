package com.howlite.cobblemoncards.block.entity;

import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;

public class GradingStationBlockEntity extends BlockEntity implements ImplementedInventory {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);
    private int timer = 0;
    private static final Random RANDOM = new Random();

    public GradingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRADING_STATION_BE, pos, state);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.sync();
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(this.inventory, slot, count);
        if (!result.isEmpty()) {
            this.sync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.inventory, slot);
        this.sync();
        return result;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(tag, this.inventory, registries);
        this.timer = tag.getInt("Timer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.inventory, registries);
        tag.putInt("Timer", this.timer);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GradingStationBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // Logic du Moniteur dans le tick (Détection de disparition)
        BlockPos monitorPos = pos.above();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(monitorPos).getBlock());
        if (!id.getNamespace().equals("cobblemon") || !id.getPath().equals("monitor")) {
            if (!blockEntity.getItem(0).isEmpty()) {
                Containers.dropContents(level, pos, blockEntity);
                blockEntity.inventory.get(0).setCount(0); // Clear logic
                blockEntity.timer = 0;
                blockEntity.sync();
            }
            return;
        }

        if (blockEntity.timer > 0) {
            blockEntity.timer--;

            if (blockEntity.timer == 0) {
                blockEntity.finalizeGrading();
            }
            blockEntity.setChanged();
        }

        // On met à jour l'écran du moniteur à chaque tick
        blockEntity.updateMonitorState();
    }

    private void updateMonitorState() {
        if (level == null) return;

        BlockPos monitorPos = worldPosition.above();
        BlockState monitorState = level.getBlockState(monitorPos);
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(monitorState.getBlock());

        // On vérifie si c'est bien un moniteur
        if (!id.getNamespace().equals("cobblemon") || !id.getPath().equals("monitor")) return;

        // On récupère la propriété "screen" de manière générique
        Property<?> screenProperty = null;
        for (Property<?> prop : monitorState.getProperties()) {
            if (prop.getName().equals("screen")) {
                screenProperty = prop;
                break;
            }
        }

        if (screenProperty == null) return;

        String targetValue = "off";
        ItemStack card = getItem(0);

        if (card.isEmpty()) {
            targetValue = "off";
        } else if (timer > 0) {
            // Progression Bleue dynamique basée sur gradingStationProcessTime
            int totalTicks = CobblemonCardsConfig.gradingStationProcessTime;
            int ticksPerStage = Math.max(1, totalTicks / 9);
            int stage = Math.min(9, Math.max(1, 9 - (timer / ticksPerStage)));
            targetValue = "blue_progress_" + stage;
        } else {
            // Finie : Vert
            targetValue = "green_progress_9";
        }

        // Application de l'état si différent
        applyScreenValue(monitorPos, monitorState, screenProperty, targetValue);
    }

    private <T extends Comparable<T>> void applyScreenValue(BlockPos pos, BlockState state, Property<T> property, String valueName) {
        Optional<T> value = property.getValue(valueName);
        if (value.isPresent()) {
            if (!state.getValue(property).equals(value.get())) {
                level.setBlock(pos, state.setValue(property, value.get()), Block.UPDATE_ALL);
            }
        }
    }

    private void finalizeGrading() {
        ItemStack stack = getItem(0);
        if (!stack.isEmpty()) {
            CardData oldData = stack.get(ModDataComponents.CARD_DATA);
            if (oldData != null && oldData.grade() == 0) {
                int newGrade = 1 + RANDOM.nextInt(10);
                // Grade 1 = +3%, Grade 5 = +15%, Grade 10 = +30% (nerfé depuis +50%)
                float bonusMultiplier = newGrade * 0.03f;
                float newStatValue = oldData.statValue() + (oldData.statValue() * bonusMultiplier);

                // A high grade is the main way to earn a "trainer" stat (Exp / Catch / Shiny).
                // Cosmetic cards are excluded: they are meant to carry no stats at all.
                CardStat finalStat = oldData.stat();
                if (!com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(oldData.pokemonId())) {
                    CardStat trainerStat = com.howlite.cobblemoncards.util.CardStatUtil
                            .rollTrainerStatForGrade(newGrade, RANDOM);
                    if (trainerStat != null) {
                        finalStat = trainerStat;
                    }
                }

                CardData newData = new CardData(
                        oldData.pokemonId(),
                        oldData.isShiny(),
                        oldData.rarity(),
                        finalStat,
                        newStatValue,
                        newGrade,
                        oldData.background(),
                        oldData.effect()
                );
                
                stack.set(ModDataComponents.CARD_DATA, newData);

                if (level != null) {
                    level.playSound(null, worldPosition, SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cobblemon", "block.fossil_machine.finished")), SoundSource.BLOCKS, 1.0f, 1.0f);
                    
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 
                            10, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
        this.sync();
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
        this.setChanged();
    }

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
}