package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.integrations.jei.UniversalArcaneCraftingRecipeTransferHandler;
import com.wjx.neoforge.projectexa.integrations.jei.ModRecipeMaterialTransferHandler;
import com.wjx.neoforge.projectexa.integrations.RecipeMaterialRequests;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import java.util.Optional;

/** Preserve native handlers; provide a fallback only where JEI has none. */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public class JeiRecipeTransferManagerMixin {
    @WrapMethod(method = "getRecipeTransferHandler")
    private Optional<?> projectexa$fallback(AbstractContainerMenu menu, IRecipeCategory<?> category,
                                             Operation<Optional<?>> original) {
        Optional<?> result = original.call(menu, category);
        if (result.isEmpty() && category.getRecipeType().equals(RecipeTypes.CRAFTING)) {
            return Optional.of(UniversalArcaneCraftingRecipeTransferHandler.INSTANCE);
        } else if (result.isEmpty() && RecipeMaterialRequests.supported(category.getRecipeType().getUid())) {
            return Optional.of(new ModRecipeMaterialTransferHandler(category.getRecipeType()));
        }
        return result;
    }
}
