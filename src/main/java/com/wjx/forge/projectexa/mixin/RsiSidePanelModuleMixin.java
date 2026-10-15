package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.config.RSIntegrationConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** RSI checks this common-config switch before Forge has loaded common configs. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.sidepanel.RSSidePanelModule", remap = false)
public abstract class RsiSidePanelModuleMixin {
    @Redirect(method = "isEnabled()Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;get()Ljava/lang/Object;"),
            remap = false)
    private static Object projectexa$readLoadedSidePanelSwitch(ForgeConfigSpec.BooleanValue value) {
        return RSIntegrationConfig.COMMON_SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
