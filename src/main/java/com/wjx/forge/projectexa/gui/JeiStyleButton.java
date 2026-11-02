package com.wjx.forge.projectexa.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** An icon-only transfer button independent of JEI's private textures. */
public class JeiStyleButton extends Button {
    private static final ResourceLocation TRANSFER_ICON =
            new ResourceLocation("projectexa", "textures/gui/arcanum_transfer.png");

    public JeiStyleButton(int x, int y, Component narration, OnPress onPress) {
        super(Button.builder(narration, onPress).pos(x, y).size(20, 20));
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        // Icon-only button. Keep the message for screen-reader narration.
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Use Minecraft's button background rather than JEI's private assets,
        // whose resource paths differ between JEI releases.
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TRANSFER_ICON, getX() + 3, getY() + 3, 0, 0, 14, 14, 14, 14);
    }
}
