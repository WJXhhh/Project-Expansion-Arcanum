package com.wjx.forge.projectexa.gui.container;

import com.wjx.forge.projectexa.item.ItemPersonalEmcWarehouse;
import com.wjx.forge.projectexa.registries.MenuTypes;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerPersonalEmcWarehouse extends AbstractContainerMenu {
    private static final int FILTER_ROWS = 3;
    private final Inventory playerInventory;
    private final InteractionHand hand;
    private final ItemStack warehouse;
    private final SimpleContainer filters = new SimpleContainer(ItemPersonalEmcWarehouse.FILTER_SLOTS);

    public static ContainerPersonalEmcWarehouse fromNetwork(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        return new ContainerPersonalEmcWarehouse(windowId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ContainerPersonalEmcWarehouse(int windowId, Inventory inventory, InteractionHand hand) {
        super(MenuTypes.PERSONAL_EMC_WAREHOUSE.get(), windowId);
        this.playerInventory = inventory;
        this.hand = hand;
        this.warehouse = inventory.player.getItemInHand(hand);

        for (int slot = 0; slot < ItemPersonalEmcWarehouse.FILTER_SLOTS; slot++) {
            filters.setItem(slot, ItemPersonalEmcWarehouse.getFilter(warehouse, slot));
            addSlot(new FilterSlot(filters, slot, 8 + slot % 9 * 18, 18 + slot / 9 * 18));
        }

        int lockedHotbarSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 85 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new WarehouseLockSlot(inventory, column, 8 + column * 18, 143, column == lockedHotbarSlot));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < ItemPersonalEmcWarehouse.FILTER_SLOTS) {
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                setFilter(slotId, getCarried(), player);
            } else if (clickType == ClickType.SWAP && button >= 0 && button < 9) {
                setFilter(slotId, playerInventory.getItem(button), player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return ItemStack.EMPTY;
        }
        if (slotId < ItemPersonalEmcWarehouse.FILTER_SLOTS) {
            setFilter(slotId, ItemStack.EMPTY, player);
            return ItemStack.EMPTY;
        }

        ItemStack source = slots.get(slotId).getItem();
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (int filterSlot = 0; filterSlot < ItemPersonalEmcWarehouse.FILTER_SLOTS; filterSlot++) {
            if (filters.getItem(filterSlot).isEmpty()) {
                setFilter(filterSlot, source, player);
                // The physical stack intentionally stays in place; returning a non-empty stack here
                // makes vanilla retry quick-move forever because the source slot never changes.
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (ItemPersonalEmcWarehouse.sameWarehouse(warehouse, player.getInventory().getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    public int getFilterRows() {
        return FILTER_ROWS;
    }

    private boolean setFilter(int slot, ItemStack rawFilter, Player player) {
        ItemStack filter = rawFilter.isEmpty() ? ItemStack.EMPTY : ItemPersonalEmcWarehouse.sanitizeFilter(rawFilter);
        if (!rawFilter.isEmpty()) {
            IKnowledgeProvider provider = ItemPersonalEmcWarehouse.getOwner(warehouse)
                    .map(Util::getKnowledgeProvider).orElse(null);
            if (filter.isEmpty() || provider == null || !provider.hasKnowledge(filter)) {
                if (player instanceof ServerPlayer) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "item.projectexa.personal_emc_warehouse.invalid_filter"), true);
                }
                return false;
            }
            for (int existingSlot = 0; existingSlot < ItemPersonalEmcWarehouse.FILTER_SLOTS; existingSlot++) {
                if (existingSlot != slot && ItemStack.isSameItemSameTags(filters.getItem(existingSlot), filter)) {
                    return false;
                }
            }
        }

        filters.setItem(slot, filter);
        if (!player.level().isClientSide) {
            ItemPersonalEmcWarehouse.setFilter(warehouse, slot, filter);
            playerInventory.setChanged();
            if (player instanceof ServerPlayer serverPlayer) {
                for (int inventorySlot = 0; inventorySlot < playerInventory.getContainerSize(); inventorySlot++) {
                    if (ItemPersonalEmcWarehouse.sameWarehouse(warehouse, playerInventory.getItem(inventorySlot))) {
                        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                                ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, inventorySlot, warehouse));
                        break;
                    }
                }
            }
            broadcastChanges();
        }
        return true;
    }

    private static class FilterSlot extends Slot {
        private FilterSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class WarehouseLockSlot extends Slot {
        private final boolean locked;

        private WarehouseLockSlot(Inventory inventory, int slot, int x, int y, boolean locked) {
            super(inventory, slot, x, y);
            this.locked = locked;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !locked;
        }
    }
}
