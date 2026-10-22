package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.integrations.jei.UniversalArcaneCraftingRecipeTransferHandler;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

/** Preserve native handlers; provide a fallback only where JEI has none. */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public class JeiRecipeTransferManagerMixin {
    @Inject(method = "getRecipeTransferHandler", at = @At("RETURN"), cancellable = true)
    private void projectexa$fallback(AbstractContainerMenu menu, IRecipeCategory<?> category,
                                      CallbackInfoReturnable<Optional<?>> cir) {
        if (cir.getReturnValue().isEmpty() && category.getRecipeType().equals(RecipeTypes.CRAFTING)) {
            cir.setReturnValue(Optional.of(UniversalArcaneCraftingRecipeTransferHandler.INSTANCE));
        }
    }
}
