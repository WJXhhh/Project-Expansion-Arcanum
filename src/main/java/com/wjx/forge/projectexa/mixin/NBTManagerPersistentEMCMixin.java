package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.nbt.NBTManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets EMC values that are registered for a normalized NBT form survive a changing stack.
 *
 * <p>ProjectE looks up the exact NBT of a stack and, when that misses, falls straight back to the value of
 * the bare item. That works for enchantments, where the bare item carries the value, but not for mods that
 * keep the item identity in NBT (TACZ guns, ammunition and attachments all share one item). This looks up
 * the persistent NBT form first - the very form ProjectE stores in transmutation knowledge - and only then
 * lets vanilla ProjectE behaviour continue.</p>
 *
 * <p>The lookup is skipped unless the exact stack has no value and the persistent form does, so nothing
 * changes for items that already resolve today.</p>
 */
@Mixin(NBTManager.class)
public class NBTManagerPersistentEMCMixin {

    @Inject(at = @At("HEAD"), method = "getEmcValue(Lmoze_intel/projecte/api/ItemInfo;)J", cancellable = true, remap = false)
    private static void projectexa$usePersistentValue(ItemInfo info, CallbackInfoReturnable<Long> cir) {
        if (!Config.persistentNbtEmcFallback.get() || !info.hasNBT() || EMCMappingHandler.hasEmcValue(info)) {
            return;
        }

        ItemInfo persistent = NBTManager.getPersistentInfo(info);
        if (!persistent.hasNBT() || persistent.equals(info) || !EMCMappingHandler.hasEmcValue(persistent)) {
            return;
        }

        // The persistent form has a value, so resolve through it. It hits the fast path above on re-entry.
        cir.setReturnValue(NBTManager.getEmcValue(persistent));
    }
}
