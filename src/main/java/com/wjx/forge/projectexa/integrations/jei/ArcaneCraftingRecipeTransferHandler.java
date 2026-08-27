package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTabletWithRecipe;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adds JEI's crafting transfer button to containers that do not have a
 * crafting grid, routing the recipe to the player's arcane tablet.
 */
public final class ArcaneCraftingRecipeTransferHandler<C extends AbstractContainerMenu>
        implements IRecipeTransferHandler<C, CraftingRecipe> {
    private static final IRecipeTransferError HIDDEN = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.INTERNAL;
        }
    };

    private static final IRecipeTransferError TRANSFER_TOOLTIP = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public void showError(GuiGraphics graphics, int mouseX, int mouseY, IRecipeSlotsView slots,
                              int recipeX, int recipeY) {
            graphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("jei.projectexa.transfer.to_arcane_transmutation_table"),
                    mouseX,
                    mouseY
            );
        }
    };

    private final Class<? extends C> containerClass;
    private final MenuType<C> menuType;

    public ArcaneCraftingRecipeTransferHandler(Class<? extends C> containerClass, MenuType<C> menuType) {
        this.containerClass = containerClass;
        this.menuType = menuType;
    }

    @Override
    public Class<? extends C> getContainerClass() {
        return containerClass;
    }

    @Override
    public Optional<MenuType<C>> getMenuType() {
        return Optional.of(menuType);
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(C container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return HIDDEN;
        }

        if (!doTransfer) {
            return TRANSFER_TOOLTIP;
        }

        List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        PacketHandler.sendToServer(new PacketOpenArcaneTransmutationTabletWithRecipe(toPossibilities(inputs), transferAll));
        return null;
    }

    private static List<List<ItemStack>> toPossibilities(List<IRecipeSlotView> inputs) {
        List<List<ItemStack>> possibilities = new ArrayList<>(9);
        for (int index = 0; index < 9; index++) {
            List<ItemStack> items = index < inputs.size() ? inputs.get(index).getItemStacks().toList() : List.of();
            possibilities.add(items.isEmpty() ? List.of(ItemStack.EMPTY) : items);
        }
        return possibilities;
    }
}
