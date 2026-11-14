package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PacketArcaneAnvilName(String name) implements IPacket {
    @Override public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet menu
                && menu.stillValid(player) && menu.isStation(3)) {
            menu.getAnvil().setItemName(name);
        }
    }
    @Override public void encode(FriendlyByteBuf buffer) { buffer.writeUtf(name, 50); }
    public static PacketArcaneAnvilName decode(FriendlyByteBuf buffer) {
        return new PacketArcaneAnvilName(buffer.readUtf(50));
    }
}
