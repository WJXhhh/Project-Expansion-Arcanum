package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.goety.GoetyRitualIntegration;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

/** Requests conversion of missing materials for one Goety ritual recipe. */
public record PacketGoetyRitualTransmutation(ResourceLocation recipeId) implements IPacket {
    public PacketGoetyRitualTransmutation {
        if (recipeId == null) {
            throw new IllegalArgumentException("recipeId cannot be null");
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("goety")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        GoetyRitualIntegration.transmute(player, recipeId);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeId);
    }

    public static PacketGoetyRitualTransmutation decode(FriendlyByteBuf buffer) {
        return new PacketGoetyRitualTransmutation(buffer.readResourceLocation());
    }
}
