package com.wjx.forge.projectexa.net.packets.to_client;

import com.wjx.forge.projectexa.capability.CapabilityAlchemicalBookLocations;
import com.wjx.forge.projectexa.item.ItemAlchemicalBook;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.util.ClientSideHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

public record PacketOpenAlchemicalBookGUI(InteractionHand hand, List<CapabilityAlchemicalBookLocations.TeleportLocation> locations, ItemAlchemicalBook.Mode mode, boolean canEdit) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ClientSideHandler.handleAlchemicalBookOpen(this);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(hand);
        PacketSyncAlchemicalBookLocations.writeLocationsToBuffer(buf, locations);
        buf.writeEnum(mode);
        buf.writeBoolean(canEdit);
    }

    public static PacketOpenAlchemicalBookGUI decode(FriendlyByteBuf buf) {
        return new PacketOpenAlchemicalBookGUI(buf.readEnum(InteractionHand.class), PacketSyncAlchemicalBookLocations.readLocationsFromBuffer(buf), buf.readEnum(ItemAlchemicalBook.Mode.class), buf.readBoolean());
    }
}
