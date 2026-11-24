package com.howlite.cobblemoncards.block.entity;

import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.block.CardCabinetBlock;
import com.howlite.cobblemoncards.menu.CardCabinetMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import com.howlite.cobblemoncards.util.CardAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

public class CardCabinetBlockEntity extends BlockEntity implements ImplementedInventory, MenuProvider {
    // 1000 pages of 12 slots each = 12000 slots total (practically infinite card storage)
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(12000, ItemStack.EMPTY);

    public CardCabinetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARD_CABINET_BE, pos, state);
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveCabinetItems(tag, inventory, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.clear();
        loadCabinetItems(tag, inventory, registries);
    }

    public static CompoundTag saveCabinetItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        int skipped = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            try {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                // ItemStack#save does NOT mutate the tag it is given: NbtOps#mergeToMap makes a
                // shallow copy and returns the merged result. Dropping the return value here
                // means only {Slot: n} is written and every card is lost on the next load.
                listTag.add(stack.save(registries, itemTag));
            } catch (Exception e) {
                // Never let a single bad stack abort the whole chunk save (that would wipe
                // far more than the cabinet).
                skipped++;
                CobblemonCards.LOGGER.error("[CardCabinet] Failed to save item in slot {} ({}), it will be lost",
                        i, stack, e);
            }
        }
        if (skipped > 0) {
            CobblemonCards.LOGGER.error("[CardCabinet] {} item(s) could not be serialized and were dropped from the save", skipped);
        }
        tag.put("Items", listTag);
        return tag;
    }

    public static void loadCabinetItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        items.clear();
        if (!tag.contains("Items", Tag.TAG_LIST)) {
            // No inventory tag at all: either a brand-new cabinet, or the block entity NBT
            // never made it to disk. Both are worth knowing about when cards go missing.
            CobblemonCards.LOGGER.debug("[CardCabinet] No 'Items' tag found while loading a cabinet");
            return;
        }
        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        int loaded = 0;
        int failed = 0;
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot;
            if (itemTag.contains("Slot", Tag.TAG_INT)) {
                slot = itemTag.getInt("Slot");
            } else {
                // Written by ContainerHelper.saveAllItems in 1.0.4 and earlier: the slot index is
                // a byte, so only the first 256 slots survived that format.
                slot = itemTag.getByte("Slot") & 0xFF;
            }
            if (slot < 0 || slot >= items.size()) {
                failed++;
                CobblemonCards.LOGGER.error("[CardCabinet] Discarding item with out-of-range slot {} (size {})", slot, items.size());
                continue;
            }
            // ItemStack.parse() returns Optional.empty() on ANY decoding problem (unknown
            // item id, invalid count, a data component whose codec rejected the data...).
            // Swallowing that silently is exactly what makes a cabinet look "wiped", so log it.
            if (!itemTag.contains("id")) {
                // Written by a build affected by the "ItemStack#save return value discarded"
                // bug: only {Slot: n} made it to disk, so the card itself is unrecoverable.
                failed++;
                CobblemonCards.LOGGER.error(
                        "[CardCabinet] Slot {} was saved without an item id (corrupted by a pre-fix build) and cannot be restored",
                        slot);
                continue;
            }
            java.util.Optional<ItemStack> parsed = ItemStack.parse(registries, itemTag);
            if (parsed.isEmpty()) {
                failed++;
                CobblemonCards.LOGGER.error("[CardCabinet] Failed to parse item for slot {}; raw tag = {}", slot, itemTag);
                continue;
            }
            items.set(slot, parsed.get());
            loaded++;
        }
        if (failed > 0) {
            CobblemonCards.LOGGER.error("[CardCabinet] Loaded {} item(s), but {} entrie(s) failed to load and were lost", loaded, failed);
        } else {
            CobblemonCards.LOGGER.debug("[CardCabinet] Loaded {} item(s)", loaded);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // IMPORTANT: do NOT send the inventory to clients here.
        // This cabinet has 12 000 slots; serialising them into every block-entity update
        // packet (and into every chunk packet) can easily produce multi-megabyte payloads,
        // which throws "Packet too big" on the encoder and kicks players.
        // The client renderer only needs the block state (FACING / FILL_LEVEL) and the
        // contents are synced through CardCabinetMenu when the GUI is open.
        return super.getUpdateTag(registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            // Single pass over the inventory to compute fill level (0-7) and full-cabinet check.
            int count = 0;
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) count++;
            }
            int newFillLevel = count == 0 ? 0 : Math.min(7, 1 + (count - 1) * 7 / inventory.size());
            BlockState currentState = getBlockState();
            if (currentState.getValue(CardCabinetBlock.FILL_LEVEL) != newFillLevel) {
                level.setBlock(getBlockPos(),
                        currentState.setValue(CardCabinetBlock.FILL_LEVEL, newFillLevel),
                        3); // flag 3 = update + notify neighbours (comparator)
            }
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());

            // Check if cabinet is completely full (12,000 cards)
            if (count == inventory.size()) {
                for (Player player : level.players()) {
                    if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu instanceof CardCabinetMenu cabinetMenu) {
                        if (cabinetMenu.getContainer() == this) {
                            CardAdvancementManager.grantAdvancement(serverPlayer, "full_cabinet");
                        }
                    }
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cobblemon-cards.card_cabinet");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int count = 0;
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()) count++;
            }
            if (count == inventory.size()) {
                CardAdvancementManager.grantAdvancement(serverPlayer, "full_cabinet");
            }
        }
        return new CardCabinetMenu(syncId, playerInventory, this);
    }
}
