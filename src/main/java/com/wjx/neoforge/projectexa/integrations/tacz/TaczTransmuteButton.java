package com.wjx.neoforge.projectexa.integrations.tacz;

import com.wjx.neoforge.projectexa.Main;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TaczTransmuteButton extends Button {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation HIGHLIGHT = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
    private static final ResourceLocation ICON = Main.rl("textures/gui/arcanum_transfer.png");

    public TaczTransmuteButton(int x, int y, OnPress onPress) {
        super(Button.builder(Component.translatable("gui.projectexa.tacz.transmute"), onPress).bounds(x, y, 20, 20));
        setTooltip(Tooltip.create(Component.translatable("gui.projectexa.tacz.transmute.tooltip")));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(isHoveredOrFocused() ? HIGHLIGHT : BACKGROUND, getX(), getY(), getWidth(), getHeight());
        graphics.blit(ICON, getX() + 3, getY() + 3, 0, 0, 14, 14, 14, 14);
    }
}
