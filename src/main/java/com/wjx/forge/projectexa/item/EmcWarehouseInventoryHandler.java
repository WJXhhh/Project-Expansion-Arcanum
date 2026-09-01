package com.wjx.forge.projectexa.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/** Adds bound EMC warehouse slots after an entity's physical inventory slots. */
public class EmcWarehouseInventoryHandler implements IItemHandler {
    private final LivingEntity holder;
    private final IItemHandler physicalInventory;

    public EmcWarehouseInventoryHandler(LivingEntity holder, IItemHandler physicalInventory) {
        this.holder = holder;
        this.physicalInventory = physicalInventory;
    }

    public static boolean hasWarehouse(IItemHandler physicalInventory) {
        for (int slot = 0; slot < physicalInventory.getSlots(); slot++) {
            ItemStack stack = physicalInventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemPersonalEmcWarehouse
                    && ItemPersonalEmcWarehouse.isBound(stack)) {
                return true;
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
        for (int slot = 0; slot < physicalInventory.getSlots(); slot++) {
            ItemStack stack = physicalInventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemPersonalEmcWarehouse
                    && ItemPersonalEmcWarehouse.isBound(stack)) {
                result.add(stack);
            }
        }
        return result;
    }

    private record LocatedSlot(IItemHandler handler, int slot) {
    }
}
