package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.RSIntegrationMod;
import com.huanghuang.rsintegration.config.RSIntegrationConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps RSI's server-configured key binding out of the mod construction phase. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.transfer.ContainerTransferClient", remap = false)
public abstract class RsiKeyMappingsMixin {
    @Shadow(remap = false)
    public static KeyMapping KEY_STORE_ALL;

    @Shadow(remap = false)
    public static KeyMapping KEY_TOGGLE_MODE;

    @Shadow(remap = false)
    private static boolean keyMappingsRegistered;

    @Inject(method = "registerKeyMappings()V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void projectexa$deferKeyMappings(CallbackInfo callbackInfo) {
        if (!keyMappingsRegistered) {
            keyMappingsRegistered = true;
            RSIntegrationMod.MOD_BUS.addListener(RsiKeyMappingsMixin::projectexa$registerKeyMappings);
        }
        callbackInfo.cancel();
    }

    @Unique
    private static void projectexa$registerKeyMappings(RegisterKeyMappingsEvent event) {
        // SERVER configs are not loaded until a world is opened/joined. Never call get() beforehand.
        int transferKey = RSIntegrationConfig.SERVER_SPEC.isLoaded()
                ? RSIntegrationConfig.CONTAINER_TRANSFER_KEY.get()
                : RSIntegrationConfig.CONTAINER_TRANSFER_KEY.getDefault();
        KEY_STORE_ALL = new KeyMapping("key.rsi.store_all", KeyConflictContext.GUI,
                InputConstants.Type.KEYSYM, transferKey, "key.categories.rsi");
        KEY_TOGGLE_MODE = new KeyMapping("key.rsi.toggle_mode", KeyConflictContext.UNIVERSAL,
                InputConstants.Type.KEYSYM, 71, "key.categories.rsi");
        event.register(KEY_STORE_ALL);
        event.register(KEY_TOGGLE_MODE);
    }
}
