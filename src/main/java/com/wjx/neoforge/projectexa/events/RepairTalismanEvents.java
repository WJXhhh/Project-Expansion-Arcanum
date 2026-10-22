package com.wjx.neoforge.projectexa.events;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.integrations.curios.CuriosIntegration;
import com.wjx.neoforge.projectexa.item.EmcWarehouseInventoryHandler;
import com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse;
import com.wjx.neoforge.projectexa.registries.AttachmentTypes;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Repair non-player living inventories, including maids, but never virtual previews. */
@EventBusSubscriber(modid = Main.MOD_ID)
public final class RepairTalismanEvents {
    private RepairTalismanEvents() {}

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player
                || entity.level().isClientSide || !entity.isAlive()) return;
        IItemHandler physical = entity.getCapability(Capabilities.ItemHandler.ENTITY);
        if (physical instanceof EmcWarehouseInventoryHandler wrapped) physical = wrapped.physicalInventory();
        IItemHandler curios = CuriosIntegration.getAll(entity);
        if (entity.tickCount % 20 == 0) { refresh(physical); refresh(curios); }
        int interval = ProjectEConfig.server.cooldown.player.repair.get();
        if (interval < 0 || !hasTalisman(physical) && !hasTalisman(curios)) return;
        int remaining = entity.getData(AttachmentTypes.REPAIR_COOLDOWN);
        if (remaining > 0) {
            entity.setData(AttachmentTypes.REPAIR_COOLDOWN, remaining - 1);
            return;
        }
        Set<ItemStack> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        repair(physical, seen); repair(curios, seen);
        entity.setData(AttachmentTypes.REPAIR_COOLDOWN, Math.max(0, interval - 1));
    }

    private static void refresh(IItemHandler handler) {
        if (handler == null) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof ItemPersonalEmcWarehouse) ItemPersonalEmcWarehouse.refreshAvailability(stack);
        }
    }

    private static boolean hasTalisman(IItemHandler handler) {
        if (handler == null) return false;
        for (int i = 0; i < handler.getSlots(); i++) if (handler.getStackInSlot(i).is(PEItems.REPAIR_TALISMAN)) return true;
        return false;
    }

    private static void repair(IItemHandler handler, Set<ItemStack> seen) {
        if (handler == null) return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (seen.add(stack) && !stack.isEmpty() && stack.getCapability(PECapabilities.MODE_CHANGER_ITEM_CAPABILITY) == null
                    && ItemHelper.isRepairableDamagedItem(stack)) stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }
}
