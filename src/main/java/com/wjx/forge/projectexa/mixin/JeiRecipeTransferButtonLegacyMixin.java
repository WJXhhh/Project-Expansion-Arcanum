package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.gui.ProjectExaTransferIcon;
import com.wjx.forge.projectexa.integrations.jei.JeiRecipeTransferButtonSupport;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** JEI 15.19 and earlier variant of the transfer button mixin. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButton", remap = false)
public abstract class JeiRecipeTransferButtonLegacyMixin {
    private static final ThreadLocal<Boolean> PROJECTEXA$SUPPORTED_RECIPE = new ThreadLocal<>();

    @Inject(
            method = "create(Lmezz/jei/api/gui/IRecipeLayoutDrawable;"
                    + "Ljava/lang/Runnable;)"
                    + "Lmezz/jei/gui/recipes/RecipeTransferButton;",
            at = @At("HEAD"),
            remap = false
    )
    private static void projectexa$rememberRecipeCategory(
            IRecipeLayoutDrawable<?> recipeLayout,
            Runnable onClose,
            CallbackInfoReturnable<?> callbackInfo) {
        PROJECTEXA$SUPPORTED_RECIPE.set(JeiRecipeTransferButtonSupport.isSupportedRecipe(recipeLayout));
    }

    @ModifyArg(
            method = "create(Lmezz/jei/api/gui/IRecipeLayoutDrawable;"
                    + "Ljava/lang/Runnable;)"
                    + "Lmezz/jei/gui/recipes/RecipeTransferButton;",
            at = @At(
                    value = "INVOKE",
                    target = "Lmezz/jei/gui/recipes/RecipeTransferButton;<init>("
                            + "Lmezz/jei/api/gui/drawable/IDrawable;"
                            + "Lmezz/jei/api/gui/IRecipeLayoutDrawable;"
                            + "Ljava/lang/Runnable;)V"
            ),
            index = 0,
            remap = false
    )
    private static IDrawable projectexa$useProjectExaTransferIcon(IDrawable original) {
        return Boolean.TRUE.equals(PROJECTEXA$SUPPORTED_RECIPE.get())
                ? ProjectExaTransferIcon.INSTANCE
                : original;
    }

    @Inject(
            method = "create(Lmezz/jei/api/gui/IRecipeLayoutDrawable;"
                    + "Ljava/lang/Runnable;)"
                    + "Lmezz/jei/gui/recipes/RecipeTransferButton;",
            at = @At("RETURN"),
            remap = false
    )
    private static void projectexa$forgetRecipeCategory(
            IRecipeLayoutDrawable<?> recipeLayout,
            Runnable onClose,
            CallbackInfoReturnable<?> callbackInfo) {
        PROJECTEXA$SUPPORTED_RECIPE.remove();
    }
}
