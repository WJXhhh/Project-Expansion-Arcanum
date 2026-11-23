package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.RSIntegrationMod;
import com.huanghuang.rsintegration.crafting.MaterialSourceSelection;
import com.huanghuang.rsintegration.crafting.endpoint.CraftStorageEndpoints;
import com.huanghuang.rsintegration.crafting.tree.IngredientKey;
import com.huanghuang.rsintegration.storage.*;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult.Cost;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.*;

/** Main-thread, read-only estimate. Never reserves, extracts or changes EMC. */
public final class RsiEmcEstimator {
    private static final Map<ServerPlayer, Integer> NEXT_QUOTE_TICK = new WeakHashMap<>();
    private static final Map<ServerPlayer, Integer> LAST_FAILURE = new WeakHashMap<>();

    private RsiEmcEstimator() {}

    public static void handle(ServerPlayer player, PacketRsiEmcEstimate request) {
        int tick = player.getServer().getTickCount();
        if (tick < NEXT_QUOTE_TICK.getOrDefault(player, 0)) return;
        NEXT_QUOTE_TICK.put(player, tick + 5);
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            reply(player, request, PacketRsiEmcEstimateResult.HIDDEN, BigInteger.ZERO, List.of());
            return;
        }
        BigInteger balance = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                .map(provider -> provider.getEmc()).orElse(BigInteger.ZERO);
        if (!request.inputsAvailable()) {
            unavailable(player, request, PacketRsiEmcEstimateResult.INPUTS_UNAVAILABLE, balance,
                    "Incomplete initial material information", null);
            return;
        }
        if (request.demands().isEmpty()) {
            reply(player, request, PacketRsiEmcEstimateResult.READY, balance, List.of());
            return;
        }
        try {
            quote(player, request, balance);
        } catch (QuoteUnavailable exception) {
            unavailable(player, request, exception.state, balance, exception.getMessage(), exception);
        } catch (RuntimeException exception) {
            // A disappearing/inaccessible backend is unknown, not a zero-cost plan.
            unavailable(player, request, PacketRsiEmcEstimateResult.UNAVAILABLE, balance,
                    "Unexpected quote failure", exception);
        }
    }

    private static void quote(ServerPlayer player, PacketRsiEmcEstimate request, BigInteger balance) {
        MaterialSourceSelection sources = MaterialSourceSelection.fromMask(request.sourceMask());
        StorageSession session = null;
        if (sources.storageNetwork() && request.storageReference() != null) {
            try {
                StorageReference reference = StorageReferenceCodec.decode(request.storageReference()).orElseThrow();
                if (RsiStorageCompat.RS.equals(reference.backendId().value())
                        && RsiStorageCompat.hasRefinedStorageApi()) {
                    session = CraftStorageEndpoints.resolve(reference, player).orElseThrow().session();
                } else {
                    // Resolve the selected backend, not whichever mod happens to be installed.
                    // BD also uses this branch when RS is installed alongside it.
                    session = RSIntegrationMod.STORAGE_BACKENDS.registry().resolve(reference, player)
                            .session().orElseThrow();
                }
            } catch (RuntimeException exception) {
                throw new QuoteUnavailable(PacketRsiEmcEstimateResult.NETWORK_UNAVAILABLE,
                        "Selected storage session could not be resolved", exception);
            }
            if (!session.hasPermission(player, StoragePermission.VIEW)
                    || !session.hasPermission(player, StoragePermission.EXTRACT)) {
                throw new QuoteUnavailable(PacketRsiEmcEstimateResult.PERMISSION_DENIED,
                        "Selected storage session did not grant view/extract access", null);
            }
        }
        if (!sources.backpacks()) {
            reply(player, request, PacketRsiEmcEstimateResult.READY, balance, List.of());
            return;
        }
        Set<Item> types = new HashSet<>();
        request.demands().forEach(demand -> types.add(demand.stack().getItem()));
        Map<StorageItemKey, Long> network = new HashMap<>();
        if (session != null) {
            StorageSnapshot snapshot;
            try {
                StorageSnapshotResult result = session.snapshotItems(player, types);
                snapshot = result.snapshot().orElseThrow(() -> new IllegalStateException(
                        "Storage snapshot status: " + result.status()));
            } catch (RuntimeException exception) {
                throw new QuoteUnavailable(PacketRsiEmcEstimateResult.NETWORK_UNAVAILABLE,
                        "Selected storage snapshot could not be read", exception);
            }
            for (StoredItem stored : snapshot.items()) {
                network.merge(stored.key(), stored.amount(), RsiEmcEstimator::saturatedAdd);
            }
        }
        RsiPhysicalInventory physical;
        try {
            physical = RsiPhysicalInventory.snapshot(player, sources);
        } catch (RuntimeException exception) {
            throw new QuoteUnavailable(PacketRsiEmcEstimateResult.INVENTORY_UNAVAILABLE,
                    "Player/backpack stock could not be read", exception);
        }
        Map<IngredientKey, Integer> demands = new LinkedHashMap<>();
        for (var demand : request.demands()) {
            demands.merge(IngredientKey.of(demand.stack()), demand.count(),
                    (left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
        }
        List<Cost> costs = new ArrayList<>();
        for (var demand : demands.entrySet()) {
            ItemStack stack = demand.getKey().stack(1);
            long remaining = demand.getValue();
            if (session != null) {
                try {
                    StorageItemKey key = session.itemKey(stack);
                    long stock = network.getOrDefault(key, 0L);
                    long used = Math.min(stock, remaining);
                    remaining -= used;
                    network.put(key, stock - used);
                } catch (IllegalArgumentException ignored) {
                    // The ledger can still supply a stack that the selected network cannot represent.
                }
            }
            // The ledger uses EMC after the selected network and physical inventory sources.
            remaining -= physical.allocateUpTo(stack, remaining);
            if (remaining == 0) continue;
            long value = ArcaneCraftingInventory.transmutationValue(player, stack);
            if (value > 0) {
                costs.add(new Cost(stack, (int) remaining,
                        BigInteger.valueOf(value).multiply(BigInteger.valueOf(remaining))));
            }
        }
        reply(player, request, PacketRsiEmcEstimateResult.READY, balance, List.copyOf(costs));
    }

    private static final class QuoteUnavailable extends RuntimeException {
        private final int state;

        private QuoteUnavailable(int state, String message, Throwable cause) {
            super(message, cause);
            this.state = state;
        }
    }

    private static void unavailable(ServerPlayer player, PacketRsiEmcEstimate request, int state,
                                    BigInteger balance, String reason, RuntimeException exception) {
        Integer previous = LAST_FAILURE.put(player, state);
        if (!Objects.equals(previous, state)) {
            // Log a failure transition once, not a stack trace for every one-second preview refresh.
            if (exception == null) {
                Main.Logger.debug("[RSI-EMC] Quote unavailable: reason={}, player={}, request={}",
                        reason, player.getUUID(), request.requestId());
            } else {
                Main.Logger.warn("[RSI-EMC] Quote unavailable: reason={}, player={}, request={}",
                        reason, player.getUUID(), request.requestId(), exception);
            }
        }
        reply(player, request, state, balance, List.of());
    }

    private static long saturatedAdd(long left, long right) {
        return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    private static void reply(ServerPlayer player, PacketRsiEmcEstimate request, int state,
                              BigInteger balance, List<Cost> costs) {
        if (state == PacketRsiEmcEstimateResult.READY || state == PacketRsiEmcEstimateResult.HIDDEN) {
            LAST_FAILURE.remove(player);
        }
        PacketHandler.sendTo(new PacketRsiEmcEstimateResult(request.requestId(), state, balance, costs), player);
    }
}
