package com.wjx.neoforge.projectexa.net.packets.to_server;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.IPacket;
import com.wjx.neoforge.projectexa.net.packets.to_client.PacketArcaneRecipeTransferResult;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public record PacketArcaneTransmutationTabletRecipeTransfer(ResourceLocation recipeId, List<List<ItemStack>> recipe,
                                                            boolean transferAll, int transferId) implements IPacket {
    public PacketArcaneTransmutationTabletRecipeTransfer(ResourceLocation recipeId, List<List<ItemStack>> recipe,
                                                         boolean transferAll) {
        this(recipeId, recipe, transferAll, -1);
    }

    public static final CustomPacketPayload.Type<PacketArcaneTransmutationTabletRecipeTransfer> TYPE = new CustomPacketPayload.Type<>(Main.rl("arcane_transmutation_recipe_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketArcaneTransmutationTabletRecipeTransfer> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, PacketArcaneTransmutationTabletRecipeTransfer::recipeId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(RecipeTransferUtil.MAX_CANDIDATES_PER_SLOT))
                    .apply(ByteBufCodecs.list(9)), PacketArcaneTransmutationTabletRecipeTransfer::recipe,
            ByteBufCodecs.BOOL, PacketArcaneTransmutationTabletRecipeTransfer::transferAll,
            ByteBufCodecs.VAR_INT, PacketArcaneTransmutationTabletRecipeTransfer::transferId,
            PacketArcaneTransmutationTabletRecipeTransfer::new
    );

    @Override
    public void handle(IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        boolean success = false;
        if (player.containerMenu instanceof ContainerArcaneTransmutationTablet container) {
            success = container.onRecipeTransfer(recipeId, recipe, transferAll);
        }
        if (transferId >= 0) {
            PacketDistributor.sendToPlayer(player, new PacketArcaneRecipeTransferResult(transferId, player.containerMenu.containerId, success));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
