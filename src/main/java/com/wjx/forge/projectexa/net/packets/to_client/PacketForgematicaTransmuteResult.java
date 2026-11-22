package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.integrations.forgematica.ForgematicaMaterialListRefresh;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

public record PacketForgematicaTransmuteResult(Component message, boolean easyPlaceResponse, boolean easyPlaceSuccess) implements IPacket {
    public PacketForgematicaTransmuteResult(Component message) {
        this(message, false, false);
    }
    @Override
    public void handle(NetworkEvent.Context context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && message != null) {
            minecraft.player.displayClientMessage(message, false);
        }
        if (easyPlaceResponse) {
            com.wjx.forge.projectexa.integrations.forgematica.ForgematicaEasyPlace.onResult(easyPlaceSuccess);
        }
        if (minecraft.screen instanceof ForgematicaMaterialListRefresh materialList) {
            materialList.projectexa$refreshAvailableMaterials();
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(message != null);
        if (message != null) buffer.writeComponent(message);
        buffer.writeBoolean(easyPlaceResponse);
        buffer.writeBoolean(easyPlaceSuccess);
    }

    public static PacketForgematicaTransmuteResult decode(FriendlyByteBuf buffer) {
        Component message = buffer.readBoolean() ? buffer.readComponent() : null;
        return new PacketForgematicaTransmuteResult(message, buffer.readBoolean(), buffer.readBoolean());
    }
}
