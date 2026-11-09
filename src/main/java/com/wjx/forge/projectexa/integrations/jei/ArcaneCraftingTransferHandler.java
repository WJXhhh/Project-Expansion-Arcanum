package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import com.wjx.forge.projectexa.registries.MenuTypes;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
import java.util.Optional;

public class ArcaneCraftingTransferHandler implements IRecipeTransferHandler<ContainerArcaneTransmutationTablet, CraftingRecipe> {
    private final RecipeType<CraftingRecipe> recipeType;

    public ArcaneCraftingTransferHandler() {
        this(RecipeTypes.CRAFTING);
    }

    public ArcaneCraftingTransferHandler(RecipeType<CraftingRecipe> recipeType) {
        this.recipeType = recipeType;
    }

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
        return recipeType;
    }

    @SuppressWarnings("removal") // JEI 15 still requires the legacy abstract overload.
    @Override
    public @Nullable IRecipeTransferError transferRecipe(ContainerArcaneTransmutationTablet container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        ArcaneCraftingTransferSupport.Status status = ArcaneCraftingTransferSupport.check(slots, player, container);
        if (!doTransfer || !status.canTransfer()) {
            return status;
        }

        PacketHandler.sendToServer(new PacketArcaneTransmutationTabletRecipeTransfer(status.recipe(transferAll), transferAll));
        return null;
    }
}
