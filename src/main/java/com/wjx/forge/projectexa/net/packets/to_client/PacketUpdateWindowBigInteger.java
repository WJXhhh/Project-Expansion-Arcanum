package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.gui.container.ContainerBase;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

public record PacketUpdateWindowBigInteger(short windowId, short propId, BigInteger propVal) implements IPacket {

    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarBigInteger(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(windowId);
        buffer.writeShort(propId);
        buffer.writeUtf(propVal.toString());
    }

    public static PacketUpdateWindowBigInteger decode(FriendlyByteBuf buffer) {
        return new PacketUpdateWindowBigInteger(buffer.readShort(), buffer.readShort(), new BigInteger(buffer.readUtf()));
    }
}