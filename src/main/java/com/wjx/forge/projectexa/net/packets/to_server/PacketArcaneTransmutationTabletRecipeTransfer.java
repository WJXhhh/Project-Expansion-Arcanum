package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

public record PacketArcaneTransmutationTabletRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && player.containerMenu instanceof ContainerArcaneTransmutationTablet container
                && container.getPlayer() == player && container.stillValid(player)
                && validRecipe(recipe)) {
            container.onRecipeTransfer(recipe, transferAll);
        }
    }

    private static boolean validRecipe(List<List<ItemStack>> recipe) {
        if (recipe == null || recipe.size() > 9) return false;
        for (List<ItemStack> possibilities : recipe) {
            if (possibilities == null || possibilities.size() > 64) return false;
            for (ItemStack stack : possibilities) {
                if (stack == null) return false;
            }
        }
        return true;
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(Math.min(recipe.size(), 9));
        for (int i = 0; i < Math.min(recipe.size(), 9); i++) {
            List<ItemStack> possibilities = recipe.get(i);
            buffer.writeVarInt(Math.min(possibilities.size(), 64));
            for (int j = 0; j < Math.min(possibilities.size(), 64); j++) buffer.writeItem(possibilities.get(j));
        }
        buffer.writeBoolean(transferAll);
    }

    public static PacketArcaneTransmutationTabletRecipeTransfer decode(FriendlyByteBuf buffer) {
        int slots = Math.max(0, Math.min(buffer.readVarInt(), 9));
        List<List<ItemStack>> recipe = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            int possibilitiesCount = Math.max(0, Math.min(buffer.readVarInt(), 64));
            List<ItemStack> possibilities = new ArrayList<>(possibilitiesCount);
            for (int j = 0; j < possibilitiesCount; j++) possibilities.add(buffer.readItem());
            recipe.add(possibilities);
        }
        return new PacketArcaneTransmutationTabletRecipeTransfer(recipe, buffer.readBoolean());
    }
}
