package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.CraftingResolver;
import com.huanghuang.rsintegration.crafting.endpoint.CraftStorageEndpoint;
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/** Keep RSI's native reservation, extraction and material-refund semantics. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.ExtractionLedger", remap = false)
public abstract class RsiExtractionLedgerMixin {
    @Shadow @Final private Map<CraftingResolver.StackKey, Long> pendingInv;

    @Shadow
    private static List<IItemHandler> findAllBackpackInventories(ServerPlayer player) {
        throw new AssertionError();
    }

    @Shadow
    private int countExactAvailableInInventory(ServerPlayer player, ItemStack template) {
        throw new AssertionError();
    }

    @Inject(method = "findAllBackpackInventories", at = @At("RETURN"), cancellable = true)
    private static void projectexa$appendArcaneSource(ServerPlayer player,
            CallbackInfoReturnable<List<IItemHandler>> cir) {
        cir.setReturnValue(ArcaneCraftingInventory.append(player, cir.getReturnValue(), Map.of()));
    }

    @Redirect(method = {"findAvailableInInventory", "countExactAvailableInInventory",
            "countExactAvailableInInventoryLong"},
            at = @At(value = "INVOKE", target = "Lcom/huanghuang/rsintegration/crafting/ExtractionLedger;findAllBackpackInventories(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/List;"))
    private List<IItemHandler> projectexa$budgetPendingEmc(ServerPlayer player) {
        return ArcaneCraftingInventory.append(player, findAllBackpackInventories(player), pendingInv);
    }

    @ModifyVariable(method = "reserveExactAcrossNetworkAndInventory", at = @At("HEAD"),
            argsOnly = true, ordinal = 0)
    private ItemStack projectexa$adaptArcaneExactTemplate(ItemStack original, ItemStack template, int needed,
            CraftStorageEndpoint endpoint, ServerPlayer player) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return original;
        return ArcaneCraftingInventory.exactReservationTemplate(player, original, needed,
                findAllBackpackInventories(player));
    }

    @Inject(method = "findAvailableInInventory", at = @At("RETURN"), cancellable = true)
    private void projectexa$combinePhysicalAndArcaneStock(ServerPlayer player, Ingredient ingredient, int needed,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty() || needed <= 0 || ingredient.isEmpty()) return;
        // RSI's generic scan subtracts pending per slot. A physical and a virtual slot
        // for the same key must instead share one pending counter, just like its exact path.
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player,
                findAllBackpackInventories(player), pendingInv);
        for (int slot = 0; slot < virtual.getSlots(); slot++) {
            ItemStack template = virtual.getStackInSlot(slot);
            if (template.isEmpty() || !ingredient.test(template)
                    || countExactAvailableInInventory(player, template) < needed) continue;
            pendingInv.merge(ArcaneCraftingInventory.key(template), (long) needed, Long::sum);
            cir.setReturnValue(template.copyWithCount(1));
            return;
        }
    }
}
