package com.wjx.neoforge.projectexa.events;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.block.entity.*;
import com.wjx.neoforge.projectexa.capability.CapabilityAlchemicalBookLocations;
import com.wjx.neoforge.projectexa.item.ItemAlchemicalBook;
import com.wjx.neoforge.projectexa.registries.Capabilities;
import com.wjx.neoforge.projectexa.registries.Items;
import com.wjx.neoforge.projectexa.util.IHasCapability;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY,
                EntityType.PLAYER,
                (player, dir) -> new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.PLAYER, (ServerPlayer) player, null)
        );
        event.registerItem(
                Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ITEM,
                (stack, dir) -> new CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode.STACK, null, stack),
                Items.BASIC_ALCHEMICAL_BOOK.get(),
                Items.ADVANCED_ALCHEMICAL_BOOK.get(),
                Items.MASTER_ALCHEMICAL_BOOK.get(),
                Items.ARCANE_ALCHEMICAL_BOOK.get()
        );

        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IHasCapability icap) {
                icap.registerCapabilities(event);
            }
        }

        BlockEntityAdvancedAlchemicalChest.registerCapabilities(event);
        BlockEntityCollector.registerCapabilities(event);
        BlockEntityCondenserMK3.registerCapabilities(event);
        BlockEntityEMCLink.registerCapabilities(event);
        BlockEntityTransmutationInterface.registerCapabilities(event);
        BlockEntityRelay.registerCapabilities(event);
    }
}
