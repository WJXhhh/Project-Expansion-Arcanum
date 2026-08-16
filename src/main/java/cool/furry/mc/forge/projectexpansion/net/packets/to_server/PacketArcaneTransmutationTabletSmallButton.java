package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
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
        CLEAR_FORCE
    }
}
