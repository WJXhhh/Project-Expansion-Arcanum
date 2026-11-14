package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record PacketArcaneTransmutationTabletSmallButton(Action action) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container
                && container.getPlayer() == player && container.stillValid(player) && action != null) {
            switch (action) {
                case ROTATE -> container.rotateCrafting(true);
                case ROTATE_COUNTER_CLOCKWISE -> container.rotateCrafting(false);
                case BALANCE -> container.balanceCrafting();
                case SPREAD -> container.spreadCrafting();
                case CLEAR -> container.clearCrafting(false);
                case CLEAR_FORCE -> container.clearCrafting(true);
                case STATION_CRAFTING -> container.selectStation(0);
                case STATION_SMITHING -> container.selectStation(1);
                case STATION_STONECUTTING -> container.selectStation(2);
                case STATION_ANVIL -> container.selectStation(3);
                case STATION_COLLAPSE -> container.selectStation(-1);
            }
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
    }

    public static PacketArcaneTransmutationTabletSmallButton decode(FriendlyByteBuf buffer) {
        return new PacketArcaneTransmutationTabletSmallButton(buffer.readEnum(Action.class));
    }

    public enum Action {
        ROTATE,
        ROTATE_COUNTER_CLOCKWISE,
        BALANCE,
        SPREAD,
        CLEAR,
        CLEAR_FORCE,
        STATION_CRAFTING,
        STATION_SMITHING,
        STATION_STONECUTTING,
        STATION_ANVIL,
        STATION_COLLAPSE
    }
}
