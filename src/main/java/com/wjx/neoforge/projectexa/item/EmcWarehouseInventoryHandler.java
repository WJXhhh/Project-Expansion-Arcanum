package com.wjx.neoforge.projectexa.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/** Adds bound EMC warehouse slots after an entity's physical inventory slots. */
public class EmcWarehouseInventoryHandler implements IItemHandlerModifiable {
    private final LivingEntity holder;
    private final IItemHandler physicalInventory;
    private final List<IItemHandler> warehouseSources;

    public EmcWarehouseInventoryHandler(LivingEntity holder, IItemHandler physicalInventory) {
        this(holder, physicalInventory, null);
    }

    public IItemHandler physicalInventory() { return physicalInventory; }

    public EmcWarehouseInventoryHandler(LivingEntity holder, IItemHandler physicalInventory,
                                        @Nullable IItemHandler accessoryInventory) {
        this.holder = holder;
        this.physicalInventory = physicalInventory;
        this.warehouseSources = new ArrayList<>();
        this.warehouseSources.add(physicalInventory);
        if (accessoryInventory != null && accessoryInventory != physicalInventory) {
            this.warehouseSources.add(accessoryInventory);
        }
    }

    public static boolean hasWarehouse(IItemHandler physicalInventory) {
        return hasWarehouse(physicalInventory, null);
    }

    public static boolean hasWarehouse(IItemHandler physicalInventory,
                                       @Nullable IItemHandler accessoryInventory) {
        if (!ItemPersonalEmcWarehouse.ENABLED) {
            return false;
        }
        IItemHandler[] sources = {physicalInventory, accessoryInventory};
        for (IItemHandler source : sources) {
            if (source == null) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (stack.getItem() instanceof ItemPersonalEmcWarehouse
                        && ItemPersonalEmcWarehouse.isBound(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getSlots() {
        return physicalInventory.getSlots() + warehouses().size() * ItemPersonalEmcWarehouse.FILTER_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        LocatedSlot located = locate(slot);
        return located == null ? ItemStack.EMPTY : located.handler().getStackInSlot(located.slot());
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        // SlotItemHandler uses this for both menu synchronization and player clicks.
        // Preserve the physical handler's direct-write behavior, including its callbacks.
        if (slot >= 0 && slot < physicalInventory.getSlots()) {
            if (physicalInventory instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, stack);
                return;
            }
            throw new UnsupportedOperationException("The physical inventory does not support direct slot writes");
        }
        // Virtual stacks are derived from EMC, not stored items. Replacing one would
        // bypass accounting; consumers must use insertItem/extractItem instead.
        throw new UnsupportedOperationException("EMC warehouse slots do not support direct slot writes");
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        LocatedSlot located = locate(slot);
        return located == null ? stack : located.handler().insertItem(located.slot(), stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        LocatedSlot located = locate(slot);
        return located == null ? ItemStack.EMPTY : located.handler().extractItem(located.slot(), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        LocatedSlot located = locate(slot);
        return located == null ? 0 : located.handler().getSlotLimit(located.slot());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        LocatedSlot located = locate(slot);
        return located != null && located.handler().isItemValid(located.slot(), stack);
    }

    private LocatedSlot locate(int slot) {
        if (slot < 0) {
            return null;
        }
        int physicalSlots = physicalInventory.getSlots();
        if (slot < physicalSlots) {
            return new LocatedSlot(physicalInventory, slot);
        }
        int virtualSlot = slot - physicalSlots;
        List<ItemStack> warehouses = warehouses();
        int warehouseIndex = virtualSlot / ItemPersonalEmcWarehouse.FILTER_SLOTS;
        if (warehouseIndex < 0 || warehouseIndex >= warehouses.size()) {
            return null;
        }
        return new LocatedSlot(new PersonalEmcWarehouseHandler(warehouses.get(warehouseIndex), holder),
                virtualSlot % ItemPersonalEmcWarehouse.FILTER_SLOTS);
    }

    private List<ItemStack> warehouses() {
        List<ItemStack> result = new ArrayList<>();
        for (IItemHandler source : warehouseSources) {
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack stack = source.getStackInSlot(slot);
                if (stack.getItem() instanceof ItemPersonalEmcWarehouse
                        && ItemPersonalEmcWarehouse.isBound(stack)) {
                    result.add(stack);
                }
            }
        }
        return result;
    }

    /**
     * Counts matching virtual items while treating warehouses bound to the same
     * player as one EMC pool. The returned value saturates at {@code limit}.
     */
    public int countMatchingWarehouseItems(Predicate<ItemStack> predicate, int limit) {
        if (limit <= 0) {
            return 0;
        }
        Map<UUID, Integer> availableByAccount = new HashMap<>();
        for (ItemStack warehouse : warehouses()) {
            UUID owner = ItemPersonalEmcWarehouse.getOwner(warehouse).orElse(null);
            if (owner == null) {
                continue;
            }
            if (holder.level().isClientSide
                    && !ItemPersonalEmcWarehouse.hasAvailabilityCountSnapshot(warehouse)
                    && !owner.equals(holder.getUUID())) {
                continue;
            }
            PersonalEmcWarehouseHandler handler = new PersonalEmcWarehouseHandler(warehouse, holder);
            int bestAvailable = 0;
            for (int slot = 0; slot < ItemPersonalEmcWarehouse.FILTER_SLOTS; slot++) {
                ItemStack available = handler.getStackInSlot(slot);
                if (!available.isEmpty() && predicate.test(available)) {
                    bestAvailable = Math.max(bestAvailable, Math.min(limit, available.getCount()));
                }
            }
            if (bestAvailable > 0) {
                availableByAccount.merge(owner, bestAvailable, Math::max);
            }
        }

        int total = 0;
        for (int available : availableByAccount.values()) {
            if (available >= limit - total) {
                return limit;
            }
            total += available;
        }
        return total;
    }

    private record LocatedSlot(IItemHandler handler, int slot) {
    }
}
