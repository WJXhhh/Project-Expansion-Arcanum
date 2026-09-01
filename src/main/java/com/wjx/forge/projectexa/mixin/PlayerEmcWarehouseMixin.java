package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.item.EmcWarehouseInventoryHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes carried EMC warehouses visible to consumers of the player's Forge item handler. */
@Mixin(Player.class)
public class PlayerEmcWarehouseMixin {
    @Inject(method = "getCapability(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)Lnet/minecraftforge/common/util/LazyOptional;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private <T> void projectexa$appendEmcWarehouse(Capability<T> capability, @Nullable Direction side,
                                                   CallbackInfoReturnable<LazyOptional<T>> cir) {
        if (capability != ForgeCapabilities.ITEM_HANDLER || !cir.getReturnValue().isPresent()) {
            return;
        }
        Player player = (Player) (Object) this;
        IItemHandler physical = (IItemHandler) cir.getReturnValue().orElse(null);
        if (physical != null && EmcWarehouseInventoryHandler.hasWarehouse(physical)) {
            cir.setReturnValue(LazyOptional.of(() -> new EmcWarehouseInventoryHandler(player, physical)).cast());
        }
    }
}
