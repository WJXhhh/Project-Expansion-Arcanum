package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.arsnouveau.ArsNouveauRecipeIntegration;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

/** Requests conversion of missing materials for one Ars Nouveau JEI recipe. */
public record PacketArsNouveauRecipeTransmutation(ResourceLocation recipeTypeId,
                                                   ResourceLocation recipeId) implements IPacket {
    public PacketArsNouveauRecipeTransmutation {
        if (recipeTypeId == null || !"ars_nouveau".equals(recipeTypeId.getNamespace())
                || recipeId == null || !"ars_nouveau".equals(recipeId.getNamespace())) {
            throw new IllegalArgumentException("recipe type and id must be Ars Nouveau resources");
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("ars_nouveau")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        ArsNouveauRecipeIntegration.transmute(player, recipeTypeId, recipeId);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeTypeId);
        buffer.writeResourceLocation(recipeId);
    }

    public static PacketArsNouveauRecipeTransmutation decode(FriendlyByteBuf buffer) {
        return new PacketArsNouveauRecipeTransmutation(
                buffer.readResourceLocation(), buffer.readResourceLocation());
    }
}
