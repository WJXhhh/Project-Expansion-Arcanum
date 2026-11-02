package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wjx.forge.projectexa.gui.ProjectExaTransferIcon;
import com.wjx.forge.projectexa.integrations.jei.JeiRecipeTransferButtonSupport;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.buttons.IButtonState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** JEI's controller-based buttons; older JEI versions skip this absent target. */
@Pseudo
@Mixin(targets = "mezz.jei.gui.recipes.RecipeTransferButtonController", remap = false)
public abstract class JeiRecipeTransferButtonControllerMixin {
    @Shadow(remap = false)
    @Final
    private IRecipeLayoutDrawable<?> recipeLayout;

    @WrapMethod(method = "initState(Lmezz/jei/api/gui/buttons/IButtonState;)V", remap = false)
    private void projectexa$useProjectExaTransferIcon(IButtonState state, Operation<Void> original) {
        original.call(state);
        if (JeiRecipeTransferButtonSupport.isSupportedRecipe(recipeLayout)) {
            state.setIcon(ProjectExaTransferIcon.INSTANCE);
        }
    }
}
