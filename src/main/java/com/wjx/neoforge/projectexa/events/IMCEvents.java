package com.wjx.neoforge.projectexa.events;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.integrations.top.TOPIntegration;
import moze_intel.projecte.integration.IntegrationHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class IMCEvents {
    @SubscribeEvent
    public static void interModEnqueueEvent(InterModEnqueueEvent event) {
        ModList modList = ModList.get();
        if (modList.isLoaded(IntegrationHelper.TOP_MODID)) {
            TOPIntegration.sendIMC(event);
        }
    }
}
