package com.wjx.forge.projectexa.util;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyHandler {
    private ClientKeyHandler() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        while (ClientKeyMappings.OPEN_ARCANE_TRANSMUTATION_TABLE.consumeClick()) {
            PacketHandler.sendToServer(new PacketOpenArcaneTransmutationTablet());
        }
    }
}
