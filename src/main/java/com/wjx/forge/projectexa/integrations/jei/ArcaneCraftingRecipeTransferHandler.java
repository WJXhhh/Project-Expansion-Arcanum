package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTabletWithRecipe;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;

import javax.annotation.Nullable;
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

    @SuppressWarnings("removal") // JEI 15 still requires the legacy abstract overload.
    @Override
    public @Nullable IRecipeTransferError transferRecipe(C container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return HIDDEN;
        }

        ArcaneCraftingTransferSupport.Status status = ArcaneCraftingTransferSupport.check(slots, player, container);
        if (!doTransfer || !status.canTransfer()) {
            return status;
        }

        PacketHandler.sendToServer(new PacketOpenArcaneTransmutationTabletWithRecipe(status.recipe(transferAll), transferAll));
        return null;
    }
}
