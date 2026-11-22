package com.wjx.forge.projectexa.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.feature.sidepanel.RSSidePanelClickPacket", remap = false)
public interface RsiWorldPickInvoker {
    @Invoker(value = "handlePickBlock", remap = false)
    static ItemStack projectexa$pickBlock(ServerPlayer player, ItemStack stack) {
        throw new AssertionError();
    }
}
