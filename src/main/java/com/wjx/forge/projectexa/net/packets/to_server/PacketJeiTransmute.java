package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval;
import com.wjx.forge.projectexa.net.JeiCreativeDelivery;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

/** All prices, knowledge and destination capacity are resolved on the server. */
public record PacketJeiTransmute(ItemStack requested, int menuId, int destination, boolean single,
                                 boolean creativeCursor, ItemStack expectedCursor, boolean pullFromRsi) implements IPacket {
    // -1: cursor, -2: inventory, 0..8: hotbar
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.containerMenu.containerId != menuId || requested.isEmpty()
                || destination < -2 || destination > 8) return;
        boolean useCreativeCursor = creativeCursor && destination == -1 && player.isCreative()
                && player.containerMenu == player.inventoryMenu;
        if (creativeCursor && !useCreativeCursor) return;
        if (pullFromRsi && ModList.get().isLoaded("rs_integration")) {
            RsiItemRetrieval.pullJei(player, requested, () -> transmute(player, useCreativeCursor));
        } else {
            transmute(player, useCreativeCursor);
        }
    }

    private boolean transmute(ServerPlayer player, boolean useCreativeCursor) {
        if (!Config.enableJeiEmcRetrieval.get() || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return false;
        if (useCreativeCursor && JeiCreativeDelivery.hasPending(player)) {
            fail(player, "pending");
            return false;
        }
        ItemStack output = Util.cleanStack(requested);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (output.isEmpty()) return false;
        if (provider == null) {
            fail(player, "provider_unavailable");
            return false;
        }
        if (!provider.hasKnowledge(output)) return false;
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0) {
            fail(player, "no_emc", output.getHoverName());
            return false;
        }
        BigInteger cost = BigInteger.valueOf(value);
        int amount = provider.getEmc().divide(cost).min(BigInteger.valueOf(single ? 1 : output.getMaxStackSize())).intValue();
        if (amount <= 0) {
            fail(player, "insufficient_emc", cost.toString());
            return false;
        }
        int delivered;
        if (useCreativeCursor) {
            ItemStack existing = expectedCursor;
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, output)) {
                fail(player, "different_item");
                return false;
            }
            int space = existing.isEmpty() ? output.getMaxStackSize() : existing.getMaxStackSize() - existing.getCount();
            delivered = Math.min(amount, Math.max(0, space));
            if (delivered == 0) {
                fail(player, "no_space");
                return false;
            }
            ItemStack result = existing.isEmpty() ? output.copyWithCount(delivered) : existing.copy();
            if (!existing.isEmpty()) result.grow(delivered);
            JeiCreativeDelivery.begin(player, output.copyWithCount(delivered), existing, result);
        } else if (destination == -2) {
            ItemStack stack = output.copyWithCount(amount);
            player.getInventory().add(stack);
            delivered = amount - stack.getCount();
        } else {
            ItemStack existing = destination == -1 ? player.containerMenu.getCarried() : player.getInventory().getItem(destination);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, output)) {
                fail(player, "different_item");
                return false;
            }
            int space = existing.isEmpty() ? output.getMaxStackSize() : existing.getMaxStackSize() - existing.getCount();
            delivered = Math.min(amount, Math.max(0, space));
            if (delivered == 0) {
                fail(player, "no_space");
                return false;
            }
            if (existing.isEmpty()) {
                if (destination == -1) player.containerMenu.setCarried(output.copyWithCount(delivered));
                else player.getInventory().setItem(destination, output.copyWithCount(delivered));
            } else existing.grow(delivered);
        }
        if (delivered == 0) {
            fail(player, "no_space");
            return false;
        }
        BigInteger totalCost = cost.multiply(BigInteger.valueOf(delivered));
        provider.setEmc(provider.getEmc().subtract(totalCost));
        provider.syncEmc(player);
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("text.projectexa.jei_transmuted",
                delivered, output.getHoverName(), totalCost.toString()));
        return true;
    }

    private static void fail(ServerPlayer player, String reason, Object... args) {
        player.sendSystemMessage(Component.translatable("text.projectexa.jei_transmute." + reason, args)
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeItem(requested);
        buffer.writeVarInt(menuId);
        buffer.writeInt(destination);
        buffer.writeBoolean(single);
        buffer.writeBoolean(creativeCursor);
        buffer.writeItem(expectedCursor);
        buffer.writeBoolean(pullFromRsi);
    }

    public static PacketJeiTransmute decode(FriendlyByteBuf buffer) {
        return new PacketJeiTransmute(buffer.readItem(), buffer.readVarInt(), buffer.readInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readItem(), buffer.readBoolean());
    }
}
