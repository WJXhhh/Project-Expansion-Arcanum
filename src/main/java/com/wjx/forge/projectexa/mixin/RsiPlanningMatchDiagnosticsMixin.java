package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.rsi.RsiPlanningDiagnostics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Inactive outside the validator scope. Does not cancel or change the matching result. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.IngredientMatcher", remap = false)
public abstract class RsiPlanningMatchDiagnosticsMixin {
    @Inject(method = "test(Lnet/minecraft/world/item/crafting/Ingredient;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), require = 0)
    private static void projectexa$captureBeforeMatch(Ingredient ingredient, ItemStack actual, CallbackInfoReturnable<Boolean> cir) {
        RsiPlanningDiagnostics.beforeMatch(ingredient, actual);
    }

    @Inject(method = "test(Lnet/minecraft/world/item/crafting/Ingredient;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Ingredient;test(Lnet/minecraft/world/item/ItemStack;)Z",
                    ordinal = 1), require = 0)
    private static void projectexa$captureAtStrictTest(Ingredient ingredient, ItemStack actual, CallbackInfoReturnable<Boolean> cir) {
        RsiPlanningDiagnostics.beforeIngredientTest(ingredient, actual);
    }

    @Inject(method = "test(Lnet/minecraft/world/item/crafting/Ingredient;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"), require = 0)
    private static void projectexa$captureAfterMatch(Ingredient ingredient, ItemStack actual, CallbackInfoReturnable<Boolean> cir) {
        RsiPlanningDiagnostics.afterMatch(ingredient, actual, cir.getReturnValue());
    }
}
