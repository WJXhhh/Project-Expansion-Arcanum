package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.rsi.RsiCraftingKnowledge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** RSI reports actual primary products here even when its FTB Quests bridge is disabled. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.feature.ftbquests.ExternalItemProgressBridge", remap = false)
public abstract class RsiCraftedOutputMixin {
    @Inject(method = "enqueueCrafted", at = @At("HEAD"))
    private static void projectexa$rememberCraftedProduct(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        RsiCraftingKnowledge.recordCrafted(player, stack);
    }
}
