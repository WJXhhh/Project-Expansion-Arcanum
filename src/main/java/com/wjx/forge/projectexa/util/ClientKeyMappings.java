package com.wjx.forge.projectexa.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.wjx.forge.projectexa.Main;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_ARCANE_TRANSMUTATION_TABLE = new KeyMapping(
            "key.projectexa.open_arcane_transmutation_table",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.projectexa"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ARCANE_TRANSMUTATION_TABLE);
    }
}
