package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.RSIItemRetrievalInput;
import com.huanghuang.rsintegration.api.RSIItemRetrievalInput.Context;
import com.huanghuang.rsintegration.api.RSIItemRetrievalInput.Decision;
import com.mojang.blaze3d.platform.InputConstants;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.integrations.jei.JeiEmcInput;
import com.wjx.forge.projectexa.util.WorldEmcPickClient;
import org.lwjgl.glfw.GLFW;

public final class RsiRetrievalInput {
    private RsiRetrievalInput() {}

    public static void register() {
        RSIItemRetrievalInput.register(Main.rl("emc_input"), 0, RsiRetrievalInput::onInput);
    }

    private static Decision onInput(Context context) {
        if (context.origin() instanceof RSIItemRetrievalInput.WorldPick world) {
            return WorldEmcPickClient.requestPick(world.hit(), context.target(), context.modifiers().shift())
                    ? Decision.HANDLED : Decision.PASS;
        }
        if (context.origin() instanceof RSIItemRetrievalInput.Jei jei
                && context.input().getType() == InputConstants.Type.MOUSE
                && context.input().getValue() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && !context.modifiers().control()
                && JeiEmcInput.tryRsiMiddleClick(jei.screen(), context.target())) {
            return Decision.HANDLED;
        }
        return Decision.PASS;
    }
}
