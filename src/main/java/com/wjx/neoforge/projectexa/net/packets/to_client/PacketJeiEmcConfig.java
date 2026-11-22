package com.wjx.neoforge.projectexa.net.packets.to_client;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.config.JeiEmcClientState;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketJeiEmcConfig(boolean enabled, boolean worldPickEnabled, boolean allowLargeRecipeTransferCandidates) implements IPacket {
    public static final Type<PacketJeiEmcConfig> TYPE = new Type<>(Main.rl("jei_emc_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketJeiEmcConfig> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PacketJeiEmcConfig::enabled,
            ByteBufCodecs.BOOL, PacketJeiEmcConfig::worldPickEnabled,
            ByteBufCodecs.BOOL, PacketJeiEmcConfig::allowLargeRecipeTransferCandidates,
            PacketJeiEmcConfig::new);

    @Override public void handle(IPayloadContext context) {
        JeiEmcClientState.setEnabled(enabled);
        JeiEmcClientState.setWorldPickEnabled(worldPickEnabled);
        JeiEmcClientState.setAllowLargeRecipeTransferCandidates(allowLargeRecipeTransferCandidates);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
