package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.item.WarehouseProjectiles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowEmcWarehouseMixin {
    // Vanilla calls this only after full charge. Multishot's copied side arrows are free;
    // leave its native charged-projectile NBT and non-pickup side-arrow behavior intact.
    @Inject(method = "loadProjectile", at = @At("HEAD"), cancellable = true)
    private static void projectexa$payForArrow(LivingEntity entity, ItemStack crossbow, ItemStack projectile,
                                              boolean multishotCopy, boolean creative,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!multishotCopy && entity instanceof Player player
                && !WarehouseProjectiles.commit(player, crossbow, projectile)) {
            cir.setReturnValue(false);
        }
    }
}
