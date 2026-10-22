package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.integrations.RecipeMaterialRequests;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** IDs plus one bounded contextual item; never accept a client-provided ingredient list or EMC price. */
public record PacketRecipeMaterialTransmutation(ResourceLocation category, ResourceLocation recipeId, ItemStack contextItem) implements IPacket {
    public static final Type<PacketRecipeMaterialTransmutation> TYPE = new Type<>(Main.rl("recipe_material_transmutation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRecipeMaterialTransmutation> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PacketRecipeMaterialTransmutation::category,
            ResourceLocation.STREAM_CODEC, PacketRecipeMaterialTransmutation::recipeId,
            ItemStack.OPTIONAL_STREAM_CODEC, PacketRecipeMaterialTransmutation::contextItem,
            PacketRecipeMaterialTransmutation::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    @Override public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) RecipeMaterialRequests.handle(player, category, recipeId, contextItem.copyWithCount(1));
    }
}
