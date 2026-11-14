package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.gui.GUIArcaneTransmutationTablet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Move vanilla-rendered item stacks with this screen's sliding side panel. */
@Mixin(AbstractContainerScreen.class)
public abstract class ArcaneStationSlotAnimationMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void projectexa$beginSideSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (slot.x < 0 && (Object) this instanceof GUIArcaneTransmutationTablet screen) {
            screen.beginStationPanelRender(graphics);
        }
    }

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void projectexa$endSideSlot(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        if (slot.x < 0 && (Object) this instanceof GUIArcaneTransmutationTablet screen) {
            screen.endStationPanelRender(graphics);
        }
    }
}
