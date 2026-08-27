package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.gui.JeiStyleButton;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.to_server.PacketTaczTransmutation;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Adds the tablet action to every TACZ gun smith table screen, including custom table variants. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.gui.GunSmithTableScreen", remap = false)
public abstract class TaczGunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    @Shadow(remap = false)
    private GunSmithTableRecipe selectedRecipe;

    @Shadow(remap = false)
    public abstract void updateIngredientCount();

    @Unique
    private Button projectexa$transmuteButton;

    @Unique
    private int projectexa$refreshesRemaining;

    @Unique
    private long projectexa$nextRefreshTick = Long.MIN_VALUE;

    @Unique
    private long projectexa$lastRefreshTick = Long.MIN_VALUE;

    protected TaczGunSmithTableScreenMixin() {
        super(null, null, Component.empty());
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void projectexa$addTransmuteButton(CallbackInfo callbackInfo) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        JeiStyleButton button = new JeiStyleButton(
                screen.getGuiLeft() + 265,
                screen.getGuiTop() + 161,
                Component.translatable("gui.projectexa.tacz.transmute"),
                this::projectexa$transmute);
        button.setTooltip(Tooltip.create(Component.translatable("gui.projectexa.tacz.transmute.tooltip")));
        projectexa$transmuteButton = addRenderableWidget(button);
        projectexa$updateTransmuteButton();
    }

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void projectexa$updateButtonVisibility(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        projectexa$updateTransmuteButton();
        projectexa$refreshTaczGui();
    }

    @Unique
    private void projectexa$updateTransmuteButton() {
        if (projectexa$transmuteButton == null) {
            return;
        }

        boolean visible = selectedRecipe != null
                && PacketOpenArcaneTransmutationTablet.hasTablet(Minecraft.getInstance().player);
        projectexa$transmuteButton.visible = visible;
        projectexa$transmuteButton.active = visible;
    }

    @Unique
    private void projectexa$transmute(Button ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (selectedRecipe == null || !PacketOpenArcaneTransmutationTablet.hasTablet(minecraft.player)) {
            return;
        }

        List<ItemStack> displayedIngredients = new ArrayList<>();
        for (GunSmithTableIngredient input : selectedRecipe.getInputs()) {
            ItemStack[] options = input.getIngredient().getItems();
            if (options.length == 0) {
                displayedIngredients.add(ItemStack.EMPTY);
                continue;
            }

            int option = (int) ((System.currentTimeMillis() / 1000L) % options.length);
            displayedIngredients.add(options[option].copyWithCount(1));
        }

        PacketHandler.sendToServer(new PacketTaczTransmutation(selectedRecipe.getId(), displayedIngredients));
        projectexa$scheduleTaczRefresh();
    }

    @Unique
    private void projectexa$scheduleTaczRefresh() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            updateIngredientCount();
            return;
        }

        // The inventory packets arrive after this click. Rebuild for a few client ticks so
        // TACZ sees the newly inserted stacks instead of its pre-click inventory snapshot.
        projectexa$refreshesRemaining = 4;
        projectexa$nextRefreshTick = minecraft.level.getGameTime() + 1;
        projectexa$lastRefreshTick = Long.MIN_VALUE;
    }

    @Unique
    private void projectexa$refreshTaczGui() {
        if (projectexa$refreshesRemaining <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime < projectexa$nextRefreshTick || gameTime == projectexa$lastRefreshTick) {
            return;
        }

        projectexa$lastRefreshTick = gameTime;
        projectexa$nextRefreshTick = gameTime + 1;
        projectexa$refreshesRemaining--;
        updateIngredientCount();
    }
}
