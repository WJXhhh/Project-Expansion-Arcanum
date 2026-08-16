package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.util.EMCFormat;
import moze_intel.projecte.utils.Constants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;

// This mixin changes ProjectE's formatting to ours
@Mixin(Constants.class)
public class EMCFormatterMixin {
    @Inject(at = @At("HEAD"), method="getFormatter()Ljava/text/NumberFormat;", cancellable = true, remap = false)
    private static void getFormatter(CallbackInfoReturnable<NumberFormat> cir) {
        cir.setReturnValue(EMCFormat.INSTANCE);
    }
}
