package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.integrations.jei.JeiArcaneTransferScreen;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import net.minecraft.client.Minecraft;
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

    // JEI 19.21 requests a menu for transfer, even when opened from a non-container screen.
    @Inject(method = "getParentContainerMenu", at = @At("RETURN"), cancellable = true, remap = false)
    private void projectexa$provideTransferMenu(CallbackInfoReturnable<AbstractContainerMenu> callback) {
        if (callback.getReturnValue() == null) {
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
