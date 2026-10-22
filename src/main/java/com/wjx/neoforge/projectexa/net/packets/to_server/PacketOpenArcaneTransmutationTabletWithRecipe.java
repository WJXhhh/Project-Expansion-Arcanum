package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resolve ingredients from the server's recipe manager, never from client-supplied item data. */
public record PacketOpenArcaneTransmutationTabletWithRecipe(ResourceLocation recipeId, boolean transferAll) implements IPacket {
    public static final Type<PacketOpenArcaneTransmutationTabletWithRecipe> TYPE = new Type<>(Main.rl("open_arcane_with_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenArcaneTransmutationTabletWithRecipe> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PacketOpenArcaneTransmutationTabletWithRecipe::recipeId,
            ByteBufCodecs.BOOL, PacketOpenArcaneTransmutationTabletWithRecipe::transferAll,
            PacketOpenArcaneTransmutationTabletWithRecipe::new);

    @Override
    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !ArcaneTabletAccess.hasTablet(player)) return;
        var holder = player.server.getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)
                || !recipe.canCraftInDimensions(3, 3) || recipe.getIngredients().isEmpty()
                || recipe.getIngredients().size() > 9) return;
        List<List<ItemStack>> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(new ArrayList<>());
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            int slot = recipe instanceof ShapedRecipe ? (i / width) * 3 + i % width : i;
            grid.set(slot, new ArrayList<>(Arrays.asList(recipe.getIngredients().get(i).getItems())));
        }
        if (ArcaneTabletAccess.openFromInventory(player)
                && player.containerMenu instanceof ContainerArcaneTransmutationTablet menu
                && menu.stillValid(player)) menu.onRecipeTransfer(grid, transferAll);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
