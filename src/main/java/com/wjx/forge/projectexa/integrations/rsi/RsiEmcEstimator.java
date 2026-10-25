package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.RSIntegrationMod;
import com.huanghuang.rsintegration.crafting.CraftStorageEndpoints;
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
                    "Incomplete graph/legacy-step material information", null);
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
        boolean legacyInventory = RsiStorageCompat.hasLegacyInventory();
        StorageSession session = null;
        if (request.storageReference() != null) {
            try {
                StorageReference reference = StorageReferenceCodec.decode(request.storageReference()).orElseThrow();
                if (RsiStorageCompat.RS.equals(reference.backendId().value()) && legacyInventory) {
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
        if (!legacyInventory && session == null) {
            throw new QuoteUnavailable(PacketRsiEmcEstimateResult.NETWORK_UNAVAILABLE,
                    "No EMC crafting endpoint was supplied", null);
        }
        boolean networkEmc = session != null && RsiStorageCompat.hasNetworkEmc(
                session.reference().backendId().value());
        Set<net.minecraft.world.item.Item> types = new HashSet<>();
        request.demands().forEach(demand -> types.add(demand.stack().getItem()));
        Map<StorageItemKey, Long> network = new HashMap<>();
        if (session != null) {
            // Outside RSI's crafting stack: the BD Mixin does not add virtual EMC stock here.
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
            physical = RsiPhysicalInventory.snapshot(player);
        } catch (RuntimeException exception) {
            throw new QuoteUnavailable(PacketRsiEmcEstimateResult.INVENTORY_UNAVAILABLE,
                    "Player/backpack stock could not be read", exception);
        }
        Map<StorageItemKey, NetworkOffer> virtualNetwork = networkEmc
                ? networkOffers(player, session, types) : new HashMap<>();
        Map<IngredientKey, Integer> demands = new LinkedHashMap<>();
        for (var demand : request.demands()) {
            demands.merge(IngredientKey.of(demand.stack()), demand.count(),
                    (left, right) -> (int) Math.min(Integer.MAX_VALUE, (long) left + right));
        }
        List<Cost> costs = new ArrayList<>();
        for (var demand : demands.entrySet()) {
            ItemStack stack = demand.getKey().stack(1);
            long remaining = demand.getValue();
            StorageItemKey key = null;
            if (session != null) {
                key = session.itemKey(stack);
                long stock = network.getOrDefault(key, 0L);
                long used = Math.min(stock, remaining);
                remaining -= used;
                network.put(key, stock - used);
            }
            long value = ArcaneCraftingInventory.transmutationValue(player, stack);
            long converted;
            if (networkEmc) {
                // RSI's allocateExactAcrossSources takes NETWORK first. On BD-only setups that
                // includes EMC stock, so subtracting all player items first would underquote the cost.
                NetworkOffer offer = virtualNetwork.get(key);
                long fromEmc = offer == null ? 0 : Math.min(remaining, offer.available());
                remaining -= fromEmc;
                remaining -= physical.allocateUpTo(stack, remaining);
                converted = fromEmc;
                if (offer != null) {
                    value = offer.unitValue();
                    virtualNetwork.put(key, new NetworkOffer(offer.available() - fromEmc, value));
                    // Keep an unfunded shortfall visible as required EMC, rather than reporting
                    // a falsely affordable plan after the balance or physical inventory changes.
                    converted += remaining;
                }
            } else {
                // With RS's ledger Mixins active (including a selected BD network), EMC is
                // appended AFTER player/backpack items, not advertised by the BD network as well.
                remaining -= physical.allocateUpTo(stack, remaining);
                converted = legacyInventory ? remaining : 0;
            }
            if (converted > 0 && value > 0) {
                costs.add(new Cost(stack, (int) converted,
                        BigInteger.valueOf(value).multiply(BigInteger.valueOf(converted))));
            }
        }
        reply(player, request, PacketRsiEmcEstimateResult.READY, balance, List.copyOf(costs));
    }

    private static Map<StorageItemKey, NetworkOffer> networkOffers(ServerPlayer player, StorageSession session,
                                                                 Set<net.minecraft.world.item.Item> types) {
        // Use the same templates, individual balance limits and normalized identities as the BD Mixin.
        // No extraction (not even simulated extraction) is needed for this detached calculation.
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player, List.of(), Map.of());
        Map<StorageItemKey, NetworkOffer> offers = new LinkedHashMap<>();
        for (int slot = 0; slot < virtual.getSlots(); slot++) {
            ItemStack template = virtual.templateForQuote(slot);
            if (!types.contains(template.getItem())) continue;
            long value = ArcaneCraftingInventory.transmutationValue(player, template);
            if (value <= 0) continue;
            ItemStack available = virtual.getStackInSlot(slot);
            try {
                StorageItemKey key = session.itemKey(template);
                NetworkOffer previous = offers.get(key);
                long count = available.isEmpty() ? 0 : available.getCount();
                // Snapshot counts normalized aliases once (max); extraction chooses the first
                // affordable template in knowledge order. Keep that template's unit price.
                long price = previous != null && (previous.available() > 0 || count == 0)
                        ? previous.unitValue() : value;
                offers.put(key, new NetworkOffer(previous == null ? count
                        : Math.max(previous.available(), count), price));
            } catch (IllegalArgumentException ignored) {
                // The BD session Mixin omits unrepresentable ProjectE templates too.
            }
        }
        return offers;
    }

    private record NetworkOffer(long available, long unitValue) {}

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
