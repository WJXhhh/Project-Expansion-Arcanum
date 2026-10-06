package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.item.EmcWarehouseInventoryHandler;
import com.wjx.forge.projectexa.item.ItemPersonalEmcWarehouse;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes bound warehouses in a maid's inventory or equipped Curios visible to TACZ's reload scan. */
@Pseudo
@Mixin(targets = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid")
public class TouhouLittleMaidEmcWarehouseMixin {
    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private <T> void projectexa$appendEmcWarehouse(Capability<T> capability, @Nullable Direction side,
                                                   CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (!ItemPersonalEmcWarehouse.ENABLED || capability != ForgeCapabilities.ITEM_HANDLER
                || !cir.getReturnValue().isPresent()) {
            return;
        }
        LivingEntity maid = (LivingEntity) (Object) this;
        IItemHandler physical = (IItemHandler) cir.getReturnValue().orElse(null);
        IItemHandler curios = ModList.get().isLoaded(IntegrationHelper.CURIO_MODID)
                ? CuriosIntegration.getAll(maid) : null;
        if (physical != null && EmcWarehouseInventoryHandler.hasWarehouse(physical, curios)) {
            cir.setReturnValue(LazyOptional.of(
                    () -> new EmcWarehouseInventoryHandler(maid, physical, curios)).cast());
        }
    }
}
