package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval;
import com.wjx.forge.projectexa.integrations.rsi.RsiItemRetrieval.PullResult;
import com.wjx.forge.projectexa.integrations.rsi.RsiStorageCompat;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.net.packets.to_client.PacketWorldPickRsiFailure;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import java.math.BigInteger;

public record PacketWorldEmcPick(BlockPos position, boolean stack) implements IPacket {
    @Override public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.isCreative() || player.isSpectator() || player.containerMenu != player.inventoryMenu) return;
        if (!Config.enableWorldEmcPick.get() || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return;
        HitResult trace = player.pick(player.getAttributeValue(ForgeMod.BLOCK_REACH.get()), 1.0F, false);
        if (!(trace instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || !position.equals(hit.getBlockPos())) return;
        ItemStack picked = player.level().getBlockState(position).getCloneItemStack(hit, player.level(), position, player);
        if (picked.isEmpty()) return;
        Inventory inventory = player.getInventory();
        ItemStack held = player.getMainHandItem();
        boolean topUp = stack && !held.isEmpty() && held.getCount() < held.getMaxStackSize()
                && ItemStack.isSameItemSameTags(held, picked)
                && ItemStack.isSameItemSameTags(held, Util.cleanStack(picked));
        if (!topUp && inventory.findSlotMatchingItem(picked) >= 0) return;
        boolean pullFromRsi = (held.isEmpty() || topUp) && ModList.get().isLoaded("rs_integration")
                && ModList.get().isLoaded(RsiStorageCompat.RS) && RsiStorageCompat.hasRefinedStorageApi();
        if (pullFromRsi) {
            int selected = inventory.selected;
            int amount = requestedCount(picked, held, topUp);
            PullResult result = RsiItemRetrieval.pullWorldPick(player, picked, amount,
                    delivered -> deliverNetworkPick(player, selected, delivered, topUp));
            if (result == PullResult.DELIVERED) return;
            if (!result.canTransmute()) {
                player.displayClientMessage(Component.translatable("rsi.world_pick.failed", picked.getHoverName()), true);
                return;
            }
        }
        if (!transmute(player, picked, topUp) && pullFromRsi) {
            PacketHandler.sendTo(new PacketWorldPickRsiFailure(picked.copyWithCount(1)), player);
        }
    }

    private boolean transmute(ServerPlayer player, ItemStack picked, boolean topUp) {
        if (!Config.enableWorldEmcPick.get() || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return false;
        Inventory inventory = player.getInventory();
        ItemStack held = player.getMainHandItem();
        ItemStack output = Util.cleanStack(picked);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (output.isEmpty() || provider == null) {
            fail(player, "provider_unavailable");
            return false;
        }
        if (topUp && (!ItemStack.isSameItemSameTags(held, output) || !held.areCapsCompatible(output))) return false;
        if (!provider.hasKnowledge(output)) return false;
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0) { fail(player, "no_emc", output.getHoverName()); return false; }
        BigInteger unit = BigInteger.valueOf(value);
        int requested = requestedCount(output, held, topUp);
        int count = provider.getEmc().divide(unit).min(BigInteger.valueOf(requested)).intValue();
        if (count <= 0) { fail(player, "insufficient_emc", Long.toString(value)); return false; }
        int target = topUp ? inventory.selected : inventory.getSuitableHotbarSlot();
        int empty = -1;
        if (topUp) {
            held.grow(count);
        } else {
            ItemStack displaced = inventory.getItem(target);
            if (!displaced.isEmpty()) {
                for (int i = 9; i < 36; i++) if (inventory.getItem(i).isEmpty()) { empty = i; break; }
                if (empty < 0) { fail(player, "no_space"); return false; }
            }
            if (empty >= 0) inventory.setItem(empty, displaced);
            inventory.setItem(target, output.copyWithCount(count));
            inventory.selected = target;
        }
        BigInteger cost = unit.multiply(BigInteger.valueOf(count));
        provider.setEmc(provider.getEmc().subtract(cost));
        provider.syncEmc(player);
        syncDelivery(player, target, empty);
        player.sendSystemMessage(Component.translatable("text.projectexa.jei_transmuted", count, output.getHoverName(), cost.toString()));
        return true;
    }

    private int requestedCount(ItemStack template, ItemStack held, boolean topUp) {
        if (topUp) return held.getMaxStackSize() - held.getCount();
        return stack ? template.getMaxStackSize() : 1;
    }

    private static int deliverNetworkPick(ServerPlayer player, int slot, ItemStack stack, boolean topUp) {
        Inventory inventory = player.getInventory();
        if (inventory.selected != slot || player.containerMenu != player.inventoryMenu) return 0;
        ItemStack held = inventory.getItem(slot);
        int count = stack.getCount();
        if (topUp) {
            if (!ItemStack.isSameItemSameTags(held, stack) || !held.areCapsCompatible(stack)) return 0;
            count = Math.min(count, held.getMaxStackSize() - held.getCount());
            if (count <= 0) return 0;
            held.grow(count);
        } else {
            if (!held.isEmpty()) return 0;
            inventory.setItem(slot, stack);
        }
        syncDelivery(player, slot, -1);
        return count;
    }

    private static void syncDelivery(ServerPlayer player, int target, int empty) {
        Inventory inventory = player.getInventory();
        inventory.setChanged();
        if (empty >= 0) player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, empty, inventory.getItem(empty)));
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, target, inventory.getItem(target)));
        player.connection.send(new ClientboundSetCarriedItemPacket(target));
        player.inventoryMenu.broadcastChanges();
    }

    private static void fail(ServerPlayer player, String reason, Object... args) {
        player.sendSystemMessage(Component.translatable("text.projectexa.jei_transmute." + reason, args)
                .withStyle(ChatFormatting.RED));
    }
    @Override public void encode(FriendlyByteBuf buffer) { buffer.writeBlockPos(position); buffer.writeBoolean(stack); }
    public static PacketWorldEmcPick decode(FriendlyByteBuf buffer) { return new PacketWorldEmcPick(buffer.readBlockPos(), buffer.readBoolean()); }
}
