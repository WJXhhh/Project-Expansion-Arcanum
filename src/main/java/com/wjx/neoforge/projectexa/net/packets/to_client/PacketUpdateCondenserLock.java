package com.wjx.neoforge.projectexa.net.packets.to_client;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerCondenserMK3Input;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PacketUpdateCondenserLock(short windowId, @Nullable ItemInfo lockInfo) implements IPacket {
    public static final CustomPacketPayload.Type<PacketUpdateCondenserLock> TYPE = new CustomPacketPayload.Type<>(Main.rl("update_condenser_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketUpdateCondenserLock> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT, PacketUpdateCondenserLock::windowId,
            ByteBufCodecs.optional(ItemInfo.STREAM_CODEC), pkt -> Optional.ofNullable(pkt.lockInfo()),
            (windowId, lockInfo) -> new PacketUpdateCondenserLock(windowId, lockInfo.orElse(null))
    );

    @Override
    public CustomPacketPayload.Type<PacketUpdateCondenserLock> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (context.player().containerMenu instanceof ContainerCondenserMK3Input container && container.containerId == windowId) {
            container.updateLockInfo(lockInfo);
        }
    }
}
