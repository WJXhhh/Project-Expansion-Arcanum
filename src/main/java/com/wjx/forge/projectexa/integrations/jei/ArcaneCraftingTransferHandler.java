package com.wjx.forge.projectexa.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import com.wjx.forge.projectexa.registries.MenuTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;

public class ArcaneCraftingTransferHandler implements IRecipeTransferHandler<ContainerArcaneTransmutationTablet, CraftingRecipe> {
    private static final int RED_HIGHLIGHT = 1727987712;

    @Override
    public Class<? extends ContainerArcaneTransmutationTablet> getContainerClass() {
        return ContainerArcaneTransmutationTablet.class;
    }

    @Override
    public Optional<MenuType<ContainerArcaneTransmutationTablet>> getMenuType() {
        return Optional.of(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get());
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(ContainerArcaneTransmutationTablet container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        List<IRecipeSlotView> recipeSlots = slots.getSlotViews();
        if (doTransfer) {
            List<List<ItemStack>> possibilities = new ArrayList<>(9);
            for (int i = 1; i <= 9; i++) {
                if (i < recipeSlots.size()) {
                    List<ItemStack> items = recipeSlots.get(i).getItemStacks().toList();
                    possibilities.add(items.isEmpty() ? List.of(ItemStack.EMPTY) : items);
                } else {
                    possibilities.add(List.of(ItemStack.EMPTY));
                }
            }
            PacketHandler.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(possibilities, transferAll));
            return null;
        }

        List<Integer> missing = findMissingSlots(recipeSlots, container, player);
        return new ErrorRenderer(slots, missing);
    }

    private static List<Integer> findMissingSlots(List<IRecipeSlotView> recipeSlots,
                                                   ContainerArcaneTransmutationTablet container, Player player) {
        List<Integer> missing = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (i >= recipeSlots.size() || recipeSlots.get(i).isEmpty()) continue;
            boolean found = false;
            for (ItemStack stack : recipeSlots.get(i).getItemStacks().toList()) {
                if (player.getInventory().contains(stack)) {
                    found = true;
                    break;
                }
                if (container.getProvider().hasKnowledge(ItemInfo.fromStack(stack))
                        && IEMCProxy.INSTANCE.getValue(stack) > 0
                        && container.transmutationInventory.getAvailableEmc().compareTo(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack))) >= 0) {
                    found = true;
                    break;
                }
            }
            if (!found) missing.add(i - 1);
        }
        return missing;
    }

    private record ErrorRenderer(IRecipeSlotsView slots, List<Integer> missing) implements IRecipeTransferError {
        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public int getButtonHighlightColor() {
            return 0;
        }

        @Override
        public void showError(GuiGraphics graphics, int mouseX, int mouseY, IRecipeSlotsView ignored, int recipeX, int recipeY) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(recipeX, recipeY, 0);
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            for (int index : missing) {
                if (index >= 0 && index < inputs.size()) inputs.get(index).drawHighlight(graphics, RED_HIGHLIGHT);
            }
            pose.popPose();
        }
    }

}
