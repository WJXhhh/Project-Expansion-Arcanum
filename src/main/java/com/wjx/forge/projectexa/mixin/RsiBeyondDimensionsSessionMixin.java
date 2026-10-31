package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.storage.StorageItemKey;
import com.huanghuang.rsintegration.storage.StorageOperationMode;
import com.huanghuang.rsintegration.storage.StorageOperationResult;
import com.huanghuang.rsintegration.storage.StorageOperationStatus;
import com.huanghuang.rsintegration.storage.StorageSnapshot;
import com.huanghuang.rsintegration.storage.StorageSnapshotResult;
import com.huanghuang.rsintegration.storage.StoredItem;
import com.wjx.forge.projectexa.integrations.rsi.ArcaneCraftingInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Supplies RSI's Beyond Dimensions crafting endpoint without transforming its RS-linked ledger. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.storage.bd.BeyondDimensionsSession", remap = false)
public abstract class RsiBeyondDimensionsSessionMixin {
    @Shadow public abstract StorageItemKey itemKey(ItemStack stack);

    @Inject(method = "snapshotItems(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/Set;)Lcom/huanghuang/rsintegration/storage/StorageSnapshotResult;",
            at = @At("RETURN"), cancellable = true)
    private void projectexa$includeEmcInCraftingSnapshot(ServerPlayer player, Set<Item> itemTypes,
            CallbackInfoReturnable<StorageSnapshotResult> cir) {
        if (!projectexa$isCraftingCall() || !cir.getReturnValue().successful()) return;
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player, List.of(), Map.of());
        if (virtual.getSlots() == 0) return;

        StorageSnapshot snapshot = cir.getReturnValue().snapshot().orElseThrow();
        Map<StorageItemKey, Long> counts = new LinkedHashMap<>();
        for (StoredItem stored : snapshot.items()) counts.merge(stored.key(), stored.amount(),
                RsiBeyondDimensionsSessionMixin::projectexa$saturatedAdd);

        // Two ProjectE identities may normalize to one BD key. Count their shared
        // EMC balance once, then add that virtual stock to the real network stock.
        Map<StorageItemKey, Long> virtualCounts = new LinkedHashMap<>();
        for (int slot = 0; slot < virtual.getSlots(); slot++) {
            ItemStack stack = virtual.getStackInSlot(slot);
            if (stack.isEmpty() || itemTypes != null && !itemTypes.contains(stack.getItem())) continue;
            try {
                virtualCounts.merge(itemKey(stack), (long) stack.getCount(), Math::max);
            } catch (IllegalArgumentException ignored) {
                // BD cannot represent this item identity; do not advertise it.
            }
        }
        if (virtualCounts.isEmpty()) return;
        virtualCounts.forEach((key, count) -> counts.merge(key, count,
                RsiBeyondDimensionsSessionMixin::projectexa$saturatedAdd));

        List<StoredItem> combined = new ArrayList<>(counts.size());
        counts.forEach((key, count) -> combined.add(new StoredItem(key, count)));
        cir.setReturnValue(StorageSnapshotResult.success(
                new StorageSnapshot(snapshot.backendId(), combined, snapshot.revision())));
    }

    @Inject(method = "extractExact", at = @At("RETURN"), cancellable = true)
    private void projectexa$extractMissingFromEmc(ServerPlayer player, StorageItemKey key,
            long amount, boolean simulate, CallbackInfoReturnable<StorageOperationResult> cir) {
        if (!projectexa$isCraftingCall() || amount <= 0) return;
        StorageOperationResult nativeResult = cir.getReturnValue();
        if (nativeResult.status() != StorageOperationStatus.NOT_FOUND
                && nativeResult.status() != StorageOperationStatus.PARTIAL) return;
        long transferred = nativeResult.transferredAmount().orElse(0);
        long missing = amount - transferred;
        if (missing <= 0) return;

        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player, List.of(), Map.of());
        for (int slot = 0; slot < virtual.getSlots(); slot++) {
            ItemStack candidate = virtual.getStackInSlot(slot);
            if (candidate.isEmpty()) continue;
            try {
                if (!key.equals(itemKey(candidate))) continue;
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ItemStack extracted = virtual.extractItem(slot, (int) Math.min(missing, Integer.MAX_VALUE), simulate);
            if (extracted.isEmpty()) return;
            List<ItemStack> stacks = new ArrayList<>(nativeResult.extractedStacks());
            int maxStack = Math.max(1, extracted.getMaxStackSize());
            for (int remaining = extracted.getCount(); remaining > 0; remaining -= Math.min(remaining, maxStack)) {
                stacks.add(extracted.copyWithCount(Math.min(remaining, maxStack)));
            }
            cir.setReturnValue(StorageOperationResult.extracted(
                    simulate ? StorageOperationMode.SIMULATE : StorageOperationMode.PERFORM,
                    amount, stacks));
            return;
        }
    }

    private static long projectexa$saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static boolean projectexa$isCraftingCall() {
        // The BD session is also used by grids and transfers. Do not expose
        // transmutation knowledge to those unrelated network operations.
        return StackWalker.getInstance().walk(frames -> frames.anyMatch(frame -> {
            String name = frame.getClassName();
            return name.equals("com.huanghuang.rsintegration.crafting.MaterialSources")
                    || name.equals("com.huanghuang.rsintegration.crafting.ExtractionLedger")
                    || name.startsWith("com.huanghuang.rsintegration.crafting.batch.");
        }));
    }
}
