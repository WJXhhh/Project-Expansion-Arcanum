package com.wjx.forge.projectexa.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.gui.container.slots.PXOutputSlot;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import com.wjx.forge.projectexa.util.EMCFormat;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.Locale;

public class GUIArcaneTransmutationTablet extends PEContainerScreen<ContainerArcaneTransmutationTablet> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/arcane_transmutation_tablet.png");
    private final TransmutationInventory inventory;
    private EditBox filterBox;
    private Button previous;
    private Button next;
    private Button rotate;
    private Button balance;
    private Button clear;

    public GUIArcaneTransmutationTablet(ContainerArcaneTransmutationTablet container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.inventory = container.transmutationInventory;
        this.imageWidth = 176;
        this.imageHeight = 217;
    }

    @Override
    public void init() {
        super.init();
        filterBox = addWidget(new EditBox(font, leftPos + 7, topPos + 6, 162, 12, Component.empty()));
        filterBox.setValue(inventory.filter);
        filterBox.setResponder(this::updateFilter);

        previous = addWidget(Button.builder(Component.empty(), button -> previousPage())
                .pos(leftPos + 7, topPos + 20).size(18, 18).build());
        next = addWidget(Button.builder(Component.empty(), button -> nextPage())
                .pos(leftPos + 151, topPos + 20).size(18, 18).build());
        rotate = addWidget(Button.builder(Component.empty(), button -> menu.action(Screen.hasShiftDown()
                        ? PacketArcaneTransmutationTabletSmallButton.Action.ROTATE_COUNTER_CLOCKWISE
                        : PacketArcaneTransmutationTabletSmallButton.Action.ROTATE))
                .pos(leftPos - 71, topPos + 16).size(9, 9).build());
        balance = addWidget(Button.builder(Component.empty(), button -> menu.action(Screen.hasShiftDown()
                        ? PacketArcaneTransmutationTabletSmallButton.Action.SPREAD
                        : PacketArcaneTransmutationTabletSmallButton.Action.BALANCE))
                .pos(leftPos - 71, topPos + 26).size(9, 9).build());
        clear = addWidget(Button.builder(Component.empty(), button -> menu.action(Screen.hasShiftDown()
                        ? PacketArcaneTransmutationTabletSmallButton.Action.CLEAR_FORCE
                        : PacketArcaneTransmutationTabletSmallButton.Action.CLEAR))
                .pos(leftPos - 71, topPos + 61).size(9, 9).build());
        updateButtons();
    }

    private void updateFilter(String text) {
        String filter = text.toLowerCase(Locale.ROOT);
        if (!inventory.filter.equals(filter)) {
            inventory.filter = filter;
            inventory.searchpage = 0;
            inventory.updateClientTargets();
        }
    }

    private boolean hasPreviousPage() {
        return inventory.searchpage > 0;
    }

    private boolean hasNextPage() {
        return inventory.getKnowledgeSize() > 0;
    }

    private void previousPage() {
        if (hasPreviousPage()) {
            inventory.searchpage--;
            inventory.updateClientTargets();
            updateButtons();
        }
    }

    private void nextPage() {
        if (hasNextPage()) {
            inventory.searchpage++;
            inventory.updateClientTargets();
            updateButtons();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY < 0 && hasNextPage()) nextPage();
        else if (scrollY > 0 && hasPreviousPage()) previousPage();
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String filter = filterBox == null ? "" : filterBox.getValue();
        init(minecraft, width, height);
        if (filterBox != null) filterBox.setValue(filter);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        if (previous != null) previous.active = hasPreviousPage();
        if (next != null) next.active = hasNextPage();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(TEXTURE, leftPos - 75, topPos + 10, 180, 32, 76, 89);
        if (filterBox != null) filterBox.render(graphics, mouseX, mouseY, partialTick);

        renderIfHovered(graphics, mouseX, mouseY, previous, 196, 0);
        renderIfHovered(graphics, mouseX, mouseY, next, 215, 0);
        renderIfHovered(graphics, mouseX, mouseY, rotate, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, balance, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, clear, 234, 0);
        if (menu.isCrafting) graphics.blit(TEXTURE, leftPos - 50, topPos + 76, 177, 19, 18, 12);
        if (Screen.hasShiftDown()) renderIfHovered(graphics, mouseX, mouseY, clear, 234, 10);
    }

    private void renderIfHovered(GuiGraphics graphics, int mouseX, int mouseY, Button button, int u, int v) {
        if (button != null && button.isMouseOver(mouseX, mouseY)) {
            graphics.blit(TEXTURE, button.getX(), button.getY(), u, v, button.getWidth(), button.getHeight());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BigInteger emc = inventory.getAvailableEmc();
        String emcText = EMCFormat.format(emc);
        graphics.drawString(font, emcText, (imageWidth - font.width(emcText)) / 2, -9, 0xFFB5B5B5);

        if (inventory.learnFlag > 0) {
            Component learned = Component.translatable("gui.projectexa.arcane_transmutation_tablet.learned");
            graphics.drawString(font, learned, 170 - font.width(learned), 60, 0xFFB5B5B5, false);
            inventory.learnFlag--;
        }
        if (inventory.unlearnFlag > 0) {
            Component unlearned = Component.translatable("gui.projectexa.arcane_transmutation_tablet.unlearned");
            graphics.drawString(font, unlearned, 6, 60, 0xFFB5B5B5, false);
            inventory.unlearnFlag--;
        }

        PoseStack pose = graphics.pose();
        for (PXOutputSlot slot : menu.getOutputSlots()) {
            long value = IEMCProxy.INSTANCE.getValue(slot.getItem());
            if (value <= 0) continue;
            BigInteger count = emc.equals(BigInteger.ZERO) ? BigInteger.ZERO : emc.divide(BigInteger.valueOf(value));
            String countText = EMCFormat.formatForceShort(count);
            pose.pushPose();
            // Keep this above item rendering while below the tooltip layer (z=400).
            pose.translate(slot.x + 17, slot.y + 12, 300F);
            pose.scale(0.5F, 0.5F, 1F);
            graphics.drawString(font, countText, -font.width(countText), 0, 0xFFFFFF, true);
            pose.popPose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (filterBox != null && filterBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                filterBox.setFocused(false);
                return true;
            }
            return filterBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double x, double y, int mouseButton) {
        if (filterBox != null && filterBox.isMouseOver(x, y)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) filterBox.setValue("");
        } else if (filterBox != null && filterBox.isFocused()) {
            if (hoveredSlot == null || (!hoveredSlot.hasItem() && menu.getCarried().isEmpty())) filterBox.setFocused(false);
        }
        return super.mouseClicked(x, y, mouseButton);
    }

    @Override
    public void removed() {
        super.removed();
        inventory.learnFlag = 0;
        inventory.unlearnFlag = 0;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int lockX = leftPos + menu.lockX;
        int lockY = topPos + menu.lockY;
        int consumeX = leftPos + menu.consumeX;
        int consumeY = topPos + menu.consumeY;
        int unlearnX = leftPos + menu.unlearnX;
        int unlearnY = topPos + menu.unlearnY;
        if (inside(mouseX, mouseY, lockX, lockY)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.arcane_transmutation_tablet.lock"));
        } else if (inside(mouseX, mouseY, consumeX, consumeY)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.arcane_transmutation_tablet.consume"));
        } else if (inside(mouseX, mouseY, unlearnX, unlearnY)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.arcane_transmutation_tablet.unlearn"));
        } else if (rotate != null && rotate.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.arcane_transmutation_tablet.rotate")
                    .append(Component.literal(": "))
                    .append(Component.translatable(Screen.hasShiftDown()
                            ? "gui.projectexa.arcane_transmutation_tablet.counter_clockwise"
                            : "gui.projectexa.arcane_transmutation_tablet.clockwise").withStyle(ChatFormatting.GRAY)));
        } else if (balance != null && balance.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Component.translatable(Screen.hasShiftDown()
                    ? "gui.projectexa.arcane_transmutation_tablet.spread"
                    : "gui.projectexa.arcane_transmutation_tablet.balance"));
        } else if (clear != null && clear.isMouseOver(mouseX, mouseY)) {
            MutableComponent clearText = Component.translatable("gui.projectexa.arcane_transmutation_tablet.clear");
            if (Screen.hasShiftDown()) clearText.append(Component.literal(" ")).append(
                    Component.translatable("gui.projectexa.arcane_transmutation_tablet.clear_force").withStyle(ChatFormatting.RED));
            setTooltipForNextRenderPass(clearText);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
