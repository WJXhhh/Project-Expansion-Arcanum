package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.integrations.rsi.RsiCompatibility;
import com.wjx.forge.projectexa.integrations.rsi.RsiPlanEmcPreview;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public record PacketRsiEmcEstimateResult(long requestId, int state, BigInteger balance,
                                         List<Cost> costs) implements IPacket {
    public static final int HIDDEN = 0;
    public static final int READY = 1;
    public static final int UNAVAILABLE = 2;
    public static final int INPUTS_UNAVAILABLE = 3;

    public record Cost(ItemStack stack, long count, BigInteger emc) {}

    @Override
    public void handle(NetworkEvent.Context context) {
        if (RsiCompatibility.isAvailable()) RsiPlanEmcPreview.acceptQuote(this);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeVarInt(state);
        buffer.writeUtf(balance.toString());
        buffer.writeVarInt(costs.size());
        for (Cost cost : costs) {
            buffer.writeItemStack(cost.stack(), false);
            buffer.writeVarLong(cost.count());
            buffer.writeUtf(cost.emc().toString());
        }
    }

    public static PacketRsiEmcEstimateResult decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        int state = buffer.readVarInt();
        BigInteger balance = new BigInteger(buffer.readUtf(1024));
        int size = buffer.readVarInt();
        if (state < HIDDEN || state > INPUTS_UNAVAILABLE || balance.signum() < 0
                || size < 0 || size > PacketRsiEmcEstimate.MAX_MATERIALS) {
            throw new DecoderException("Invalid EMC quote");
        }
        List<Cost> costs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            long count = buffer.readVarLong();
            BigInteger emc = new BigInteger(buffer.readUtf(128));
            if (stack.isEmpty() || count <= 0 || emc.signum() <= 0) throw new DecoderException("Invalid EMC cost");
            costs.add(new Cost(stack, count, emc));
        }
        return new PacketRsiEmcEstimateResult(id, state, balance, List.copyOf(costs));
    }
}
