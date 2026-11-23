package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KnowledgeSyncChangePKT.class, remap = false)
public class KnowledgeSyncChangePKTMixin {
    @Shadow @Final private ItemInfo change;

    @WrapMethod(method = "handle(Lnet/minecraftforge/network/NetworkEvent$Context;)V", remap = false)
    private void projectexa$refreshTargets(NetworkEvent.Context context, Operation<Void> original) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof ContainerArcaneTransmutationTablet container)) {
            original.call(context);
            return;
        }

        var provider = container.getProvider();
        boolean previouslyKnown = provider.hasKnowledge(change);
        original.call(context);
        boolean known = provider.hasKnowledge(change);
        if (previouslyKnown == known) {
            container.transmutationInventory.invalidateKnowledge();
        } else if (known) {
            container.transmutationInventory.itemLearned();
        } else {
            container.transmutationInventory.itemUnlearned();
        }
    }
}
