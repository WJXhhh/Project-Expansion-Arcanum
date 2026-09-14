package com.wjx.forge.projectexa.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A small JEI-looking button with the ProjectExA transfer icon. */
public class JeiStyleButton extends Button {
    private static final ResourceLocation ENABLED_TEXTURE =
            new ResourceLocation("jei", "textures/jei/atlas/gui/button_enabled.png");
    private static final ResourceLocation HIGHLIGHT_TEXTURE =
            new ResourceLocation("jei", "textures/jei/atlas/gui/button_highlight.png");
    private static final ResourceLocation DISABLED_TEXTURE =
            new ResourceLocation("jei", "textures/jei/atlas/gui/button_disabled.png");
    private static final ResourceLocation TRANSFER_ICON =
            new ResourceLocation("projectexa", "textures/gui/arcanum_transfer.png");

    public JeiStyleButton(int x, int y, Component narration, OnPress onPress) {
        super(Button.builder(narration, onPress).pos(x, y).size(20, 20));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = !active
                ? DISABLED_TEXTURE
                : isHoveredOrFocused() ? HIGHLIGHT_TEXTURE : ENABLED_TEXTURE;
        graphics.blit(background, getX(), getY(), 0, 0, getWidth(), getHeight(), 20, 20);
        graphics.blit(TRANSFER_ICON, getX() + 3, getY() + 3, 0, 0, 14, 14, 14, 14);
    }
}
