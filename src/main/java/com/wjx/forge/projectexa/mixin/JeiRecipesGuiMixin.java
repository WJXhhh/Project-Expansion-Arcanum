package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wjx.forge.projectexa.integrations.jei.JeiArcaneTransferScreen;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/** Keeps arcane transfer available when JEI was opened from e.g. FTB Quests. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipesGui", remap = false)
public class JeiRecipesGuiMixin {
    @Unique
    private @Nullable JeiArcaneTransferScreen projectexa$transferContext;

    // Newer JEI jars can reuse slot 0 (this) before these methods return.
    // Wrap the complete methods so our receiver remains valid outside their
    // optimized bodies, and keep real parent containers ahead of our fallback.
    @WrapMethod(method = "getParentContainerMenu()Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            remap = false)
    private @Nullable AbstractContainerMenu projectexa$provideTransferMenu(
            Operation<AbstractContainerMenu> original) {
        AbstractContainerMenu menu = original.call();
        if (menu != null) {
            return menu;
        }
        JeiArcaneTransferScreen context = projectexa$getTransferContext();
        return context == null ? null : context.getMenu();
    }

    // Older JEI versions do not expose a parent screen accessor.
    @WrapMethod(method = "getParentContainerScreen()"
            + "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;",
            remap = false, require = 0)
    private @Nullable AbstractContainerScreen<?> projectexa$provideTransferScreen(
            Operation<AbstractContainerScreen<?>> original) {
        AbstractContainerScreen<?> screen = original.call();
        return screen != null ? screen : projectexa$getTransferContext();
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
