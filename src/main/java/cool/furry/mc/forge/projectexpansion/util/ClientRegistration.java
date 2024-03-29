package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.gui.GUICollector;
import cool.furry.mc.forge.projectexpansion.registries.MenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistration {

    @SubscribeEvent
    public static void registerContainers(RegisterEvent event) {
        event.register(Registry.MENU_REGISTRY, helper -> {
            registerScreen(MenuTypes.COLLECTOR_TIER_1, GUICollector.Tier1::new);
            registerScreen(MenuTypes.COLLECTOR_TIER_2, GUICollector.Tier2::new);
            registerScreen(MenuTypes.COLLECTOR_TIER_3, GUICollector.Tier3::new);
        });
    }

    private static <C extends AbstractContainerMenu, U extends Screen & MenuAccess<C>> void registerScreen(RegistryObject<MenuType<C>> type, MenuScreens.ScreenConstructor<C, U> factory) {
        MenuScreens.register(type.get(), factory);
    }
}
