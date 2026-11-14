package com.wjx.forge.projectexa.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.gui.animation.StationPanelAnimation;
import com.wjx.forge.projectexa.gui.container.slots.PXOutputSlot;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneAnvilName;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.util.EMCFormat;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.Locale;
import java.util.List;

import static com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet.*;

@IPNIgnore
public class GUIArcaneTransmutationTablet extends PEContainerScreen<ContainerArcaneTransmutationTablet> {
    private static final ResourceLocation TEXTURE = Main.rl("textures/gui/arcane_transmutation_tablet.png");
    private static final int SMITHING_PREVIEW_X = 110;
    private static final int SMITHING_PREVIEW_Y = 80;
    private static final int SMITHING_PREVIEW_SCALE = 25;
    private static final List<ResourceLocation> SMITHING_TEMPLATE_ICONS = List.of(
            new ResourceLocation("item/empty_slot_smithing_template_armor_trim"),
            new ResourceLocation("item/empty_slot_smithing_template_netherite_upgrade"));
    private final CyclingSlotBackground smithingTemplateIcon = new CyclingSlotBackground(SMITHING);
    private final CyclingSlotBackground smithingBaseIcon = new CyclingSlotBackground(SMITHING + 1);
    private final CyclingSlotBackground smithingAdditionIcon = new CyclingSlotBackground(SMITHING + 2);
    private ArmorStand smithingArmorStand;
    private ItemStack lastSmithingResult = ItemStack.EMPTY;
    private final TransmutationInventory inventory;
    private EditBox filterBox;
    private EditBox anvilName;
    private ItemStack lastAnvilInput = ItemStack.EMPTY;
    private int stoneScroll;
    private boolean draggingStoneScroll;
    private Button previous;
    private Button next;
    private Button rotate;
    private Button balance;
    private Button clear;
    private final Button[] stationButtons = new Button[4];
    private int selectedStation = 0;
    private boolean craftingPanelVisible = true;
    private final StationPanelAnimation stationAnimation = new StationPanelAnimation(0, Util.getMillis());

    public GUIArcaneTransmutationTablet(ContainerArcaneTransmutationTablet container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.inventory = container.transmutationInventory;
        this.imageWidth = 176;
        this.imageHeight = 217;
    }

    @Override
    public void init() {
        super.init();
        boolean finishPendingSelection = selectedStation != stationAnimation.target();
        selectedStation = stationAnimation.target();
        if (selectedStation > 0 && !menu.canUseStation(selectedStation)) {
            selectedStation = 0;
            finishPendingSelection = true;
        }
        craftingPanelVisible = selectedStation == 0;
        stationAnimation.reset(selectedStation, Util.getMillis());
        // Center the main screen together with the enlarged side panel.
        // At the minimum GUI width the station tabs move above the panel.
        int sideWidth = Math.min(164, width - imageWidth);
        leftPos = (width - imageWidth + sideWidth) / 2;
        smithingArmorStand = new ArmorStand(minecraft.level, 0.0D, 0.0D, 0.0D);
        smithingArmorStand.setNoBasePlate(true);
        smithingArmorStand.setShowArms(true);
        smithingArmorStand.yBodyRot = 210.0F;
        smithingArmorStand.setXRot(25.0F);
        smithingArmorStand.yHeadRot = smithingArmorStand.getYRot();
        smithingArmorStand.yHeadRotO = smithingArmorStand.getYRot();
        lastSmithingResult = ItemStack.EMPTY;
        menu.selectStation(selectedStation);
        if (finishPendingSelection) menu.action(stationAction(selectedStation));
        filterBox = addWidget(new EditBox(font, leftPos + 7, topPos + 6, 162, 12, Component.empty()));
        filterBox.setValue(inventory.filter);
        filterBox.setResponder(this::updateFilter);
        anvilName = addWidget(new EditBox(font, leftPos + STATION_PANEL_X + 12,
                topPos + STATION_PANEL_Y + 29, 116, 12, Component.empty()));
        anvilName.setMaxLength(50);
        anvilName.setBordered(false);
        anvilName.setResponder(name -> {
            menu.getAnvil().setItemName(name);
            PacketHandler.sendToServer(new PacketArcaneAnvilName(name));
        });
        anvilName.visible = selectedStation == 3;
        lastAnvilInput = ItemStack.EMPTY;

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
        rotate.visible = craftingPanelVisible;
        balance.visible = craftingPanelVisible;
        clear.visible = craftingPanelVisible;
        for (int i = 0; i < stationButtons.length; i++) {
            final int station = i;
            stationButtons[i] = addWidget(Button.builder(Component.empty(), button -> selectStation(station))
                    .pos(leftPos - 95, topPos + 10 + i * 20).size(18, 18).build());
        }
        updateButtons();
        positionStationButtons();
        if (selectedStation == 1) tickSmithingSlotIcons();
    }

    private void positionStationButtons() {
        boolean horizontal = selectedStation >= 0 && width < imageWidth + 164;
        float openness = stationAnimation.openness();
        if (horizontal) openness = StationPanelAnimation.ease(Math.min(1, openness * 2));
        // Spread horizontally before forming a row, so the square tabs never
        // overlap while changing between the vertical dock and horizontal bar.
        float xProgress = horizontal ? StationPanelAnimation.ease(Math.min(1, openness * 2)) : openness;
        float yProgress = horizontal ? StationPanelAnimation.ease(Math.max(0, openness * 2 - 1)) : openness;
        int visibleIndex = 0;
        for (int i = 0; i < stationButtons.length; i++) {
            if (!stationButtons[i].visible) continue;
            int index = visibleIndex++;
            int expandedX = horizontal ? STATION_PANEL_X + index * 20 : stationPanelDimension(-95, STATION_PANEL_X - 20);
            int expandedY = horizontal ? -10 : 10 + index * 20;
            stationButtons[i].setX(leftPos + Math.round(-20 + (expandedX + 20) * xProgress));
            stationButtons[i].setY(topPos + Math.round(10 + index * 20 + (expandedY - 10 - index * 20) * yProgress));
        }
    }

    private void selectStation(int station) {
        if (!menu.canUseStation(station)) return;
        stationAnimation.select(station, Util.getMillis());
        anvilName.setFocused(false);
        draggingStoneScroll = false;
        hoveredSlot = null;
        updateStationAnimation();
    }

    private void applyStation(int station) {
        selectedStation = station;
        craftingPanelVisible = selectedStation == 0;
        draggingStoneScroll = false;
        menu.selectStation(selectedStation);
        menu.action(stationAction(selectedStation));
        anvilName.visible = selectedStation == 3;
        if (selectedStation == 3) PacketHandler.sendToServer(new PacketArcaneAnvilName(anvilName.getValue()));
        if (selectedStation != 3) anvilName.setFocused(false);
        rotate.visible = craftingPanelVisible;
        balance.visible = craftingPanelVisible;
        clear.visible = craftingPanelVisible;
        positionStationButtons();
        if (selectedStation == 1) tickSmithingSlotIcons();
    }

    private static PacketArcaneTransmutationTabletSmallButton.Action stationAction(int station) {
        return switch (station) {
            case -1 -> PacketArcaneTransmutationTabletSmallButton.Action.STATION_COLLAPSE;
            case 1 -> PacketArcaneTransmutationTabletSmallButton.Action.STATION_SMITHING;
            case 2 -> PacketArcaneTransmutationTabletSmallButton.Action.STATION_STONECUTTING;
            case 3 -> PacketArcaneTransmutationTabletSmallButton.Action.STATION_ANVIL;
            default -> PacketArcaneTransmutationTabletSmallButton.Action.STATION_CRAFTING;
        };
    }

    private void updateStationAnimation() {
        int target = stationAnimation.target();
        if (target > 0 && !menu.canUseStation(target)) {
            stationAnimation.select(0, Util.getMillis());
            anvilName.setFocused(false);
            draggingStoneScroll = false;
            hoveredSlot = null;
        }
        stationAnimation.update(Util.getMillis());
        if (selectedStation != stationAnimation.station()) applyStation(stationAnimation.station());
        boolean interactive = !stationAnimation.isMoving();
        rotate.active = balance.active = clear.active = interactive;
        anvilName.setEditable(interactive);
        updateStationAvailability();
        positionStationButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateStationAnimation();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int stationPanelDimension(int crafting, int other) {
        return Math.round(other + (crafting - other) * stationAnimation.craftingWeight());
    }
    private int stationPanelWidth() { return stationPanelDimension(76, STATION_PANEL_WIDTH); }
    private int stationPanelHeight() { return stationPanelDimension(89, STATION_PANEL_HEIGHT); }
    private int stationPanelLeft() { return leftPos + stationPanelDimension(-75, STATION_PANEL_X); }
    private float stationPanelOpenness() {
        float openness = stationAnimation.openness();
        // On narrow screens, finish arranging the tabs before revealing the
        // panel underneath them; closing follows the same path in reverse.
        return width < imageWidth + 164
                ? StationPanelAnimation.ease(Math.max(0, openness * 2 - 1)) : openness;
    }
    private int stationPanelOffset() { return Math.round((1 - stationPanelOpenness()) * stationPanelWidth()); }

    public void beginStationPanelRender(GuiGraphics graphics) {
        beginStationPanelRender(graphics, false);
    }

    private void beginStationPanelRender(GuiGraphics graphics, boolean previous) {
        // Tooltip text can still be queued when Polymorph renders in
        // ScreenEvent.Render.Post. Submit it before restricting the scissor.
        graphics.flush();
        int x = stationPanelLeft(), y = topPos + STATION_PANEL_Y;
        int split = x + Math.round(stationPanelWidth() * stationAnimation.switchProgress());
        boolean switching = stationAnimation.previousStation() >= 0;
        int inset = switching ? 4 : 0;
        int left = previous ? Math.max(split, x + inset) : x + inset;
        int right = previous ? x + stationPanelWidth() - inset : Math.min(split, x + stationPanelWidth() - inset);
        graphics.enableScissor(left, y + inset, Math.max(left, right), y + stationPanelHeight() - inset);
        graphics.pose().pushPose();
        int nativeX = leftPos + (selectedStation == 0 ? -75 : STATION_PANEL_X);
        graphics.pose().translate(x - nativeX + stationPanelOffset(), 0, 0);
    }

    public void endStationPanelRender(GuiGraphics graphics) {
        graphics.flush();
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    public boolean isCraftingPanelVisible() {
        return craftingPanelVisible;
    }

    public boolean isStationPanelInteractive() { return !stationAnimation.isMoving(); }

    public List<Rect2i> getStationExtraAreas() {
        int minX = width, minY = height, maxX = 0, maxY = 0;
        for (Button button : stationButtons) {
            if (!button.visible) continue;
            minX = Math.min(minX, button.getX()); minY = Math.min(minY, button.getY());
            maxX = Math.max(maxX, button.getX() + 18); maxY = Math.max(maxY, button.getY() + 18);
        }
        Rect2i tabs = new Rect2i(minX, minY, maxX - minX, maxY - minY);
        int offset = stationPanelOffset();
        if (selectedStation < 0 || offset >= stationPanelWidth()) return List.of(tabs);
        Rect2i panel = new Rect2i(stationPanelLeft() + offset, topPos + STATION_PANEL_Y,
                stationPanelWidth() - offset, stationPanelHeight());
        return List.of(panel, tabs);
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
        if (stationAnimation.isMoving() && mouseX < leftPos) return true;
        if (selectedStation == 2 && mouseX >= leftPos + STATION_PANEL_X && mouseX < leftPos - 4
                && mouseY >= topPos + STATION_PANEL_Y && mouseY < topPos + STATION_PANEL_Y + STATION_PANEL_HEIGHT) {
            int rows = (menu.getStonecutter().getNumRecipes() + STONE_COLUMNS - 1) / STONE_COLUMNS;
            stoneScroll = Math.max(0, Math.min(Math.max(0, rows - STONE_ROWS), stoneScroll + (scrollY < 0 ? 1 : -1)));
            return true;
        }
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
        if (selectedStation == 1) tickSmithingSlotIcons();
        ItemStack input = menu.getAnvil().getSlot(0).getItem();
        if (!ItemStack.isSameItemSameTags(input, lastAnvilInput)) {
            lastAnvilInput = input.copy();
            anvilName.setValue(input.isEmpty() ? "" : input.getHoverName().getString());
        }
    }

    private void updateButtons() {
        if (previous != null) previous.active = hasPreviousPage();
        if (next != null) next.active = hasNextPage();
        updateStationAvailability();
    }

    private void updateStationAvailability() {
        for (int i = 0; i < stationButtons.length; i++) {
            if (stationButtons[i] != null) {
                boolean unlocked = menu.canUseStation(i);
                stationButtons[i].active = unlocked;
                stationButtons[i].visible = unlocked;
            }
        }
    }

    private void tickSmithingSlotIcons() {
        ItemStack stack = menu.getSlot(SMITHING).getItem();
        SmithingTemplateItem template = stack.getItem() instanceof SmithingTemplateItem item ? item : null;
        smithingTemplateIcon.tick(SMITHING_TEMPLATE_ICONS);
        smithingBaseIcon.tick(template == null ? List.of() : template.getBaseSlotEmptyIcons());
        smithingAdditionIcon.tick(template == null ? List.of() : template.getAdditionalSlotEmptyIcons());
    }

    private void renderSmithingSlotIcons(GuiGraphics graphics, float partialTick) {
        smithingTemplateIcon.render(menu, graphics, partialTick, leftPos, topPos);
        smithingBaseIcon.render(menu, graphics, partialTick, leftPos, topPos);
        smithingAdditionIcon.render(menu, graphics, partialTick, leftPos, topPos);
    }

    private void renderSmithingArmorStand(GuiGraphics graphics) {
        if (smithingArmorStand == null) return;
        ItemStack result = menu.getSlot(SMITHING + 3).getItem();
        if (!ItemStack.isSameItemSameTags(result, lastSmithingResult)) {
            lastSmithingResult = result.copy();
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                smithingArmorStand.setItemSlot(slot, ItemStack.EMPTY);
            }
            if (!result.isEmpty()) {
                EquipmentSlot slot = result.getItem() instanceof ArmorItem armor
                        ? armor.getEquipmentSlot() : EquipmentSlot.OFFHAND;
                smithingArmorStand.setItemSlot(slot, result.copy());
            }
        }
        InventoryScreen.renderEntityInInventory(graphics,
                leftPos + STATION_PANEL_X + SMITHING_PREVIEW_X,
                topPos + STATION_PANEL_Y + SMITHING_PREVIEW_Y, SMITHING_PREVIEW_SCALE,
                SmithingScreen.ARMOR_STAND_ANGLE, null, smithingArmorStand);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (selectedStation >= 0) {
            if (stationAnimation.previousStation() >= 0) drawStationTransitionFrame(graphics);
            renderPreviousStation(graphics, partialTick, mouseX, mouseY);
            beginStationPanelRender(graphics);
            renderStationContents(graphics, partialTick, mouseX, mouseY);
            endStationPanelRender(graphics);
        }
        for (int i = 0; i < stationButtons.length; i++) {
            Button button = stationButtons[i];
            if (!button.visible) continue;
            int x = button.getX(), y = button.getY();
            drawStationButton(graphics, i, x, y, button.active,
                    i == stationAnimation.target(), button.isMouseOver(mouseX, mouseY));
        }
        if (filterBox != null) filterBox.render(graphics, mouseX, mouseY, partialTick);
        renderIfHovered(graphics, mouseX, mouseY, previous, 196, 0);
        renderIfHovered(graphics, mouseX, mouseY, next, 215, 0);
    }

    private void drawStationButton(GuiGraphics graphics, int station, int x, int y,
                                   boolean enabled, boolean selected, boolean hovered) {
        if (!enabled) return;
        int border = selected ? 0xFFB0A39C : hovered ? 0xFF8A8A8A : 0xFF515151;
        graphics.fill(x, y, x + 18, y + 18, 0xFF202020);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, border);
        graphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF373737);
        drawStationIcon(graphics, station, x + 3, y + 3);
    }

    private void drawStationTransitionFrame(GuiGraphics graphics) {
        int x = stationPanelLeft() + stationPanelOffset(), y = topPos + STATION_PANEL_Y;
        int right = stationPanelLeft() + stationPanelWidth(), bottom = y + stationPanelHeight();
        if (right - x < 8) return;
        graphics.fill(x, y, right, bottom, 0xFF202020);
        graphics.fill(x + 2, y + 2, right - 2, bottom - 2, 0xFF777777);
        graphics.fill(x + 4, y + 4, right - 4, bottom - 4, 0xFF454545);
    }

    private void renderPreviousStation(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int previous = stationAnimation.previousStation();
        if (previous < 0) return;
        int next = selectedStation;
        boolean nameVisible = anvilName.visible;
        selectedStation = previous;
        craftingPanelVisible = previous == 0;
        anvilName.visible = previous == 3;
        beginStationPanelRender(graphics, true);
        try {
            renderStationContents(graphics, partialTick, mouseX, mouseY);
            graphics.pose().pushPose();
            graphics.pose().translate(leftPos, topPos, 0);
            for (Slot slot : menu.getStationSlots(previous)) {
                ItemStack stack = slot.getItem();
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slot.x, slot.y);
                    graphics.renderItemDecorations(font, stack, slot.x, slot.y);
                }
            }
            graphics.pose().popPose();
        } finally {
            endStationPanelRender(graphics);
            selectedStation = next;
            craftingPanelVisible = next == 0;
            anvilName.visible = nameVisible;
        }
    }

    private void renderStationContents(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (craftingPanelVisible) {
            graphics.blit(TEXTURE, leftPos - 75, topPos + 10, 180, 32, 76, 89);
        } else if (selectedStation >= 0) {
            renderStationPanel(graphics);
            if (selectedStation == 1) {
                renderSmithingSlotIcons(graphics, partialTick);
                renderSmithingArmorStand(graphics);
            }
        }
        if (selectedStation == 3) {
            anvilName.render(graphics, mouseX, mouseY, partialTick);
            int cost = menu.getAnvil().getCost();
            if (cost > 0) {
                Component label = Component.translatable("container.repair.cost", cost);
                graphics.drawString(font, label, leftPos + STATION_PANEL_X + 10,
                        topPos + STATION_PANEL_Y + 99, 0xFF80FF20);
            }
        }

        if (craftingPanelVisible) {
            renderIfHovered(graphics, mouseX, mouseY, rotate, 234, 0);
            renderIfHovered(graphics, mouseX, mouseY, balance, 234, 0);
            renderIfHovered(graphics, mouseX, mouseY, clear, 234, 0);
            if (menu.isCrafting) graphics.blit(TEXTURE, leftPos - 50, topPos + 76, 177, 19, 18, 12);
            if (Screen.hasShiftDown()) renderIfHovered(graphics, mouseX, mouseY, clear, 234, 10);
        }
    }

    private void renderStationPanel(GuiGraphics graphics) {
        int x = leftPos + STATION_PANEL_X, y = topPos + STATION_PANEL_Y;
        graphics.fill(x, y, x + STATION_PANEL_WIDTH, y + STATION_PANEL_HEIGHT, 0xFF202020);
        graphics.fill(x + 2, y + 2, x + STATION_PANEL_WIDTH - 2, y + STATION_PANEL_HEIGHT - 2, 0xFF777777);
        graphics.fill(x + 4, y + 4, x + STATION_PANEL_WIDTH - 4, y + STATION_PANEL_HEIGHT - 4, 0xFF454545);
        String title = switch (selectedStation) {
            case 1 -> "container.upgrade";
            case 2 -> "container.stonecutter";
            default -> "container.repair";
        };
        graphics.drawString(font, Component.translatable(title), x + 10, y + 9, 0xFFCCCCCC, false);
        int slotCount = selectedStation == 1 ? 4 : selectedStation == 2 ? 2 : 3;
        for (int i = 0; i < slotCount; i++) {
            drawSlot(graphics, leftPos + stationSlotX(selectedStation, i) - 1,
                    topPos + stationSlotY(selectedStation, i) - 1);
        }
        if (selectedStation == 1) {
            drawStationArrow(graphics, x + 86, y + 101);
        } else if (selectedStation == 2) {
            int gridX = leftPos + STONE_RECIPE_X, gridY = topPos + STONE_RECIPE_Y;
            graphics.fill(gridX - 2, gridY - 2, gridX + STONE_COLUMNS * STONE_CELL + 2,
                    gridY + STONE_ROWS * STONE_CELL + 2, 0xFF292929);
            var recipes = menu.getStonecutter().getRecipes();
            int maxScroll = Math.max(0, (recipes.size() + STONE_COLUMNS - 1) / STONE_COLUMNS - STONE_ROWS);
            stoneScroll = Math.min(stoneScroll, maxScroll);
            for (int i = 0; i < STONE_COLUMNS * STONE_ROWS; i++) {
                int rx = gridX + (i % STONE_COLUMNS) * STONE_CELL;
                int ry = gridY + (i / STONE_COLUMNS) * STONE_CELL;
                graphics.fill(rx, ry, rx + STONE_CELL, ry + STONE_CELL, 0xFF515151);
                graphics.fill(rx + 1, ry + 1, rx + STONE_CELL - 1, ry + STONE_CELL - 1, 0xFF383838);
                int index = stoneScroll * STONE_COLUMNS + i;
                if (index >= recipes.size()) continue;
                if (index == menu.getStonecutter().getSelectedRecipeIndex())
                    graphics.fill(rx, ry, rx + STONE_CELL, ry + STONE_CELL, 0xFF897D75);
                graphics.renderItem(recipes.get(index).getResultItem(minecraft.level.registryAccess()), rx + 1, ry + 1);
            }
            int trackX = gridX + STONE_COLUMNS * STONE_CELL + 5;
            graphics.fill(trackX, gridY, trackX + 8, gridY + 54, 0xFF202020);
            int thumbY = gridY + (maxScroll == 0 ? 0 : stoneScroll * 36 / maxScroll);
            graphics.fill(trackX + 1, thumbY, trackX + 7, thumbY + 18,
                    maxScroll == 0 ? 0xFF515151 : 0xFFAAAAAA);
            drawStationArrow(graphics, x + 86, y + 103);
        } else {
            graphics.fill(x + 10, y + 23, x + 130, y + 43, 0xFF202020);
            graphics.fill(x + 11, y + 24, x + 129, y + 42, 0xFF303030);
            graphics.fill(x + 42, y + 72, x + 49, y + 73, 0xFFCCCCCC);
            graphics.fill(x + 45, y + 69, x + 46, y + 76, 0xFFCCCCCC);
            drawStationArrow(graphics, x + 88, y + 67);
        }
    }

    private static void drawStationArrow(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y + 4, x + 16, y + 8, 0xFFAAAAAA);
        for (int i = 0; i < 6; i++) {
            graphics.fill(x + 12 + i, y + i, x + 13 + i, y + 12 - i, 0xFFAAAAAA);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF222222);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF383838);
    }

    private void drawStationIcon(GuiGraphics graphics, int station, int x, int y) {
        int light = 0xFFCCCCCC;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        // Draw on a 48x48 canvas inside the existing 12x12 GUI area so details
        // can use the extra screen pixels available at higher GUI scales.
        pose.scale(0.25F, 0.25F, 1F);
        if (station == 0) {
            graphics.fill(2, 2, 46, 5, light);
            graphics.fill(2, 43, 46, 46, light);
            graphics.fill(2, 5, 5, 43, light);
            graphics.fill(43, 5, 46, 43, light);
            for (int divider = 16; divider <= 30; divider += 14) {
                graphics.fill(divider, 5, divider + 3, 43, light);
                graphics.fill(5, divider, 43, divider + 3, light);
            }
        } else if (station == 1) {
            pose.pushPose();
            pose.translate(24, 24, 0);
            // A 45-degree diagonal keeps the pixel steps regular at small GUI scales.
            pose.mulPose(Axis.ZP.rotationDegrees(45));
            pose.translate(-24, -24, 0);
            // Square-ended rectangular hammer head and a straight handle.
            graphics.fill(9, 8, 39, 19, light);
            // Overlap the head slightly to avoid a rasterization seam at the joint.
            graphics.fill(21, 18, 27, 46, light);
            pose.popPose();
        } else if (station == 2) {
            // User-edited 48x48 silhouette, retaining pixels with alpha >= 128.
            // Draw opaque spans to remove translucent edge noise while keeping
            // the hooked teeth and the notch along the bottom of the blade.
            graphics.fill(23, 9, 25, 12, light);
            graphics.fill(16, 11, 19, 13, light);
            graphics.fill(30, 11, 32, 12, light);
            graphics.fill(20, 12, 32, 13, light);
            graphics.fill(16, 13, 32, 14, light);
            graphics.fill(35, 13, 37, 14, light);
            graphics.fill(10, 14, 11, 15, light);
            graphics.fill(15, 14, 37, 15, light);
            graphics.fill(10, 15, 36, 17, light);
            graphics.fill(11, 17, 37, 18, light);
            graphics.fill(10, 18, 41, 19, light);
            graphics.fill(9, 19, 41, 20, light);
            graphics.fill(5, 20, 7, 21, light);
            graphics.fill(8, 20, 40, 21, light);
            graphics.fill(5, 21, 40, 22, light);
            graphics.fill(6, 22, 41, 23, light);
            graphics.fill(6, 23, 44, 25, light);
            graphics.fill(6, 25, 42, 27, light);
            graphics.fill(3, 27, 43, 28, light);
            graphics.fill(3, 28, 22, 29, light);
            graphics.fill(26, 28, 45, 29, light);
            graphics.fill(5, 29, 21, 31, light);
            graphics.fill(27, 29, 43, 31, light);
            graphics.fill(3, 33, 45, 36, light);
            graphics.fill(6, 36, 42, 44, light);
        } else {
            // Flat working face, tapered horn, curved waist and flared foot.
            graphics.fill(15, 9, 44, 18, light);
            graphics.fill(3, 12, 15, 14, light);
            graphics.fill(5, 14, 15, 16, light);
            graphics.fill(8, 16, 15, 18, light);
            graphics.fill(12, 18, 39, 20, light);
            graphics.fill(17, 20, 36, 22, light);
            graphics.fill(20, 22, 33, 25, light);
            graphics.fill(22, 25, 31, 30, light);
            graphics.fill(20, 30, 33, 33, light);
            graphics.fill(16, 33, 37, 35, light);
            graphics.fill(11, 35, 41, 38, light);
            graphics.fill(8, 38, 43, 41, light);
        }
        pose.popPose();
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
        if (anvilName != null && anvilName.visible && anvilName.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { anvilName.setFocused(false); return true; }
            return anvilName.keyPressed(keyCode, scanCode, modifiers);
        }
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
        if (stationAnimation.isMoving() && x < leftPos) {
            int station = hoveredStation((int) x, (int) y);
            if (station >= 0 && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                stationButtons[station].mouseClicked(x, y, mouseButton);
            }
            return true;
        }
        if (selectedStation == 2 && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int rx = (int) x - (leftPos + STONE_RECIPE_X), ry = (int) y - (topPos + STONE_RECIPE_Y);
            if (rx >= STONE_COLUMNS * STONE_CELL + 5 && rx < STONE_COLUMNS * STONE_CELL + 13
                    && ry >= 0 && ry < STONE_ROWS * STONE_CELL) {
                draggingStoneScroll = true;
                scrollStoneRecipes(y);
                return true;
            }
            if (rx >= 0 && rx < STONE_COLUMNS * STONE_CELL && ry >= 0 && ry < STONE_ROWS * STONE_CELL) {
                int index = stoneScroll * STONE_COLUMNS + (ry / STONE_CELL) * STONE_COLUMNS + rx / STONE_CELL;
                if (menu.clickMenuButton(minecraft.player, index)) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
                    return true;
                }
            }
        }
        if (filterBox != null && filterBox.isMouseOver(x, y)) {
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                filterBox.setValue("");
            } else if (!menu.getCarried().isEmpty()) {
                filterBox.setValue(menu.getCarried().getHoverName().getString());
            }
        } else if (filterBox != null && filterBox.isFocused()) {
            if (hoveredSlot == null || (!hoveredSlot.hasItem() && menu.getCarried().isEmpty())) filterBox.setFocused(false);
        }
        return super.mouseClicked(x, y, mouseButton);
    }

    private void scrollStoneRecipes(double mouseY) {
        int rows = (menu.getStonecutter().getNumRecipes() + STONE_COLUMNS - 1) / STONE_COLUMNS;
        int maxScroll = Math.max(0, rows - STONE_ROWS);
        double position = (mouseY - (topPos + STONE_RECIPE_Y) - 9) / 36.0;
        stoneScroll = (int) Math.round(Math.max(0, Math.min(1, position)) * maxScroll);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (stationAnimation.isMoving() && x < leftPos) return true;
        if (selectedStation == 2 && draggingStoneScroll && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            scrollStoneRecipes(y);
            return true;
        }
        return super.mouseDragged(x, y, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        draggingStoneScroll = false;
        if (stationAnimation.isMoving() && x < leftPos) return true;
        return super.mouseReleased(x, y, button);
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        if (x < 0 && stationAnimation.isMoving()) return false;
        return super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot != null && slot.x < 0 && stationAnimation.isMoving()) return;
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        int panelX = guiLeft + (craftingPanelVisible ? -75 : STATION_PANEL_X);
        int panelY = guiTop + STATION_PANEL_Y;
        int panelWidth = craftingPanelVisible ? 76 : STATION_PANEL_WIDTH;
        int panelHeight = craftingPanelVisible ? 89 : STATION_PANEL_HEIGHT;
        if (selectedStation >= 0 && mouseX >= panelX && mouseX < panelX + panelWidth
                && mouseY >= panelY && mouseY < panelY + panelHeight) {
            return false;
        }
        if (hoveredStation((int) mouseX, (int) mouseY) >= 0) return false;
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    @Override
    public void removed() {
        super.removed();
        inventory.learnFlag = 0;
        inventory.unlearnFlag = 0;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (stationAnimation.isMoving() && mouseX < leftPos) return;
        if (selectedStation == 1 && hoveredSlot != null && !hoveredSlot.hasItem()) {
            Component hint = null;
            ItemStack stack = menu.getSlot(SMITHING).getItem();
            if (hoveredSlot.index == SMITHING) {
                hint = Component.translatable("container.upgrade.missing_template_tooltip");
            } else if (stack.getItem() instanceof SmithingTemplateItem template) {
                if (hoveredSlot.index == SMITHING + 1) hint = template.getBaseSlotDescription();
                else if (hoveredSlot.index == SMITHING + 2) hint = template.getAdditionSlotDescription();
            }
            if (hint != null) {
                graphics.renderTooltip(font, font.split(hint, 115), mouseX, mouseY);
                return;
            }
        }
        if (selectedStation == 2) {
            int rx = mouseX - (leftPos + STONE_RECIPE_X), ry = mouseY - (topPos + STONE_RECIPE_Y);
            if (rx >= 0 && rx < STONE_COLUMNS * STONE_CELL && ry >= 0 && ry < STONE_ROWS * STONE_CELL) {
                int index = stoneScroll * STONE_COLUMNS + (ry / STONE_CELL) * STONE_COLUMNS + rx / STONE_CELL;
                var recipes = menu.getStonecutter().getRecipes();
                if (index < recipes.size()) {
                    graphics.renderTooltip(font, recipes.get(index).getResultItem(minecraft.level.registryAccess()), mouseX, mouseY);
                    return;
                }
            }
        }
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
        } else if (hoveredStation(mouseX, mouseY) >= 0) {
            int station = hoveredStation(mouseX, mouseY);
            Component name = new ItemStack(stationItem(station)).getHoverName();
            setTooltipForNextRenderPass(name);
        } else if (craftingPanelVisible && rotate != null && rotate.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Component.translatable("gui.projectexa.arcane_transmutation_tablet.rotate")
                    .append(Component.literal(": "))
                    .append(Component.translatable(Screen.hasShiftDown()
                            ? "gui.projectexa.arcane_transmutation_tablet.counter_clockwise"
                            : "gui.projectexa.arcane_transmutation_tablet.clockwise").withStyle(ChatFormatting.GRAY)));
        } else if (craftingPanelVisible && balance != null && balance.isMouseOver(mouseX, mouseY)) {
            setTooltipForNextRenderPass(Component.translatable(Screen.hasShiftDown()
                    ? "gui.projectexa.arcane_transmutation_tablet.spread"
                    : "gui.projectexa.arcane_transmutation_tablet.balance"));
        } else if (craftingPanelVisible && clear != null && clear.isMouseOver(mouseX, mouseY)) {
            MutableComponent clearText = Component.translatable("gui.projectexa.arcane_transmutation_tablet.clear");
            if (Screen.hasShiftDown()) clearText.append(Component.literal(" ")).append(
                    Component.translatable("gui.projectexa.arcane_transmutation_tablet.clear_force").withStyle(ChatFormatting.RED));
            setTooltipForNextRenderPass(clearText);
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private int hoveredStation(int x, int y) {
        for (int i = 0; i < stationButtons.length; i++) {
            Button button = stationButtons[i];
            if (button != null && button.isMouseOver(x, y)) return i;
        }
        return -1;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
