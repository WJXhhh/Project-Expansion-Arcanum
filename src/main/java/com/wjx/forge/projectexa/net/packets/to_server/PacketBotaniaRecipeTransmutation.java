package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.botania.BotaniaRecipeIntegration;
import com.wjx.forge.projectexa.integrations.jei.BotaniaJeiRecipeTypes;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

/** Requests conversion of missing materials for one Botania JEI recipe. */
public record PacketBotaniaRecipeTransmutation(ResourceLocation recipeTypeId,
                                                ResourceLocation recipeId) implements IPacket {
    public PacketBotaniaRecipeTransmutation {
        if (recipeTypeId == null || !BotaniaJeiRecipeTypes.isSupported(recipeTypeId)
                || recipeId == null) {
            throw new IllegalArgumentException("recipe type and id must be Botania recipe resources");
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("botania")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        BotaniaRecipeIntegration.transmute(player, recipeTypeId, recipeId);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeTypeId);
        buffer.writeResourceLocation(recipeId);
    }

    public static PacketBotaniaRecipeTransmutation decode(FriendlyByteBuf buffer) {
        return new PacketBotaniaRecipeTransmutation(
                buffer.readResourceLocation(), buffer.readResourceLocation());
    }
}
