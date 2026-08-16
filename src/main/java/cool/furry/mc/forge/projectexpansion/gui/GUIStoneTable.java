package cool.furry.mc.forge.projectexpansion.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerStoneTable;
import cool.furry.mc.forge.projectexpansion.net.PacketHandler;
import cool.furry.mc.forge.projectexpansion.net.packets.to_server.PacketStoneTableAction;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.StoneTableHelper;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GUIStoneTable extends PEContainerScreen<ContainerStoneTable> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/stone_table.png");
    private static final int[][] OUTPUT_POSITIONS = {
            {80, 28}, {110, 38}, {50, 38}, {120, 68},
            {40, 68}, {110, 98}, {50, 98}, {80, 108}
    };

    private EditBox filterBox;
    private Button previous;
    private Button next;
    private Button burn;
    private Button learn;
    private Button unlearn;
    private final List<Button> outputButtons = new ArrayList<>();
    private List<ItemStack> visibleItems = List.of();
    private int page;

    public GUIStoneTable(ContainerStoneTable menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 217;
    }

    @Override
    public void init() {
        super.init();
        filterBox = addWidget(new EditBox(font, leftPos + 8, topPos + 7, 160, 11, Component.empty()));
        filterBox.setResponder(this::updateFilter);

        previous = addButton(leftPos + 7, topPos + 20, 18, 18, button -> changePage(false));
        next = addButton(leftPos + 151, topPos + 20, 18, 18, button -> changePage(true));
        learn = addButton(leftPos + 8, topPos + 115, 18, 18,
                button -> sendAction(ContainerStoneTable.Action.LEARN, ItemStack.EMPTY, false));
        unlearn = addButton(leftPos + 152, topPos + 115, 18, 18,
                button -> sendAction(ContainerStoneTable.Action.UNLEARN, ItemStack.EMPTY, false));
        burn = addButton(leftPos + 80, topPos + 68, 18, 18,
                button -> sendAction(ContainerStoneTable.Action.BURN, ItemStack.EMPTY, Screen.hasShiftDown()));

        outputButtons.clear();
        for (int[] position : OUTPUT_POSITIONS) {
            outputButtons.add(addButton(leftPos + position[0], topPos + position[1], 18, 18,
                    button -> {
                        ItemStack target = targetForButton(button);
                        if (!target.isEmpty()) {
                            sendAction(ContainerStoneTable.Action.EXTRACT, target, Screen.hasShiftDown());
                        }
                    }));
        }
        updateItems();
    }

    private Button addButton(int x, int y, int width, int height, Button.OnPress press) {
        Button button = addWidget(Button.builder(Component.empty(), press).pos(x, y).size(width, height).build());
        button.setAlpha(0.0F);
        return button;
    }

    private ItemStack targetForButton(Button button) {
        int index = outputButtons.indexOf(button);
        int targetIndex = page * OUTPUT_POSITIONS.length + index;
        return targetIndex >= 0 && targetIndex < visibleItems.size() ? visibleItems.get(targetIndex) : ItemStack.EMPTY;
    }

    private void sendAction(ContainerStoneTable.Action action, ItemStack target, boolean shiftHeld) {
        PacketHandler.sendToServer(new PacketStoneTableAction(action, target.copy(), shiftHeld));
    }

    private void updateFilter(String value) {
        page = 0;
        updateItems();
    }

    private void updateItems() {
        List<ItemStack> known = StoneTableHelper.getKnownItems(menu.getProvider());
        visibleItems = StoneTableHelper.filter(known, filterBox == null ? "" : filterBox.getValue());
        int pageCount = pageCount();
        if (pageCount == 0) {
            page = 0;
        } else if (page >= pageCount) {
            page = pageCount - 1;
        }

        if (previous != null) previous.active = page > 0;
        if (next != null) next.active = page + 1 < pageCount;
        for (int i = 0; i < outputButtons.size(); i++) {
            outputButtons.get(i).active = page * OUTPUT_POSITIONS.length + i < visibleItems.size();
        }
    }

    private int pageCount() {
        return (visibleItems.size() + OUTPUT_POSITIONS.length - 1) / OUTPUT_POSITIONS.length;
    }

    private void changePage(boolean forward) {
        if (forward && page + 1 < pageCount()) {
            page++;
        } else if (!forward && page > 0) {
            page--;
        }
        updateItems();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateItems();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String filter = filterBox == null ? "" : filterBox.getValue();
        init(minecraft, width, height);
        if (filterBox != null) filterBox.setValue(filter);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (scrollY < 0) changePage(true);
        else if (scrollY > 0) changePage(false);
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderOutputHover(graphics, mouseX, mouseY);
        if (filterBox != null) filterBox.render(graphics, mouseX, mouseY, partialTick);

        renderIfHovered(graphics, mouseX, mouseY, previous, 196, 0);
        renderIfHovered(graphics, mouseX, mouseY, next, 215, 0);
        renderIfHovered(graphics, mouseX, mouseY, learn, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, unlearn, 234, 0);
        renderIfHovered(graphics, mouseX, mouseY, burn, 234, 0);
        for (Button button : outputButtons) {
            renderIfHovered(graphics, mouseX, mouseY, button, 234, 0);
        }
    }

    private void renderIfHovered(GuiGraphics graphics, int mouseX, int mouseY, @Nullable Button button, int u, int v) {
        if (button != null && button.isMouseOver(mouseX, mouseY)) {
            graphics.blit(TEXTURE, button.getX(), button.getY(), u, v, button.getWidth(), button.getHeight(), 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        BigInteger emc = menu.getProvider().getEmc();
        String emcText = EMCFormat.format(emc);
        graphics.drawString(font, emcText, (imageWidth - font.width(emcText)) / 2, -9, 0xFFB5B5B5);

        PoseStack pose = graphics.pose();
        int first = page * OUTPUT_POSITIONS.length;
        for (int i = 0; i < OUTPUT_POSITIONS.length; i++) {
            int itemIndex = first + i;
            if (itemIndex >= visibleItems.size()) continue;
            ItemStack stack = visibleItems.get(itemIndex);
            int x = OUTPUT_POSITIONS[i][0];
            int y = OUTPUT_POSITIONS[i][1];
            graphics.renderItem(stack, x, y);

            long value = IEMCProxy.INSTANCE.getValue(stack);
            if (value <= 0) continue;
            BigInteger count = emc.divide(BigInteger.valueOf(value));
            String countText = EMCFormat.formatForceShort(count);
            pose.pushPose();
            pose.translate(OUTPUT_POSITIONS[i][0] + 17, OUTPUT_POSITIONS[i][1] + 12, 200F);
            pose.scale(0.5F, 0.5F, 1F);
            graphics.drawString(font, countText, -font.width(countText), 0, 0xFFFFFF, true);
            pose.popPose();
        }
    }

    private void renderOutputHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (targetAtScreen(mouseX, mouseY).isEmpty()) {
            return;
        }
        Rect2i area = outputAreaAtScreen(mouseX, mouseY);
        if (area != null) {
            graphics.fill(area.getX(), area.getY(), area.getX() + 16, area.getY() + 16, 0x80FFFFFF);
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
        } else if (filterBox != null && filterBox.isFocused()
                && (hoveredSlot == null || (!hoveredSlot.hasItem() && menu.getCarried().isEmpty()))) {
            filterBox.setFocused(false);
        }
        return super.mouseClicked(x, y, mouseButton);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack target = targetAt(mouseX - leftPos, mouseY - topPos);
        if (!target.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : getTooltipFromItem(target)) {
                lines.addAll(font.split(line, 400));
            }
            setTooltipForNextRenderPass(lines);
            return;
        }

        if (inside(mouseX, mouseY, leftPos + 8, topPos + 115)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.stone_table.learn"));
        } else if (inside(mouseX, mouseY, leftPos + 152, topPos + 115)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.stone_table.unlearn"));
        } else if (inside(mouseX, mouseY, leftPos + 80, topPos + 68)) {
            MutableComponent text = Component.translatable("gui.projectexa.stone_table.burn");
            if (Screen.hasShiftDown()) {
                text.append(Component.literal(" "))
                        .append(Component.translatable("gui.projectexa.stone_table.burn_emc_holder").withStyle(ChatFormatting.GRAY));
            }
            setTooltipForNextRenderPass(text);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    public List<Component> getTooltipFromItem(ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromItem(Minecraft.getInstance(), stack));
        if (!StoneTableHelper.isWhitelisted(stack) && IEMCProxy.INSTANCE.hasValue(stack)) {
            tooltip.add(Component.translatable("gui.projectexa.stone_table.cant_use").withStyle(ChatFormatting.RED));
        }
        return tooltip;
    }

    public ItemStack targetAt(double x, double y) {
        int buttonIndex = outputIndexAt(x, y);
        if (buttonIndex >= 0) {
            int itemIndex = page * OUTPUT_POSITIONS.length + buttonIndex;
            return itemIndex < visibleItems.size() ? visibleItems.get(itemIndex) : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack targetAtScreen(double x, double y) {
        return targetAt(x - leftPos, y - topPos);
    }

    @Nullable
    public Rect2i outputAreaAtScreen(double x, double y) {
        int buttonIndex = outputIndexAt(x - leftPos, y - topPos);
        if (buttonIndex < 0) {
            return null;
        }
        int itemIndex = page * OUTPUT_POSITIONS.length + buttonIndex;
        if (itemIndex >= visibleItems.size()) {
            return null;
        }
        int[] position = OUTPUT_POSITIONS[buttonIndex];
        return new Rect2i(leftPos + position[0], topPos + position[1], 18, 18);
    }

    private static int outputIndexAt(double x, double y) {
        for (int i = 0; i < OUTPUT_POSITIONS.length; i++) {
            int buttonX = OUTPUT_POSITIONS[i][0];
            int buttonY = OUTPUT_POSITIONS[i][1];
            if (x >= buttonX && x < buttonX + 18 && y >= buttonY && y < buttonY + 18) {
                return i;
            }
        }
        return -1;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }
}
