package com.wjx.neoforge.projectexa.events;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.GUIStoneTable;
import com.wjx.neoforge.projectexa.gui.GUIPersonalEmcWarehouse;
import com.wjx.neoforge.projectexa.client.Keybinds;
import com.wjx.neoforge.projectexa.gui.GUIArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.gui.GUICollector;
import com.wjx.neoforge.projectexa.gui.GUICondenserMK3Input;
import com.wjx.neoforge.projectexa.gui.GUICondenserMK3Output;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketOpenTransmutationTablet;
import com.wjx.neoforge.projectexa.registries.MenuTypes;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ClientEvents {
    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(MenuTypes.STONE_TABLE.get(), GUIStoneTable::new);
            event.register(MenuTypes.PERSONAL_EMC_WAREHOUSE.get(), GUIPersonalEmcWarehouse::new);
            event.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
            event.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
            event.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
            event.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
            event.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
            event.register(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), GUIArcaneTransmutationTablet::new);
        }

        @SubscribeEvent
        public static void registerKeyMappingsEvent(RegisterKeyMappingsEvent event) {
            Keybinds.register(event);
        }
    }

    @EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Pre event) {
            if (!Keybinds.REGISTERED) return;

            boolean openTransmutationTablet = Keybinds.OPEN_TRANSMUTATION_TABLET.consumeClick();
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null) return;

            if (openTransmutationTablet) PacketDistributor.sendToServer(PacketOpenTransmutationTablet.INSTANCE);
        }
    }
}
