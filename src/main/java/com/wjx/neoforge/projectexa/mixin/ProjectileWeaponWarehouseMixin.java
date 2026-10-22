package com.wjx.neoforge.projectexa.mixin;

import com.wjx.neoforge.projectexa.api.ammo.AmmoSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21 unifies bow and crossbow consumption here, after charge/cancellation checks. */
@Mixin(value = ProjectileWeaponItem.class, remap = false)
public class ProjectileWeaponWarehouseMixin {
    @Inject(method = "useAmmo", at = @At("HEAD"), cancellable = true)
    private static void projectexa$validate(ItemStack weapon, ItemStack ammo, LivingEntity shooter,
                                            boolean intangible, CallbackInfoReturnable<ItemStack> cir) {
        if (shooter instanceof Player player && !AmmoSources.isPreviewValid(player, weapon, ammo)) cir.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "useAmmo", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private static void projectexa$pay(ItemStack weapon, ItemStack ammo, LivingEntity shooter,
                                       boolean intangible, CallbackInfoReturnable<ItemStack> cir) {
        if (!intangible && shooter instanceof Player player && !AmmoSources.consumeProjectile(player, weapon, ammo)) cir.setReturnValue(ItemStack.EMPTY);
    }
}
