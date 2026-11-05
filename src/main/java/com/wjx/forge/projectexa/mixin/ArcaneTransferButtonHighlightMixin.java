package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wjx.forge.projectexa.integrations.jei.ArcaneCraftingTransferSupport;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keep JEI's disabled-button semantics, but render our missing-material tint. */
@Pseudo
@Mixin(targets = {
        "mezz.jei.gui.recipes.RecipeTransferButton",
        "mezz.jei.gui.recipes.RecipeTransferButtonController"
}, remap = false)
public abstract class ArcaneTransferButtonHighlightMixin {
    @WrapOperation(
            method = {
                    "draw(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    "drawExtras(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/Rect2i;IIF)V"
            },
            at = @At(value = "INVOKE", target = "Lmezz/jei/api/recipe/transfer/IRecipeTransferError;getType()"
                    + "Lmezz/jei/api/recipe/transfer/IRecipeTransferError$Type;"),
            require = 0,
            remap = false
    )
    private IRecipeTransferError.Type projectexa$renderMissingMaterialTint(
            IRecipeTransferError error, Operation<IRecipeTransferError.Type> original) {
        return error instanceof ArcaneCraftingTransferSupport.Status
                ? IRecipeTransferError.Type.COSMETIC : original.call(error);
    }
}
