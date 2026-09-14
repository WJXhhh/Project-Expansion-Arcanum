package com.wjx.forge.projectexa.events;

import com.wjx.forge.projectexa.Main;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Extends ProjectE's Repair Talisman to living entities other than players.
 *
 * <p>ProjectE's implementation deliberately gates its normal inventory tick
 * behind {@link Player}. Curios can still tick the item on any living entity,
 * so this listener supplies the missing non-player path. This also covers
 * entities, such as Touhou Little Maid maids, whose equipment is exposed via
 * a Forge item-handler capability.</p>
 */
@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public final class RepairTalismanEvents {
    private static final String COOLDOWN_TAG = "Cooldown";

    private RepairTalismanEvents() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity instanceof Player || !entity.isAlive()) {
            return;
        }

        List<IItemHandler> handlers = getItemHandlers(entity);
        if (handlers.isEmpty()) {
            return;
        }

        Set<ItemStack> talismans = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IItemHandler handler : handlers) {
            collectRepairTalismans(handler, talismans);
        }
        if (talismans.isEmpty()) {
            return;
        }

        int cooldown = ProjectEConfig.server.cooldown.player.repair.get();
        if (cooldown < 0) {
            return;
        }

        boolean ready = false;
        for (ItemStack talisman : talismans) {
            CompoundTag tag = talisman.getOrCreateTag();
            int remaining = tag.getByte(COOLDOWN_TAG);
            if (remaining > 0) {
                tag.putByte(COOLDOWN_TAG, (byte) (remaining - 1));
            } else {
                ready = true;
            }
        }

        if (ready) {
            repairAllItems(handlers);
            // Set every talisman to the same cooldown so multiple talismans
            // cannot stagger into repairing the same item every tick.
            byte reset = (byte) Math.max(0, cooldown - 1);
            for (ItemStack talisman : talismans) {
                talisman.getOrCreateTag().putByte(COOLDOWN_TAG, reset);
            }
        }
    }

    private static List<IItemHandler> getItemHandlers(LivingEntity entity) {
        List<IItemHandler> handlers = new ArrayList<>(2);
        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handlers::add);

        if (ModList.get().isLoaded(IntegrationHelper.CURIO_MODID)) {
            IItemHandler curios = CuriosIntegration.getAll(entity);
            if (curios != null) {
                handlers.add(curios);
            }
        }
        return handlers;
    }

    private static void collectRepairTalismans(IItemHandler handler, Set<ItemStack> talismans) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.getItem() == PEItems.REPAIR_TALISMAN.get()) {
                talismans.add(stack);
            }
        }
    }

    private static boolean canRepair(ItemStack stack) {
        return !stack.isEmpty()
                && !stack.getCapability(PECapabilities.MODE_CHANGER_ITEM_CAPABILITY).isPresent()
                && ItemHelper.isRepairableDamagedItem(stack);
    }

    private static boolean repairAllItems(List<IItemHandler> handlers) {
        boolean repaired = false;
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IItemHandler handler : handlers) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (seen.add(stack) && canRepair(stack)) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                    repaired = true;
                }
            }
        }
        return repaired;
    }
}
