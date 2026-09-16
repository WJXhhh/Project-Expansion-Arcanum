package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.gui.ProjectExaTransferIcon;
import com.wjx.forge.projectexa.integrations.jei.GoetyJeiRecipeTypes;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the ProjectExA transfer icon for Goety JEI transfer buttons. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButton", remap = false)
public abstract class JeiRecipeTransferButtonMixin {
    private static final ThreadLocal<Boolean> PROJECTEXA$GOETY_RECIPE = new ThreadLocal<>();

    @Inject(method = "create", at = @At("HEAD"), remap = false)
    private static void projectexa$rememberRecipeCategory(
            IRecipeLayoutDrawable<?> recipeLayout, Runnable onClose,
            CallbackInfoReturnable<?> callbackInfo) {
        PROJECTEXA$GOETY_RECIPE.set(isGoetyRecipe(recipeLayout));
    }

    @ModifyArg(
            method = "create",
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
    private static IDrawable projectexa$useGoetyTransferIcon(IDrawable original) {
        return Boolean.TRUE.equals(PROJECTEXA$GOETY_RECIPE.get())
                ? ProjectExaTransferIcon.INSTANCE
                : original;
    }

    @Inject(method = "create", at = @At("RETURN"), remap = false)
    private static void projectexa$forgetRecipeCategory(
            IRecipeLayoutDrawable<?> recipeLayout, Runnable onClose,
            CallbackInfoReturnable<?> callbackInfo) {
        PROJECTEXA$GOETY_RECIPE.remove();
    }

    private static boolean isGoetyRecipe(IRecipeLayoutDrawable<?> recipeLayout) {
        ResourceLocation uid = recipeLayout.getRecipeCategory().getRecipeType().getUid();
        return GoetyJeiRecipeTypes.isSupported(uid);
    }
}
