package com.wjx.neoforge.projectexa.net.packets.to_client;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class UpdateTransmutationTargetsPacket implements IPacket {
    public static final UpdateTransmutationTargetsPacket INSTANCE = new UpdateTransmutationTargetsPacket();
    public static final CustomPacketPayload.Type<UpdateTransmutationTargetsPacket> TYPE = new CustomPacketPayload.Type<>(Main.rl("update_transmutation_targets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTransmutationTargetsPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            container.transmutationInventory.updateClientTargets(false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}