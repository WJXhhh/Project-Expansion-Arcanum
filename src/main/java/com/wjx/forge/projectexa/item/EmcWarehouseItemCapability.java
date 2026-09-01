package com.wjx.forge.projectexa.item;

import moze_intel.projecte.capability.ItemCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

/** Exposes a personal EMC warehouse as a regular Forge item inventory. */
public class EmcWarehouseItemCapability extends ItemCapability<IItemHandler> {
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(
            () -> new PersonalEmcWarehouseHandler(getStack(), null));

    @Override
    public Capability<IItemHandler> getCapability() {
        return ForgeCapabilities.ITEM_HANDLER;
    }

    @Override
    public LazyOptional<IItemHandler> getLazyCapability() {
        return handler;
    }
}
