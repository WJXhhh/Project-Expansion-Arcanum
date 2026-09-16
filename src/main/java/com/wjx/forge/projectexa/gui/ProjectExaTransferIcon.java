package com.wjx.forge.projectexa.gui;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The 14x14 ProjectExA icon used by JEI transfer buttons. */
public final class ProjectExaTransferIcon implements IDrawable {
    public static final ProjectExaTransferIcon INSTANCE = new ProjectExaTransferIcon();

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("projectexa", "textures/gui/arcanum_transfer.png");
    private static final int SIZE = 14;
    private static final int DRAW_SIZE = 12;

    private ProjectExaTransferIcon() {
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y) {
        // Leave a pixel of padding inside JEI's 14x14 icon area. The source art
        // reaches close to all four edges and JEI's button does not clip drawables.
        graphics.blit(TEXTURE, x + 1, y + 1, DRAW_SIZE, DRAW_SIZE,
                0, 0, SIZE, SIZE, SIZE, SIZE);
    }
}
