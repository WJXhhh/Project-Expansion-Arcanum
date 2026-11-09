package com.wjx.forge.projectexa.mixin;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TransmutationInventory.class, remap = false)
public interface TransmutationInventoryAccessor {
    // Preserve ProjectE's filter behavior, including other mods' matcher injections.
    @Invoker("doesItemMatchFilter")
    boolean projectexa$matchesFilter(ItemInfo info);
}
