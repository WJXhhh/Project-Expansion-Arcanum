package com.wjx.neoforge.projectexa.integrations.jei;

import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneSmithingTransfer;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import com.wjx.neoforge.projectexa.util.Util;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Opens the arcane tablet's smithing station wherever JEI has no native transfer handler. */
public final class ArcaneSmithingTransferHandler implements
        IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<SmithingRecipe>> {
    public static final ArcaneSmithingTransferHandler INSTANCE = new ArcaneSmithingTransferHandler();
    private static final IRecipeTransferError HIDDEN = () -> IRecipeTransferError.Type.INTERNAL;

    public static boolean available(Player player) {
        if (!ArcaneTabletAccess.hasTablet(player)) return false;
        var provider = Util.getKnowledgeProvider(player);
        return provider != null && provider.hasKnowledge(new ItemStack(Items.SMITHING_TABLE));
    }

    @Override public Class<AbstractContainerMenu> getContainerClass() {
        return AbstractContainerMenu.class;
    }

    @Override public Optional<MenuType<AbstractContainerMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override public RecipeType<RecipeHolder<SmithingRecipe>> getRecipeType() { return RecipeTypes.SMITHING; }

    // Keep the legacy entry point as well as JEI's asynchronous transfer lifecycle.
    @SuppressWarnings("removal")
    @Override public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu menu,
            RecipeHolder<SmithingRecipe> recipe, IRecipeSlotsView slots, Player player,
            boolean transferAll, boolean doTransfer) {
        return transfer(menu, recipe, slots, player, transferAll, doTransfer, -1);
    }

    @Override public @Nullable IRecipeTransferError transferRecipe(
            IRecipeTransferContext<RecipeHolder<SmithingRecipe>, AbstractContainerMenu> context,
            boolean doTransfer) {
        return JeiTransferCompletion.transfer(context, doTransfer, transferId -> transfer(
                context.getContainer(), context.getRecipe(), context.getRecipeSlots(), context.getPlayer(),
                context.isMaxTransfer(), doTransfer, transferId));
    }

    private @Nullable IRecipeTransferError transfer(AbstractContainerMenu menu,
            RecipeHolder<SmithingRecipe> recipe, IRecipeSlotsView slots, Player player,
            boolean transferAll, boolean doTransfer, int transferId) {
        if (!available(player) || slots.getSlotViews(RecipeIngredientRole.INPUT).size() != 3) return HIDDEN;
        var status = ArcaneSmithingTransferSupport.check(slots, player, menu, recipe.value());
        if (!doTransfer || !status.canTransfer(transferAll)) return status;
        PacketDistributor.sendToServer(new PacketArcaneSmithingTransfer(
                menu.containerId, recipe.id(), status.recipe(transferAll, player, menu, recipe.value()), transferAll, transferId));
        return null;
    }
}
