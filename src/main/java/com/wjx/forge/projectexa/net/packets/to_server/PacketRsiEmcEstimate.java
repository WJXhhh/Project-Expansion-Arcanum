package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.rsi.RsiCompatibility;
import com.wjx.forge.projectexa.integrations.rsi.RsiEmcEstimator;
import com.wjx.forge.projectexa.net.packets.IPacket;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** No RSI types in the wire format: RSI remains an optional dependency. */
public record PacketRsiEmcEstimate(long requestId, boolean complete, List<Allocation> allocations) implements IPacket {
    public static final int MAX_MATERIALS = 4096;

    public record Allocation(ItemStack stack, long count) {}

    @Override
    public void handle(NetworkEvent.Context context) {
        if (context.getSender() != null && RsiCompatibility.isAvailable()) {
            RsiEmcEstimator.handle(context.getSender(), this);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeBoolean(complete);
        buffer.writeVarInt(allocations.size());
        for (Allocation allocation : allocations) {
            buffer.writeItemStack(allocation.stack(), false);
            buffer.writeVarLong(allocation.count());
        }
    }

    public static PacketRsiEmcEstimate decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        boolean complete = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_MATERIALS) throw new DecoderException("Too many EMC materials");
        List<Allocation> allocations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            long count = buffer.readVarLong();
            if (stack.isEmpty() || count <= 0) throw new DecoderException("Invalid EMC allocation");
            allocations.add(new Allocation(stack.copyWithCount(1), count));
        }
        return new PacketRsiEmcEstimate(id, complete, List.copyOf(allocations));
    }
}
