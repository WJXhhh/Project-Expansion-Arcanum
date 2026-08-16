package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.gui.container.ContainerBase;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PacketUpdateWindowLong(short windowId, short propId, long propVal) implements IPacket {

    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarLong(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(windowId);
        buffer.writeShort(propId);
        buffer.writeLong(propVal);
    }

    public static PacketUpdateWindowLong decode(FriendlyByteBuf buffer) {
        return new PacketUpdateWindowLong(buffer.readShort(), buffer.readShort(), buffer.readLong());
    }
}