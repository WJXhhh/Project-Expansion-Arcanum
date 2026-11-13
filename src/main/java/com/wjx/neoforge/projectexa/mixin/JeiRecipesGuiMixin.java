package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.integrations.jei.JeiArcaneTransferScreen;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/** Keeps arcane transfer available when JEI was opened from e.g. FTB Quests. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipesGui", remap = false)
public class JeiRecipesGuiMixin {
    @Shadow private @Nullable Screen parentScreen;

    @Unique
    private @Nullable JeiArcaneTransferScreen projectexa$transferContext;

    // Run before JEI can reuse the local slot holding this in newer versions.
    @Inject(method = "getParentContainerMenu", at = @At("HEAD"), cancellable = true, remap = false)
    private void projectexa$provideTransferMenu(CallbackInfoReturnable<AbstractContainerMenu> callback) {
        Screen parent = parentScreen != null ? parentScreen : Minecraft.getInstance().screen;
        if (!(parent instanceof AbstractContainerScreen<?>)) {
            JeiArcaneTransferScreen context = projectexa$getTransferContext();
            if (context != null) {
                callback.setReturnValue(context.getMenu());
            }
        }
    }

    @Unique
    private @Nullable JeiArcaneTransferScreen projectexa$getTransferContext() {
        Player player = Minecraft.getInstance().player;
        if (!ArcaneTabletAccess.hasTablet(player)) {
            projectexa$transferContext = null;
            return null;
        }
        if (projectexa$transferContext == null || !projectexa$transferContext.isFor(player)) {
            projectexa$transferContext = new JeiArcaneTransferScreen(player);
        }
        return projectexa$transferContext;
    }
}
