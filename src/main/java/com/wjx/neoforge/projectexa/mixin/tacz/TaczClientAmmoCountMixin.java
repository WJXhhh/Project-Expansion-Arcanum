package com.wjx.neoforge.projectexa.mixin.tacz;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gameplay.ClientAmmoCount;
import com.wjx.neoforge.projectexa.integrations.tacz.TaczIntegration;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientAmmoCount.class, remap = false)
abstract class TaczClientAmmoCountMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private static void projectexa$addWarehouseAmmo(LocalPlayer player, ItemStack gun, IGun gunItem,
                                                    CallbackInfoReturnable<Integer> cir) {
        int physical = cir.getReturnValueI();
        if (physical >= ClientAmmoCount.MAX_DISPLAY_COUNT || gunItem.useDummyAmmo(gun)) return;
        int warehouse = TaczIntegration.countWarehouseAmmo(player, gun, ClientAmmoCount.MAX_DISPLAY_COUNT - physical);
        cir.setReturnValue(physical + warehouse);
    }
}
