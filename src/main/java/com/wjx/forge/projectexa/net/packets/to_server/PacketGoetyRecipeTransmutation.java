package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.goety.GoetyRecipeIntegration;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;

/** Requests conversion of missing materials for a non-ritual Goety JEI recipe. */
public record PacketGoetyRecipeTransmutation(ResourceLocation recipeType,
                                             @Nullable ResourceLocation recipeId,
                                             ItemStack brewingCatalyst) implements IPacket {
    public PacketGoetyRecipeTransmutation {
        if (recipeType == null || !"goety".equals(recipeType.getNamespace())) {
            throw new IllegalArgumentException("recipeType must be a Goety recipe type");
        }
        if (brewingCatalyst == null) {
            brewingCatalyst = ItemStack.EMPTY;
        } else {
            brewingCatalyst = brewingCatalyst.copyWithCount(1);
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("goety")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        GoetyRecipeIntegration.transmute(player, recipeType, recipeId, brewingCatalyst);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeType);
        buffer.writeBoolean(recipeId != null);
        if (recipeId != null) {
            buffer.writeResourceLocation(recipeId);
        }
        buffer.writeItem(brewingCatalyst);
    }

    public static PacketGoetyRecipeTransmutation decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeType = buffer.readResourceLocation();
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        return new PacketGoetyRecipeTransmutation(recipeType, recipeId, buffer.readItem());
    }
}
