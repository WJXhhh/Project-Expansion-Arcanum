package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.tacz.TaczIntegration;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

/** Requests transmutation of the concrete materials currently shown by a TACZ recipe. */
public record PacketTaczTransmutation(ResourceLocation recipeId, List<ItemStack> displayedIngredients) implements IPacket {
    private static final int MAX_INPUTS = 64;

    public PacketTaczTransmutation {
        if (recipeId == null) throw new IllegalArgumentException("recipeId cannot be null");
        if (displayedIngredients == null || displayedIngredients.size() > MAX_INPUTS) {
            throw new IllegalArgumentException("Too many TACZ recipe inputs");
        }
        displayedIngredients = List.copyOf(displayedIngredients);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("tacz")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        TaczIntegration.transmute(player, recipeId, displayedIngredients);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeId);
        buffer.writeVarInt(displayedIngredients.size());
        for (ItemStack stack : displayedIngredients) {
            buffer.writeItem(stack);
        }
    }

    public static PacketTaczTransmutation decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        int inputCount = buffer.readVarInt();
        if (inputCount < 0 || inputCount > MAX_INPUTS) {
            throw new IllegalArgumentException("Invalid TACZ recipe input count: " + inputCount);
        }

        List<ItemStack> displayedIngredients = new ArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            displayedIngredients.add(buffer.readItem());
        }
        return new PacketTaczTransmutation(recipeId, displayedIngredients);
    }
}
