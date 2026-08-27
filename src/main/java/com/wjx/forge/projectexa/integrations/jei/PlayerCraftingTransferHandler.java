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
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps JEI's normal 2x2 player crafting behavior while handing oversized
 * crafting recipes to the arcane transmutation tablet when available.
 */
public class PlayerCraftingTransferHandler implements IRecipeTransferHandler<InventoryMenu, CraftingRecipe> {
    private static final int[] PLAYER_GRID_SLOTS = {0, 1, 3, 4};

    private final IRecipeTransferHandlerHelper transferHelper;
    private final IRecipeTransferHandler<InventoryMenu, CraftingRecipe> playerHandler;

    public PlayerCraftingTransferHandler(IRecipeTransferHandlerHelper transferHelper) {
        this.transferHelper = transferHelper;
        IRecipeTransferInfo<InventoryMenu, CraftingRecipe> transferInfo = transferHelper.createBasicRecipeTransferInfo(
                InventoryMenu.class, null, RecipeTypes.CRAFTING, 1, 4, 9, 36);
        this.playerHandler = transferHelper.createUnregisteredRecipeTransferHandler(transferInfo);
    }

    @Override
    public Class<? extends InventoryMenu> getContainerClass() {
        return InventoryMenu.class;
    }

    @Override
    public Optional<MenuType<InventoryMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(InventoryMenu container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        if (fitsPlayerCrafting(recipe, inputs)) {
            IRecipeSlotsView playerInputs = transferHelper.createRecipeSlotsView(playerGridSlots(inputs));
            return playerHandler.transferRecipe(container, recipe, playerInputs, player, transferAll, doTransfer);
        }

        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return transferHelper.createUserErrorWithTooltip(
                    Component.translatable("jei.tooltip.error.recipe.transfer.too.large.player.inventory"));
        }

        if (doTransfer) {
            PacketHandler.sendToServer(new PacketOpenArcaneTransmutationTabletWithRecipe(toPossibilities(inputs), transferAll));
        }
        return null;
    }

    private static boolean fitsPlayerCrafting(CraftingRecipe recipe, List<IRecipeSlotView> inputs) {
        if (!recipe.canCraftInDimensions(2, 2)) return false;

        // JEI normally exposes the full 3x3 layout. Keep the compact case
        // tolerant as well for third-party crafting category implementations.
        if (inputs.size() <= 4) return true;
        for (int index = 0; index < inputs.size(); index++) {
            if (!isPlayerGridSlot(index) && !inputs.get(index).isEmpty()) return false;
        }
        return true;
    }

    private static List<IRecipeSlotView> playerGridSlots(List<IRecipeSlotView> inputs) {
        if (inputs.size() <= 4) return inputs;
        List<IRecipeSlotView> result = new ArrayList<>(PLAYER_GRID_SLOTS.length);
        for (int index : PLAYER_GRID_SLOTS) {
            if (index < inputs.size()) result.add(inputs.get(index));
        }
        return result;
    }

    private static boolean isPlayerGridSlot(int index) {
        for (int playerGridSlot : PLAYER_GRID_SLOTS) {
            if (index == playerGridSlot) return true;
        }
        return false;
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
