package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.integrations.tacz.TaczIntegration;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds EMC warehouse ammunition to TACZ's direct vanilla-inventory HUD count. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.gui.overlay.GunHudOverlay")
public abstract class TaczGunHudEmcWarehouseMixin {
    @Shadow(remap = false)
    private static int cacheInventoryAmmoCount;

    @Inject(method = "handleInventoryAmmo(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Inventory;)V",
            at = @At("RETURN"), remap = false, require = 0)
    private static void projectexa$appendWarehouseAmmo(ItemStack gunStack, Inventory inventory, CallbackInfo ci) {
        int warehouseAmmo = TaczIntegration.getWarehouseAmmoCount(inventory.player, gunStack);
        cacheInventoryAmmoCount = Math.min(TaczIntegration.MAX_HUD_AMMO_COUNT,
                cacheInventoryAmmoCount + warehouseAmmo);
    }
}
