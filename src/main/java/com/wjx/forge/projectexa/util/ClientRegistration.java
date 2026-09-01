package com.wjx.forge.projectexa.util;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.gui.GUICollector;
import com.wjx.forge.projectexa.gui.GUIArcaneTransmutationTablet;
import com.wjx.forge.projectexa.gui.GUIStoneTable;
import com.wjx.forge.projectexa.gui.GUIPersonalEmcWarehouse;
import com.wjx.forge.projectexa.registries.MenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistration {

    @SubscribeEvent
    public static void registerContainers(RegisterEvent event) {
        event.register(ForgeRegistries.MENU_TYPES.getRegistryKey(), helper -> {
            registerScreen(MenuTypes.COLLECTOR_TIER_1, GUICollector.Tier1::new);
            registerScreen(MenuTypes.COLLECTOR_TIER_2, GUICollector.Tier2::new);
            registerScreen(MenuTypes.COLLECTOR_TIER_3, GUICollector.Tier3::new);
            registerScreen(MenuTypes.ARCANE_TRANSMUTATION_TABLET, GUIArcaneTransmutationTablet::new);
            registerScreen(MenuTypes.STONE_TABLE, GUIStoneTable::new);
            registerScreen(MenuTypes.PERSONAL_EMC_WAREHOUSE, GUIPersonalEmcWarehouse::new);
        });
    }

    private static <C extends AbstractContainerMenu, U extends Screen & MenuAccess<C>> void registerScreen(RegistryObject<MenuType<C>> type, MenuScreens.ScreenConstructor<C, U> factory) {
        MenuScreens.register(type.get(), factory);
    }
}
