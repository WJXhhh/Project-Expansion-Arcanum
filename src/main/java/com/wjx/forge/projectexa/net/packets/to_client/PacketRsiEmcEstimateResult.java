package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.integrations.rsi.RsiEmcPreview;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import io.netty.handler.codec.DecoderException;
import net.minecraft.client.Minecraft;
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
    public static final int NETWORK_UNAVAILABLE = 4;
    public static final int PERMISSION_DENIED = 5;
    public static final int INVENTORY_UNAVAILABLE = 6;

    public record Cost(ItemStack stack, int count, BigInteger emc) {}

    @Override
    public void handle(NetworkEvent.Context context) {
        if (Minecraft.getInstance().screen instanceof RsiEmcPreview preview) preview.projectexa$acceptQuote(this);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeVarInt(state);
        buffer.writeUtf(balance.toString());
        buffer.writeVarInt(costs.size());
        for (Cost cost : costs) {
            buffer.writeItem(cost.stack());
            buffer.writeVarInt(cost.count());
            buffer.writeUtf(cost.emc().toString());
        }
    }

    public static PacketRsiEmcEstimateResult decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        int state = buffer.readVarInt();
        BigInteger balance = new BigInteger(buffer.readUtf(1024));
        int size = buffer.readVarInt();
        if (state < HIDDEN || state > INVENTORY_UNAVAILABLE || balance.signum() < 0
                || size < 0 || size > PacketRsiEmcEstimate.MAX_MATERIALS) {
            throw new DecoderException("Invalid EMC quote");
        }
        List<Cost> costs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            int count = buffer.readVarInt();
            BigInteger emc = new BigInteger(buffer.readUtf(128));
            if (stack.isEmpty() || count <= 0 || emc.signum() <= 0) throw new DecoderException("Invalid EMC cost");
            costs.add(new Cost(stack, count, emc));
        }
        return new PacketRsiEmcEstimateResult(id, state, balance, List.copyOf(costs));
    }
}
