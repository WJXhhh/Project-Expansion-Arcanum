package com.wjx.neoforge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.wjx.neoforge.projectexa.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.components.DataComponentManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DataComponentManager.class, remap = false)
abstract class PersistentComponentEmcMixin {
    @ModifyExpressionValue(method = "getEmcValue", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lmoze_intel/projecte/emc/EMCMappingHandler;getStoredEmcValue(Lmoze_intel/projecte/api/ItemInfo;)J"))
    private static long projectexa$usePersistentValue(long exact, ItemInfo info) {
        if (exact != 0 || !info.hasModifiedComponents() || !Config.server.Spec.isLoaded()
                || !Config.server.persistentNbtEmcFallback.get()) return exact;
        ItemInfo persistent = IEMCProxy.INSTANCE.getPersistentInfo(info);
        if (!persistent.hasModifiedComponents() || persistent.equals(info)) return exact;
        return EMCMappingHandler.getStoredEmcValue(persistent);
    }
}
