package cool.furry.mc.forge.projectexpansion.net.packets.to_client;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerBase;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PacketUpdateWindowInt(short windowId, short propId, int propVal) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof ContainerBase container && player.containerMenu.containerId == windowId) {
            container.updateProgressBarInt(propId, propVal);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeShort(windowId);
        buffer.writeShort(propId);
        buffer.writeVarInt(propVal);
    }

    public static PacketUpdateWindowInt decode(FriendlyByteBuf buffer) {
        return new PacketUpdateWindowInt(buffer.readShort(), buffer.readShort(), buffer.readVarInt());
    }
}