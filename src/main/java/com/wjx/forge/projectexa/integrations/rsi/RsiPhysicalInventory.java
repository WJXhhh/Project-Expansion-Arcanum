package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.crafting.MaterialMatcher;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Detached quote-only stock. Never loads RSI's RS-linked ExtractionLedger or extracts real items. */
final class RsiPhysicalInventory {
    private static BackpackAccess backpackAccess;
    private final List<ItemStack> stacks = new ArrayList<>();

    private RsiPhysicalInventory() {}

    static RsiPhysicalInventory snapshot(ServerPlayer player) {
        RsiPhysicalInventory stock = new RsiPhysicalInventory();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) stock.stacks.add(stack.copy());
        }
        if (!ModList.get().isLoaded("sophisticatedbackpacks")) return stock;

        // Mirror RSI: backpacks in main inventory and equipped Curios, not armor/offhand,
        // loose Curios ingredients, nested backpacks or the player's wrapped item capability.
        BackpackAccess access = backpackAccess();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ItemStack stack : player.getInventory().items) stock.addBackpack(stack, access, seen);
        if (ModList.get().isLoaded(IntegrationHelper.CURIO_MODID)) {
            IItemHandler curios = CuriosIntegration.getAll(player);
            if (curios != null) {
                for (int slot = 0; slot < curios.getSlots(); slot++) {
                    stock.addBackpack(curios.getStackInSlot(slot), access, seen);
                }
            }
        }
        return stock;
    }

    /** Allocates only detached copies, so multiple demands cannot quote the same physical stack twice. */
    long allocateUpTo(ItemStack template, long requested) {
        long allocated = 0;
        for (ItemStack stack : stacks) {
            if (allocated >= requested) break;
            if (stack.isEmpty() || !MaterialMatcher.sameRuntimeFragment(template, stack)) continue;
            int used = (int) Math.min(requested - allocated, stack.getCount());
            stack.shrink(used);
            allocated += used;
        }
        return allocated;
    }

    private void addBackpack(ItemStack stack, BackpackAccess access, Set<Object> seen) {
        if (stack.isEmpty()) return;
        Object wrapper = stack.getCapability(access.capability()).resolve().orElse(null);
        if (wrapper == null || !seen.add(wrapper)) return;
        try {
            Object inventory = access.inventory().invoke(wrapper);
            if (!(inventory instanceof IItemHandler handler)) {
                throw new IllegalStateException("Unsupported Sophisticated Backpacks inventory");
            }
            if (!seen.add(handler)) return;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack contents = handler.getStackInSlot(slot);
                if (!contents.isEmpty() && !ArcaneCraftingInventory.protectedStack(contents)) {
                    stacks.add(contents.copy());
                }
            }
        } catch (ReflectiveOperationException exception) {
            // Unknown stock is not empty stock. The caller reports an unavailable quote on API failure.
            throw new IllegalStateException("Cannot read Sophisticated Backpacks inventory", exception);
        }
    }

    private static synchronized BackpackAccess backpackAccess() {
        if (backpackAccess != null) return backpackAccess;
        try {
            Class<?> capabilityApi = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper");
            Object capability = capabilityApi.getMethod("getCapabilityInstance").invoke(null);
            if (!(capability instanceof Capability<?> typedCapability)) {
                throw new IllegalStateException("Unsupported Sophisticated Backpacks capability");
            }
            Method inventory = Class.forName(
                    "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper")
                    .getMethod("getInventoryForUpgradeProcessing");
            backpackAccess = new BackpackAccess(typedCapability, inventory);
            return backpackAccess;
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Cannot resolve Sophisticated Backpacks API", exception);
        }
    }

    private record BackpackAccess(Capability<?> capability, Method inventory) {}
}
