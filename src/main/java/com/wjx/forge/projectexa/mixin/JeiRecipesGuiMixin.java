package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.jei.JeiArcaneTransferScreen;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** Keeps arcane transfer available when JEI was opened from e.g. FTB Quests. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipesGui", remap = false)
public class JeiRecipesGuiMixin {
    @Unique
    private @Nullable JeiArcaneTransferScreen projectexa$transferContext;

    // Older JEI versions only request the menu; modern versions also require
    // a screen for both transfer validation and the actual button click.
    @Inject(method = "getParentContainerMenu", at = @At("RETURN"), cancellable = true, remap = false)
    private void projectexa$provideTransferMenu(CallbackInfoReturnable<AbstractContainerMenu> callback) {
        if (callback.getReturnValue() == null) {
            JeiArcaneTransferScreen context = projectexa$getTransferContext();
            if (context != null) {
                callback.setReturnValue(context.getMenu());
            }
        }
    }

    @Inject(method = "getParentContainerScreen", at = @At("RETURN"), cancellable = true,
            remap = false, require = 0)
    private void projectexa$provideTransferScreen(
            CallbackInfoReturnable<AbstractContainerScreen<?>> callback) {
        if (callback.getReturnValue() == null) {
            JeiArcaneTransferScreen context = projectexa$getTransferContext();
            if (context != null) {
                callback.setReturnValue(context);
            }
        }
    }

    @Unique
    private @Nullable JeiArcaneTransferScreen projectexa$getTransferContext() {
        Player player = Minecraft.getInstance().player;
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            projectexa$transferContext = null;
            return null;
        }
        if (projectexa$transferContext == null || !projectexa$transferContext.isFor(player)) {
            projectexa$transferContext = new JeiArcaneTransferScreen(player);
        }
        return projectexa$transferContext;
    }
}
