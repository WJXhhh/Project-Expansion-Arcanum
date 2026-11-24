package com.wjx.forge.projectexa.integrations.rsi;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult.Cost;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Prices the EMC allocations reported by RSI using the player's current ProjectE state. */
public final class RsiEmcEstimator {
    private static final Map<ServerPlayer, Integer> NEXT_QUOTE_TICK = new WeakHashMap<>();

    private RsiEmcEstimator() {}

    public static void handle(ServerPlayer player, PacketRsiEmcEstimate request) {
        int tick = player.getServer().getTickCount();
        if (tick < NEXT_QUOTE_TICK.getOrDefault(player, 0)) return;
        NEXT_QUOTE_TICK.put(player, tick + 5);
        PacketHandler.sendTo(quote(player, request), player);
    }

    private static PacketRsiEmcEstimateResult quote(ServerPlayer player, PacketRsiEmcEstimate request) {
        try {
            var provider = RsiEmcMaterialSource.provider(player);
            if (provider == null) return unavailable(request, PacketRsiEmcEstimateResult.HIDDEN);
            if (!request.complete()) return unavailable(request, PacketRsiEmcEstimateResult.INPUTS_UNAVAILABLE);
            List<Cost> costs = new ArrayList<>();
            for (var allocation : request.allocations()) {
                long value = RsiEmcMaterialSource.unitValue(provider, allocation.stack());
                if (value <= 0) return unavailable(request, PacketRsiEmcEstimateResult.UNAVAILABLE);
                BigInteger cost = BigInteger.valueOf(value).multiply(BigInteger.valueOf(allocation.count()));
                costs.add(new Cost(allocation.stack(), allocation.count(), cost));
            }
            return new PacketRsiEmcEstimateResult(request.requestId(), PacketRsiEmcEstimateResult.READY,
                    provider.getEmc(), List.copyOf(costs));
        } catch (RuntimeException failure) {
            Main.Logger.debug("Could not read RSI EMC quote for {}", player.getUUID(), failure);
            return unavailable(request, PacketRsiEmcEstimateResult.UNAVAILABLE);
        }
    }

    private static PacketRsiEmcEstimateResult unavailable(PacketRsiEmcEstimate request, int state) {
        return new PacketRsiEmcEstimateResult(request.requestId(), state, BigInteger.ZERO, List.of());
    }
}
