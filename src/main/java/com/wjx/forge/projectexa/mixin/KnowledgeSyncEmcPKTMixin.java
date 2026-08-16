package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncEmcPKT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KnowledgeSyncEmcPKT.class, remap = false)
public class KnowledgeSyncEmcPKTMixin {
    @Inject(method = "handle(Lnet/minecraftforge/network/NetworkEvent$Context;)V", at = @At("TAIL"))
    private void projectexa$refreshTargets(NetworkEvent.Context context, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.transmutationInventory.updateClientTargets();
        }
    }
}
