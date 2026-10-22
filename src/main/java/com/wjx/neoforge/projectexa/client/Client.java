package com.wjx.neoforge.projectexa.client;

import com.wjx.neoforge.projectexa.Main;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Main.MOD_ID, dist = Dist.CLIENT)
public class Client {
    public Client(ModContainer modContainer, IEventBus eventBus) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
