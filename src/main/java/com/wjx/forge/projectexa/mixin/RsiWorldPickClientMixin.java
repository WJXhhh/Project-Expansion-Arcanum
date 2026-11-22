package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.util.WorldEmcPickClient;
import net.minecraftforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.client.input.WorldPickClient", remap = false)
public abstract class RsiWorldPickClientMixin {
    @Inject(method = "onPickBlock", at = @At("HEAD"), cancellable = true)
    private static void projectexa$coordinatePick(InputEvent.InteractionKeyMappingTriggered event, CallbackInfo ci) {
        if (WorldEmcPickClient.tryHandle(event)) ci.cancel();
    }
}
