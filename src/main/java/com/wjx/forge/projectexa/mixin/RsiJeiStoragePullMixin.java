package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.mods.jei.JeiStoragePullPacket", remap = false)
public abstract class RsiJeiStoragePullMixin {
    @Inject(method = "notify", at = @At("HEAD"), cancellable = true)
    private static void projectexa$captureFailure(ServerPlayer player, String key, Object[] args, CallbackInfo ci) {
        if (RsiItemRetrieval.captureJeiFailure(player, key, args)) ci.cancel();
    }
}
