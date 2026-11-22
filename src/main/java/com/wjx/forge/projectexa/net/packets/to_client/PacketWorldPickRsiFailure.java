package com.wjx.forge.projectexa.net.packets.to_client;

import com.huanghuang.rsintegration.client.recipe.RecipeBrowserBridge;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

public record PacketWorldPickRsiFailure(ItemStack stack) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        var player = Minecraft.getInstance().player;
        if (player == null || stack.isEmpty() || !ModList.get().isLoaded("rs_integration")) return;
        boolean bookmarked = RecipeBrowserBridge.addFavorite(stack) != RecipeBrowserBridge.FavoriteResult.UNAVAILABLE;
        player.displayClientMessage(Component.translatable(
                bookmarked ? "rsi.world_pick.bookmarked" : "rsi.world_pick.bookmark_unavailable", stack.getHoverName()), true);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(stack);
    }

    public static PacketWorldPickRsiFailure decode(FriendlyByteBuf buffer) {
        return new PacketWorldPickRsiFailure(buffer.readItem());
    }
}
