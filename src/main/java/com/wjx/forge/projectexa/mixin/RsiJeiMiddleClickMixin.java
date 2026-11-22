package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.mods.jei.JeiStoragePullPacket;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wjx.forge.projectexa.integrations.jei.JeiEmcInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.mods.jei.client.JeiCheatShortcuts", remap = false)
public abstract class RsiJeiMiddleClickMixin {
    @Unique private static final ThreadLocal<Boolean> projectexa$middleClick = new ThreadLocal<>();

    @WrapMethod(method = "onMousePressed", remap = false)
    private static void projectexa$scopeMiddleClick(ScreenEvent.MouseButtonPressed.Pre event, Operation<Void> original) {
        Boolean previous = projectexa$middleClick.get();
        projectexa$middleClick.set(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && !Screen.hasControlDown());
        try {
            original.call(event);
        } finally {
            if (previous == null) projectexa$middleClick.remove();
            else projectexa$middleClick.set(previous);
        }
    }

    @WrapOperation(method = "pullFromStorage", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/network/simple/SimpleChannel;sendToServer(Ljava/lang/Object;)V"),
            remap = false)
    private static void projectexa$coordinatePull(SimpleChannel channel, Object message, Operation<Void> original) {
        if (Boolean.TRUE.equals(projectexa$middleClick.get()) && message instanceof JeiStoragePullPacket pull
                && JeiEmcInput.tryRsiMiddleClick(Minecraft.getInstance().screen, pull.stack())) return;
        original.call(channel, message);
    }
}
