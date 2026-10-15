package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.CraftingResolver;
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.MaterialSources", remap = false)
public abstract class RsiMaterialSourcesMixin {
    // When Sophisticated Backpacks is present, RSI already scans this list. Do not count EMC twice.
    @Redirect(method = "countInventory", at = @At(value = "INVOKE",
            target = "Lcom/huanghuang/rsintegration/crafting/ExtractionLedger;findAllBackpackInventories(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;"))
    private static List<IItemHandler> projectexa$physicalBackpacksOnly(ServerPlayer player) {
        // This package-private RSI method is accessed through the public accessor Mixin below.
        return RsiExtractionLedgerAccessor.projectexa$backpacks(player).stream()
                .filter(handler -> !(handler instanceof ArcaneCraftingInventory)).toList();
    }

    @Inject(method = "countInventory", at = @At("RETURN"), cancellable = true)
    private static void projectexa$includeArcaneKnowledge(Player player,
            CallbackInfoReturnable<Map<CraftingResolver.StackKey, Integer>> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Map<CraftingResolver.StackKey, Integer> counts = new HashMap<>(cir.getReturnValue());
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(serverPlayer,
                RsiExtractionLedgerAccessor.projectexa$backpacks(serverPlayer), Map.of());
        for (int slot = 0; slot < virtual.getSlots(); slot++) {
            ItemStack stack = virtual.getStackInSlot(slot);
            if (!stack.isEmpty()) counts.merge(ArcaneCraftingInventory.key(stack), stack.getCount(),
                    (left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
        }
        cir.setReturnValue(counts);
    }
}
