package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.mods.jei.JeiStoragePullPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.mods.jei.JeiStoragePullPacket", remap = false)
public interface RsiJeiStoragePullInvoker {
    @Invoker(value = "handleOnServer", remap = false)
    static void projectexa$handlePull(JeiStoragePullPacket packet, ServerPlayer player) {
        throw new AssertionError();
    }
}
