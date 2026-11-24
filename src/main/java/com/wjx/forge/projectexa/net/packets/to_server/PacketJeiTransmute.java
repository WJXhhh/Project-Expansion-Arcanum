package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.integrations.rsi.RsiCompatibility;
import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval;
import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval.PullResult;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

/** All prices, knowledge and destination capacity are resolved on the server. */
public record PacketJeiTransmute(ItemStack requested, int menuId, int destination, boolean single,
                                 boolean creativeCursor, ItemStack expectedCursor, boolean pullFromRsi) implements IPacket {
    // -1: cursor, -2: inventory, 0..8: hotbar
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.isSpectator() || player.containerMenu.containerId != menuId || requested.isEmpty()
                || destination < -2 || destination > 8) return;
        boolean useCreativeCursor = creativeCursor && destination == -1 && player.isCreative()
                && player.containerMenu == player.inventoryMenu;
        if (creativeCursor && !useCreativeCursor) return;
        if (!Config.enableJeiEmcRetrieval.get() || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        if (useCreativeCursor && JeiCreativeDelivery.hasPending(player)) {
            fail(player, "pending");
            return;
        }
        if (pullFromRsi && RsiCompatibility.isAvailable()) {
            PullResult result = useCreativeCursor
                    ? RsiItemRetrieval.pullCreativeCursor(player, requested,
                            Math.min(single ? 1 : requested.getMaxStackSize(), slotSpace(expectedCursor, requested)),
                            stack -> deliver(player, stack, true))
                    : RsiItemRetrieval.pullJei(player, requested, single, menuId, destination);
            if (result == PullResult.DELIVERED) {
                player.containerMenu.broadcastChanges();
                return;
            }
            if (result.canTransmute() && transmute(player, useCreativeCursor)) return;
            String failureKey = switch (result) {
                case NO_NETWORK -> "rsi.jei.pull.no_network";
                case NO_PERMISSION -> "rsi.jei.pull.no_permission";
                case NO_SPACE -> "text.projectexa.jei_transmute.no_space";
                case NOT_FOUND -> "rsi.jei.pull.not_found";
                default -> "rsi.jei.pull.failed";
            };
            player.sendSystemMessage(Component.translatable(failureKey, requested.getHoverName()));
        } else {
            transmute(player, useCreativeCursor);
        }
    }

    private boolean transmute(ServerPlayer player, boolean useCreativeCursor) {
        if (!Config.enableJeiEmcRetrieval.get() || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return false;
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
        if (destination != -2 && !canStack(destinationStack(player, useCreativeCursor), output)) {
            fail(player, "different_item");
            return false;
        }
        int delivered = deliver(player, output.copyWithCount(amount), useCreativeCursor);
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

    private int deliver(ServerPlayer player, ItemStack stack, boolean useCreativeCursor) {
        if (player.containerMenu.containerId != menuId) return 0;
        if (destination == -2) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    new PlayerMainInvWrapper(player.getInventory()), stack.copy(), false);
            return stack.getCount() - remainder.getCount();
        }
        if (useCreativeCursor && (!player.isCreative() || player.containerMenu != player.inventoryMenu
                || JeiCreativeDelivery.hasPending(player))) return 0;

        ItemStack existing = destinationStack(player, useCreativeCursor);
        int count = Math.min(stack.getCount(), slotSpace(existing, stack));
        if (count <= 0) return 0;

        ItemStack delivered = stack.copyWithCount(count);
        ItemStack result = existing.isEmpty() ? delivered : existing.copyWithCount(existing.getCount() + count);
        if (useCreativeCursor) {
            JeiCreativeDelivery.begin(player, delivered, existing, result);
        } else if (destination == -1) {
            player.containerMenu.setCarried(result);
        } else {
            player.getInventory().setItem(destination, result);
        }
        return count;
    }

    private ItemStack destinationStack(ServerPlayer player, boolean useCreativeCursor) {
        if (useCreativeCursor) return expectedCursor;
        return destination == -1 ? player.containerMenu.getCarried() : player.getInventory().getItem(destination);
    }

    private static int slotSpace(ItemStack existing, ItemStack stack) {
        if (existing.isEmpty()) return stack.getMaxStackSize();
        return canStack(existing, stack) ? Math.max(0, existing.getMaxStackSize() - existing.getCount()) : 0;
    }

    private static boolean canStack(ItemStack existing, ItemStack stack) {
        return existing.isEmpty() || ItemStack.isSameItemSameTags(existing, stack) && existing.areCapsCompatible(stack);
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
