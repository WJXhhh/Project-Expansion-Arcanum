package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.integrations.curios.CuriosIntegration;
import com.wjx.neoforge.projectexa.item.EmcWarehouseInventoryHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Decorate the resolved capability, preserving providers from NeoForge and optional mods. */
@Mixin(value = EntityCapability.class, remap = false)
public class EntityWarehouseCapabilityMixin {
    @Inject(method = "getCapability", at = @At("RETURN"), cancellable = true)
    private void projectexa$warehouse(Entity entity, Object context, CallbackInfoReturnable<Object> cir) {
        if ((Object) this != Capabilities.ItemHandler.ENTITY || !(entity instanceof LivingEntity living)
                || !(cir.getReturnValue() instanceof IItemHandler physical)
                || physical instanceof EmcWarehouseInventoryHandler) return;
        IItemHandler curios = CuriosIntegration.getAll(living);
        if (EmcWarehouseInventoryHandler.hasWarehouse(physical, curios)) {
            cir.setReturnValue(new EmcWarehouseInventoryHandler(living, physical, curios));
        }
    }
}
