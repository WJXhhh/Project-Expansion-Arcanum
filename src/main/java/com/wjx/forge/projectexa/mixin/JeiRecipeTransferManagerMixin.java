package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.jei.UniversalArcaneCraftingRecipeTransferHandler;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Adds the tablet transfer handler when JEI has no container-specific one. */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public class JeiRecipeTransferManagerMixin {
    @Inject(method = "getRecipeTransferHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void projectexa$addUniversalCraftingTransferHandler(
            AbstractContainerMenu container, IRecipeCategory<?> recipeCategory,
            CallbackInfoReturnable<Optional<?>> callback) {
        if (callback.getReturnValue().isPresent()
                || recipeCategory.getRecipeType() != RecipeTypes.CRAFTING) {
            return;
        }

        callback.setReturnValue(Optional.of(UniversalArcaneCraftingRecipeTransferHandler.INSTANCE));
    }
}
