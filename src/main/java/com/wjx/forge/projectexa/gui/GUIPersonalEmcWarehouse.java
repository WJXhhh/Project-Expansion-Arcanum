package com.wjx.forge.projectexa.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wjx.forge.projectexa.gui.container.ContainerPersonalEmcWarehouse;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIPersonalEmcWarehouse extends AbstractContainerScreen<ContainerPersonalEmcWarehouse> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final int rows;

    public GUIPersonalEmcWarehouse(ContainerPersonalEmcWarehouse menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        rows = menu.getFilterRows();
        imageHeight = 114 + rows * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, rows * 18 + 17);
        graphics.blit(TEXTURE, x, y + rows * 18 + 17, 0, 126, imageWidth, 96);
    }
}
