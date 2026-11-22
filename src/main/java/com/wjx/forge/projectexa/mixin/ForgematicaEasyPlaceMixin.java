package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wjx.forge.projectexa.integrations.forgematica.ForgematicaEasyPlace;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.WorldUtils", remap = false)
public abstract class ForgematicaEasyPlaceMixin {
    @WrapOperation(method = "doEasyPlaceAction", at = @At(value = "INVOKE",
            target = "Lfi/dy/masa/litematica/util/InventoryUtils;schematicWorldPickBlock(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/client/Minecraft;)V"), remap = false)
    private static void projectexa$transmuteMissing(ItemStack stack, BlockPos pos, Level level, Minecraft mc, Operation<Void> original) {
        original.call(stack, pos, level, mc);
        ForgematicaEasyPlace.request(stack, pos, mc);
    }
}
