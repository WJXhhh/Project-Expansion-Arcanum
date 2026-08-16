package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.gui.container.ContainerStoneTable;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public record PacketStoneTableAction(ContainerStoneTable.Action action, ItemStack target, boolean shiftHeld) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !(player.containerMenu instanceof ContainerStoneTable container)
                || container.getPlayer() != player || !container.stillValid(player) || action == null) {
            return;
        }
        container.handleAction(action, target, shiftHeld);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeItem(target == null ? ItemStack.EMPTY : target);
        buffer.writeBoolean(shiftHeld);
    }

    public static PacketStoneTableAction decode(FriendlyByteBuf buffer) {
        return new PacketStoneTableAction(buffer.readEnum(ContainerStoneTable.Action.class), buffer.readItem(), buffer.readBoolean());
    }
}
