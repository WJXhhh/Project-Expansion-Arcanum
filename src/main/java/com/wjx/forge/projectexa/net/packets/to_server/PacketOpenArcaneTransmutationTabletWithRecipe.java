package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.item.ItemArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;

/** Opens the tablet from a player inventory and transfers a JEI recipe into it. */
public record PacketOpenArcaneTransmutationTabletWithRecipe(List<List<ItemStack>> recipe, boolean transferAll)
        implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !PacketOpenArcaneTransmutationTablet.hasTablet(player)
                || !PacketArcaneTransmutationTabletRecipeTransfer.validRecipe(recipe)) {
            return;
        }

        ItemArcaneTransmutationTablet.openFromInventory(player);
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container
                && container.getPlayer() == player && container.stillValid(player)) {
            container.onRecipeTransfer(recipe, transferAll);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        PacketArcaneTransmutationTabletRecipeTransfer.writeRecipe(buffer, recipe);
        buffer.writeBoolean(transferAll);
    }

    public static PacketOpenArcaneTransmutationTabletWithRecipe decode(FriendlyByteBuf buffer) {
        return new PacketOpenArcaneTransmutationTabletWithRecipe(
                PacketArcaneTransmutationTabletRecipeTransfer.readRecipe(buffer), buffer.readBoolean());
    }
}
