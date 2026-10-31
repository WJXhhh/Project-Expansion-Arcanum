package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.CraftingResolver;
import com.huanghuang.rsintegration.crafting.endpoint.CraftStorageEndpoint;
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import com.wjx.forge.projectexa.integrations.rsi.RsiCraftingDiagnostics;
import com.huanghuang.rsintegration.util.CraftLogContext;
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
    @Shadow private CraftLogContext logContext;

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
        ItemStack adapted = ArcaneCraftingInventory.exactReservationTemplate(player, original, needed,
                findAllBackpackInventories(player));
        RsiCraftingDiagnostics.log(player, "exact-template", RsiCraftingDiagnostics.item(original),
                () -> "op=" + (logContext == null ? "none" : logContext.opId())
                        + " ledger=" + Integer.toHexString(System.identityHashCode(this))
                        + " needed=" + needed + " endpoint=" + (endpoint == null ? "default" : endpoint.getClass().getName())
                        + " adapted=" + (adapted != original) + " before=[" + RsiCraftingDiagnostics.stack(original)
                        + "] after=[" + RsiCraftingDiagnostics.stack(adapted) + "] pendingInv="
                        + RsiCraftingDiagnostics.pending(pendingInv) + " physical=" + RsiCraftingDiagnostics.physical(player, adapted));
        return adapted;
    }

    @Inject(method = "reserveExactAcrossNetworkAndInventory", at = @At("RETURN"))
    private void projectexa$diagnoseExactResult(ItemStack template, int needed, CraftStorageEndpoint endpoint,
            ServerPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        RsiCraftingDiagnostics.log(player, cir.getReturnValue().isEmpty() ? "exact-reserve-failed" : "exact-reserve-ok",
                RsiCraftingDiagnostics.item(template), () -> "op=" + (logContext == null ? "none" : logContext.opId())
                        + " ledger=" + Integer.toHexString(System.identityHashCode(this)) + " needed=" + needed
                        + " template=[" + RsiCraftingDiagnostics.stack(template) + "] result=["
                        + RsiCraftingDiagnostics.stack(cir.getReturnValue()) + "] pendingInv=" + RsiCraftingDiagnostics.pending(pendingInv));
    }

    @Inject(method = "countExactAvailableInInventory", at = @At("RETURN"))
    private void projectexa$diagnoseInventoryCount(ServerPlayer player, ItemStack template, CallbackInfoReturnable<Integer> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        RsiCraftingDiagnostics.log(player, "inventory-exact-count", RsiCraftingDiagnostics.item(template),
                () -> "op=" + (logContext == null ? "none" : logContext.opId()) + " template=["
                        + RsiCraftingDiagnostics.stack(template) + "] available=" + cir.getReturnValue()
                        + " pendingInv=" + RsiCraftingDiagnostics.pending(pendingInv));
    }

    @Inject(method = "countExactAvailableInInventoryLong", at = @At("RETURN"), require = 0)
    private void projectexa$diagnoseBatchCount(ServerPlayer player, ItemStack template, CallbackInfoReturnable<Long> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        RsiCraftingDiagnostics.log(player, "inventory-exact-count-long", RsiCraftingDiagnostics.item(template),
                () -> "op=" + (logContext == null ? "none" : logContext.opId()) + " template=["
                        + RsiCraftingDiagnostics.stack(template) + "] available=" + cir.getReturnValue()
                        + " pendingInv=" + RsiCraftingDiagnostics.pending(pendingInv));
    }

    @Inject(method = "countExactAvailableInEndpoint", at = @At("RETURN"), require = 0)
    private void projectexa$diagnoseEndpointCount(CraftStorageEndpoint endpoint, ItemStack template, ServerPlayer player,
            CallbackInfoReturnable<Integer> cir) {
        if (player == null || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        RsiCraftingDiagnostics.log(player, "endpoint-exact-count", RsiCraftingDiagnostics.item(template),
                () -> "op=" + (logContext == null ? "none" : logContext.opId()) + " available=" + cir.getReturnValue()
                        + " endpoint=" + (endpoint == null ? "none" : endpoint.getClass().getName()));
    }

    @Inject(method = "commitToEndpoint", at = @At("RETURN"), require = 0)
    private void projectexa$diagnoseCommit(CraftStorageEndpoint endpoint, ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        RsiCraftingDiagnostics.log(player, "ledger-commit", String.valueOf(cir.getReturnValue()),
                () -> "op=" + (logContext == null ? "none" : logContext.opId())
                        + " ledger=" + Integer.toHexString(System.identityHashCode(this)) + " success=" + cir.getReturnValue());
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
