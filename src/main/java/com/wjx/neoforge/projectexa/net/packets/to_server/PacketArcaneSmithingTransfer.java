package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import com.wjx.neoforge.projectexa.net.packets.to_client.PacketArcaneRecipeTransferResult;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import com.wjx.neoforge.projectexa.util.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PacketArcaneSmithingTransfer(int containerId, ResourceLocation recipeId,
        List<List<ItemStack>> choices, boolean transferAll, int transferId) implements IPacket {
    public static final Type<PacketArcaneSmithingTransfer> TYPE = new Type<>(Main.rl("arcane_smithing_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneSmithingTransfer> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PacketArcaneSmithingTransfer::containerId,
            ResourceLocation.STREAM_CODEC, PacketArcaneSmithingTransfer::recipeId,
            RecipeTransferUtil.candidateListCodec(3), PacketArcaneSmithingTransfer::choices,
            ByteBufCodecs.BOOL, PacketArcaneSmithingTransfer::transferAll,
            ByteBufCodecs.VAR_INT, PacketArcaneSmithingTransfer::transferId,
            PacketArcaneSmithingTransfer::new);

    @Override public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        boolean success = transfer(player);
        if (transferId >= 0) {
            PacketDistributor.sendToPlayer(player, new PacketArcaneRecipeTransferResult(
                    transferId, player.containerMenu.containerId, success));
        }
    }

    private boolean transfer(ServerPlayer player) {
        if (player.containerMenu.containerId != containerId || !ArcaneTabletAccess.hasTablet(player)
                || choices.size() != 3) return false;
        var provider = Util.getKnowledgeProvider(player);
        if (provider == null || !provider.hasKnowledge(new ItemStack(Items.SMITHING_TABLE))) return false;
        var holder = player.server.getRecipeManager().byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof SmithingRecipe recipe)) return false;
        if (!(player.containerMenu instanceof ContainerArcaneTransmutationTablet)
                && !ArcaneTabletAccess.openFromInventory(player)) return false;
        return player.containerMenu instanceof ContainerArcaneTransmutationTablet menu
                && menu.getPlayer() == player && menu.stillValid(player)
                && menu.transferSmithingRecipe(recipe, choices, transferAll);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
