package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.rsi.RsiEmcEstimator;
import com.wjx.forge.projectexa.net.packets.IPacket;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** No RSI types in the wire format: RSI remains an optional dependency. */
public record PacketRsiEmcEstimate(long requestId, CompoundTag storageReference,
                                   boolean inputsAvailable, List<Demand> demands) implements IPacket {
    public static final int MAX_MATERIALS = 4096;

    public record Demand(ItemStack stack, int count) {}

    @Override
    public void handle(NetworkEvent.Context context) {
        if (context.getSender() != null && ModList.get().isLoaded("rs_integration")) {
            RsiEmcEstimator.handle(context.getSender(), this);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(requestId);
        buffer.writeNbt(storageReference);
        buffer.writeBoolean(inputsAvailable);
        buffer.writeVarInt(demands.size());
        for (Demand demand : demands) {
            buffer.writeItem(demand.stack());
            buffer.writeVarInt(demand.count());
        }
    }

    public static PacketRsiEmcEstimate decode(FriendlyByteBuf buffer) {
        long id = buffer.readLong();
        CompoundTag reference = buffer.readNbt();
        boolean inputsAvailable = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_MATERIALS) throw new DecoderException("Too many EMC materials");
        List<Demand> demands = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            int count = buffer.readVarInt();
            if (stack.isEmpty() || count <= 0) throw new DecoderException("Invalid EMC demand");
            demands.add(new Demand(stack.copyWithCount(1), count));
        }
        return new PacketRsiEmcEstimate(id, reference, inputsAvailable, List.copyOf(demands));
    }
}
