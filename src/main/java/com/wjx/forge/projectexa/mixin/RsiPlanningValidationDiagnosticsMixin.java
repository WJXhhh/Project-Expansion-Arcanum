package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.IngredientMatcher;
import com.huanghuang.rsintegration.crafting.graph.CraftPlanGraph;
import com.huanghuang.rsintegration.crafting.graph.MaterialKey;
import com.wjx.forge.projectexa.integrations.rsi.RsiPlanningDiagnostics;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.graph.CraftPlanValidator", remap = false)
public abstract class RsiPlanningValidationDiagnosticsMixin {
    @Redirect(method = "findErrors", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/huanghuang/rsintegration/crafting/IngredientMatcher;test(Lnet/minecraft/world/item/crafting/Ingredient;Lcom/huanghuang/rsintegration/crafting/graph/MaterialKey;)Z"))
    private static boolean projectexa$observeExistingMatch(Ingredient ingredient, MaterialKey material, CraftPlanGraph graph) {
        RsiPlanningDiagnostics.Probe probe = RsiPlanningDiagnostics.begin(graph, ingredient, material.item(), material.tag());
        try {
            // Exactly one original invocation. Preserve the result and original exception object.
            boolean matched = IngredientMatcher.test(ingredient, material);
            RsiPlanningDiagnostics.complete(probe, matched, null);
            return matched;
        } catch (RuntimeException | LinkageError error) {
            RsiPlanningDiagnostics.complete(probe, false, error);
            throw error;
        } finally {
            RsiPlanningDiagnostics.close(probe);
        }
    }

    @Inject(method = "findErrors", at = @At("RETURN"), require = 0)
    private static void projectexa$observeValidation(CraftPlanGraph graph, CallbackInfoReturnable<List<String>> cir) {
        RsiPlanningDiagnostics.validation(graph, cir.getReturnValue());
    }
}
