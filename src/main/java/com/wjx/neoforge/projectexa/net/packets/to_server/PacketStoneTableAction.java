package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerStoneTable;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketStoneTableAction(ContainerStoneTable.Action action, ItemStack target, boolean shiftHeld) implements IPacket {
    public static final Type<PacketStoneTableAction> TYPE = new Type<>(Main.rl("stone_table_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketStoneTableAction> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeEnum(packet.action);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.target);
                buf.writeBoolean(packet.shiftHeld);
            }, buf -> new PacketStoneTableAction(buf.readEnum(ContainerStoneTable.Action.class),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readBoolean()));

    @Override
    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ContainerStoneTable menu
                && menu.getPlayer() == player && menu.stillValid(player)) menu.handleAction(action, target, shiftHeld);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
