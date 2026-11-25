package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.CraftingResolver;
import com.huanghuang.rsintegration.crafting.MaterialSourceSelection;
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

/** Adds EMC stock to the source-filtered planning and task-admission snapshots. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.MaterialSources", remap = false)
public abstract class RsiTypedMaterialSourcesMixin {
    @Inject(method = "countInventoryForTypes", at = @At("RETURN"), cancellable = true)
    private static void projectexa$includeArcaneKnowledge(Player player, Set<Item> itemTypes,
            CallbackInfoReturnable<Map<CraftingResolver.StackKey, Long>> cir) {
        MaterialSourceSelection sources = MaterialSourceSelection.current();
        if (!sources.backpacks() || !(player instanceof ServerPlayer serverPlayer)) return;
        Map<CraftingResolver.StackKey, Long> physical = cir.getReturnValue();
        Map<CraftingResolver.StackKey, Long> counts =
                ArcaneCraftingInventory.withArcaneCounts(serverPlayer, itemTypes, physical);
        if (counts != physical) cir.setReturnValue(counts);
    }
}
