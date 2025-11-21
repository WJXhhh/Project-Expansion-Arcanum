package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.client.Keybinds;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICollector;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Input;
import cool.furry.mc.neoforge.projectexpansion.gui.GUICondenserMK3Output;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketOpenTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
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
