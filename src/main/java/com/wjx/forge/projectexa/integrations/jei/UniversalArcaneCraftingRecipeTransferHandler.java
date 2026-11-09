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
 * Fallback JEI handler for crafting recipes in containers without their own
 * crafting transfer handler. It is supplied by the JEI manager mixin rather
 * than registered for one specific container class.
 */
public final class UniversalArcaneCraftingRecipeTransferHandler
        implements IRecipeTransferHandler<AbstractContainerMenu, CraftingRecipe> {
    public static final UniversalArcaneCraftingRecipeTransferHandler INSTANCE =
            new UniversalArcaneCraftingRecipeTransferHandler();

    private static final IRecipeTransferError HIDDEN = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.INTERNAL;
        }
    };

    private UniversalArcaneCraftingRecipeTransferHandler() {
    }

    @Override
    public Class<? extends AbstractContainerMenu> getContainerClass() {
        return AbstractContainerMenu.class;
    }

    @Override
    public Optional<MenuType<AbstractContainerMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<CraftingRecipe> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @SuppressWarnings("removal") // JEI 15 still requires the legacy abstract overload.
    @Override
    public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu container, CraftingRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return HIDDEN;
        }

        ArcaneCraftingTransferSupport.Status status = ArcaneCraftingTransferSupport.check(slots, player, container);
        if (!doTransfer || !status.canTransfer()) {
            return status;
        }

        PacketHandler.sendToServer(new PacketOpenArcaneTransmutationTabletWithRecipe(
                recipe.getId(), status.recipe(transferAll), transferAll));
        return null;
    }
}
