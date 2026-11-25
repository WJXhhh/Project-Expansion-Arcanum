package com.wjx.neoforge.projectexa.mixin.tacz;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.util.time.GunTime;
import com.wjx.neoforge.projectexa.integrations.tacz.TaczTransmuteButton;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketTaczTransmutation;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GunSmithTableScreen.class, remap = false)
abstract class TaczGunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    @Shadow private RecipeHolder<GunSmithTableRecipe> selectedRecipe;
    @Shadow private void getPlayerIngredientCount(RecipeHolder<GunSmithTableRecipe> holder) { throw new AssertionError(); }

    @Unique private Button projectexa$transmuteButton;
    @Unique private int projectexa$refreshesRemaining;
    @Unique private long projectexa$nextRefreshTick;

    protected TaczGunSmithTableScreenMixin(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void projectexa$addTransmuteButton(CallbackInfo ci) {
        projectexa$transmuteButton = addRenderableWidget(new TaczTransmuteButton(leftPos + 265, topPos + 161,
                this::projectexa$transmute));
        projectexa$updateButton();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void projectexa$update(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        projectexa$updateButton();
        if (projectexa$refreshesRemaining > 0 && minecraft.level != null
                && minecraft.level.getGameTime() >= projectexa$nextRefreshTick) {
            projectexa$nextRefreshTick = minecraft.level.getGameTime() + 1;
            projectexa$refreshesRemaining--;
            getPlayerIngredientCount(selectedRecipe);
        }
    }

    @Unique
    private void projectexa$updateButton() {
        if (projectexa$transmuteButton == null) return;
        boolean available = selectedRecipe != null
                && selectedRecipe.value().getInputs().size() <= PacketTaczTransmutation.MAX_INPUTS
                && ArcaneTabletAccess.hasTablet(minecraft.player) && minecraft.getConnection() != null
                && NetworkRegistry.hasChannel(minecraft.getConnection(), PacketTaczTransmutation.TYPE.id());
        projectexa$transmuteButton.visible = available;
        projectexa$transmuteButton.active = available;
    }

    @Unique
    private void projectexa$transmute(Button button) {
        if (!button.active || selectedRecipe == null || minecraft.level == null) return;
        List<ItemStack> displayed = new ArrayList<>();
        long rotation = GunTime.nowMillis() / 1_000;
        for (GunSmithTableIngredient input : selectedRecipe.value().getInputs()) {
            ItemStack[] options = input.getIngredient().getItems();
            displayed.add(options.length == 0 ? ItemStack.EMPTY
                    : options[Math.floorMod(rotation, options.length)].copyWithCount(1));
        }
        PacketDistributor.sendToServer(new PacketTaczTransmutation(menu.containerId, selectedRecipe.id(), displayed));
        projectexa$refreshesRemaining = 4;
        projectexa$nextRefreshTick = minecraft.level.getGameTime() + 1;
    }
}
