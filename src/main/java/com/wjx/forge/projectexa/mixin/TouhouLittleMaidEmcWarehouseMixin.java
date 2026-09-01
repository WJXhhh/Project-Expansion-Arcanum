package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.item.EmcWarehouseInventoryHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes a bound warehouse in a maid inventory visible to TACZ's normal reload scan. */
@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid")
public class TouhouLittleMaidEmcWarehouseMixin {
    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private <T> void projectexa$appendEmcWarehouse(Capability<T> capability, @Nullable Direction side,
                                                   CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability != ForgeCapabilities.ITEM_HANDLER || !cir.getReturnValue().isPresent()) {
            return;
        }
        LivingEntity maid = (LivingEntity) (Object) this;
        IItemHandler physical = (IItemHandler) cir.getReturnValue().orElse(null);
        if (physical != null && EmcWarehouseInventoryHandler.hasWarehouse(physical)) {
            cir.setReturnValue(LazyOptional.of(() -> new EmcWarehouseInventoryHandler(maid, physical)).cast());
        }
    }
}
