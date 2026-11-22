package com.wjx.forge.projectexa.mixin;

import com.google.common.collect.ImmutableList;
import com.wjx.forge.projectexa.integrations.forgematica.ForgematicaEasyPlace;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.litematica.config.Configs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.config.Configs$Generic", remap = false)
public abstract class ForgematicaConfigMixin {
    @Shadow @Final @Mutable public static ImmutableList<IConfigBase> OPTIONS;

    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void projectexa$addOption(CallbackInfo ci) {
        ImmutableList.Builder<IConfigBase> builder = ImmutableList.builder();
        for (IConfigBase option : OPTIONS) {
            builder.add(option);
            if (option == Configs.Generic.EASY_PLACE_MODE) builder.add(ForgematicaEasyPlace.AUTO_TRANSMUTE);
        }
        OPTIONS = builder.build();
    }
}
