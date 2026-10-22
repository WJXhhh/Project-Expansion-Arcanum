package com.wjx.neoforge.projectexa.net.packets.to_server;


import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.capability.CapabilityAlchemicalBookLocations;
import com.wjx.neoforge.projectexa.capability.IAlchemicalBookLocationsProvider;
import com.wjx.neoforge.projectexa.item.ItemAlchemicalBook;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import com.wjx.neoforge.projectexa.util.Lang;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.network.PEStreamCodecs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketCreateTeleportLocation(String name, Player player, InteractionHand hand) implements IPacket {
    public static final CustomPacketPayload.Type<PacketCreateTeleportLocation> TYPE = new CustomPacketPayload.Type<>(Main.rl("create_teleport_location"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCreateTeleportLocation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PacketCreateTeleportLocation::name,
            Util.PLAYER_STREAM_CODEC, PacketCreateTeleportLocation::player,
            PEStreamCodecs.INTERACTION_HAND, PacketCreateTeleportLocation::hand,
            PacketCreateTeleportLocation::new
    );

    @Override
    public void handle(IPayloadContext context) {
        ItemStack stack = player.getItemInHand(hand);
        if(stack.getItem() instanceof ItemAlchemicalBook) {
            try {
                IAlchemicalBookLocationsProvider provider = CapabilityAlchemicalBookLocations.from(stack);
                provider.ensureEditable((ServerPlayer) player);
                provider.addLocation(player, name);
                provider.sync((ServerPlayer) player);
                provider.syncToOtherPlayers();
            } catch (CapabilityAlchemicalBookLocations.BookError error) {
                player.sendSystemMessage(Lang.Items.ALCHEMICAL_BOOK_CREATE_FAILED.translateColored(ChatFormatting.RED, error.getComponent()));
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
