package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.config.RSIntegrationConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Guards RSI's server-configured key separately from its client-configured layout. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.sidepanel.RSSidePanelClient", remap = false)
public abstract class RsiSidePanelClientMixin {
    @Redirect(method = "registerKeyMappings()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;get()Ljava/lang/Object;"),
            remap = false)
    private static Object projectexa$readLoadedSidePanelKey(ForgeConfigSpec.IntValue value) {
        return RSIntegrationConfig.SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    @Redirect(method = "init()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;get()Ljava/lang/Object;"),
            remap = false)
    private static Object projectexa$readLoadedSidePanelInteger(ForgeConfigSpec.IntValue value) {
        return RSIntegrationConfig.CLIENT_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    @Redirect(method = "init()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"),
            remap = false)
    private static Object projectexa$readLoadedSidePanelHidden(ForgeConfigSpec.BooleanValue value) {
        return RSIntegrationConfig.CLIENT_SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
