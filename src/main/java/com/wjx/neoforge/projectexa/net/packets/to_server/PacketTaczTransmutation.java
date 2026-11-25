package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.integrations.tacz.TaczIntegration;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PacketTaczTransmutation(int menuId, ResourceLocation recipeId, List<ItemStack> displayedIngredients)
        implements IPacket {
    public static final int MAX_INPUTS = 64;
    public static final Type<PacketTaczTransmutation> TYPE = new Type<>(Main.rl("tacz_transmutation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketTaczTransmutation> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacketTaczTransmutation::menuId,
            ResourceLocation.STREAM_CODEC, PacketTaczTransmutation::recipeId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INPUTS)), PacketTaczTransmutation::displayedIngredients,
            PacketTaczTransmutation::new);

    public PacketTaczTransmutation {
        displayedIngredients = List.copyOf(displayedIngredients);
    }

    @Override public Type<PacketTaczTransmutation> type() { return TYPE; }

    @Override
    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && ModList.get().isLoaded("tacz")) {
            TaczIntegration.transmute(player, menuId, recipeId, displayedIngredients);
        }
    }
}
