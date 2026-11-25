package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.jei.JeiEmcInput;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.util.CommandUtil;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Pseudo
@Mixin(targets = {ProjectExaMixinConfigPlugin.JEI_CHEAT_INPUT, ProjectExaMixinConfigPlugin.JEI_FOCUS_INPUT},
        remap = false)
public abstract class JeiEmcCheatInputMixin {
    @Shadow @Final private CombinedRecipeFocusSource focusSource;
    @Shadow @Final private IIngredientManager ingredientManager;
    @Shadow @Final private IClientToggleState toggleState;
    @Shadow @Final private CommandUtil commandUtil;

    @Inject(method = {
            ProjectExaMixinConfigPlugin.JEI_HANDLE_INPUT + ProjectExaMixinConfigPlugin.JEI_GUI_INPUT_SIGNATURE,
            ProjectExaMixinConfigPlugin.JEI_HANDLE_INPUT + ProjectExaMixinConfigPlugin.JEI_COMMON_INPUT_SIGNATURE
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private void projectexa$transmute(Screen screen, @Coerce Object userInput, IInternalKeyMappings keys,
            CallbackInfoReturnable<Optional<?>> callback) {
        if (!toggleState.isCheatItemsEnabled() && userInput instanceof IJeiUserInput input) {
            var result = JeiEmcInput.handle(this, screen, input, keys,
                    focusSource, ingredientManager, toggleState, commandUtil);
            if (result.isPresent()) {
                callback.setReturnValue(result);
            }
        }
    }
}
