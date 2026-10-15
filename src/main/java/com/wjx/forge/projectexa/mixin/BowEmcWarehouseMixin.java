package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.item.WarehouseProjectiles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BowItem.class)
public class BowEmcWarehouseMixin {
    // ArrowLooseEvent has already run here; cancelled or undercharged shots never pay.
    // Capturing the actual selected stack preserves physical/event-supplied ammunition.
    @Inject(method = "releaseUsing", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"),
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void projectexa$payForArrow(ItemStack bow, Level level, LivingEntity entity, int timeLeft,
                                       CallbackInfo ci, Player player, boolean infinite,
                                       ItemStack projectile, int charge) {
        if (BowItem.getPowerForTime(charge) >= 0.1F
                && !WarehouseProjectiles.commit(player, bow, projectile)) {
            ci.cancel();
        }
    }
}
