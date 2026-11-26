package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.IntegrationResourceKey;
import com.huanghuang.rsintegration.api.RSIItemRetrieval;
import com.huanghuang.rsintegration.api.RSIItemRetrieval.Cursor;
import com.huanghuang.rsintegration.api.RSIItemRetrieval.Fallback;
import com.huanghuang.rsintegration.api.RSIItemRetrieval.Intent;
import com.huanghuang.rsintegration.api.RSIItemRetrieval.SelectedHotbar;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Delivered;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Destination;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Request;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Result;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Scene;
import com.huanghuang.rsintegration.crafting.endpoint.CraftStorageEndpoint;
import com.huanghuang.rsintegration.storage.StorageRestockSupport;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.math.BigInteger;
import java.util.function.ToIntFunction;

public final class RsiItemRetrieval {
    private RsiItemRetrieval() {}

    public static void register() {
        RSIItemRetrievalFallback.register(Main.rl("emc_retrieval"), RsiItemRetrieval::tryDeliver);
    }

    public enum PullResult {
        DELIVERED, NO_NETWORK, NO_PERMISSION, NO_SPACE, NOT_FOUND, FAILED;

        public boolean canTransmute() {
            return this != DELIVERED && this != FAILED;
        }
    }

    public static PullResult pullJei(ServerPlayer player, ItemStack stack, boolean single, int menuId, int slot) {
        RSIItemRetrieval.Destination target = switch (slot) {
            case -2 -> new RSIItemRetrieval.Inventory();
            case -1 -> new Cursor(menuId);
            default -> new SelectedHotbar(slot);
        };
        return retrieve(new RSIItemRetrieval.Request(player, stack, single ? 1 : stack.getMaxStackSize(),
                RSIItemRetrieval.Scene.JEI, single ? Intent.SINGLE : Intent.STACK, target, Fallback.DISABLED));
    }

    public static PullResult pullWorldPick(ServerPlayer player, ItemStack stack, int amount, int slot,
                                           boolean wholeStack, boolean topUp) {
        Intent intent = topUp ? Intent.TOP_UP : wholeStack ? Intent.STACK : Intent.SINGLE;
        return retrieve(new RSIItemRetrieval.Request(player, stack, amount, RSIItemRetrieval.Scene.WORLD_PICK,
                intent, new SelectedHotbar(slot), Fallback.DISABLED));
    }

    private static PullResult retrieve(RSIItemRetrieval.Request request) {
        var result = RSIItemRetrieval.retrieve(request);
        if (result.deliveredCount() > 0) return PullResult.DELIVERED;
        if (!result.confirmedZeroDelivery()) return PullResult.FAILED;
        return switch (result.status()) {
            case NO_NETWORK -> PullResult.NO_NETWORK;
            case NO_PERMISSION -> PullResult.NO_PERMISSION;
            case NO_SPACE -> PullResult.NO_SPACE;
            case OUT_OF_STOCK -> PullResult.NOT_FOUND;
            default -> PullResult.FAILED;
        };
    }

    /** The creative inventory cursor is client-owned and uses ProjectExA's delivery acknowledgement. */
    public static PullResult pullCreativeCursor(ServerPlayer player, ItemStack stack, int amount,
                                                ToIntFunction<ItemStack> deliver) {
        if (amount <= 0) return PullResult.NO_SPACE;
        ItemStack template = stack.copyWithCount(1);
        try {
            CraftStorageEndpoint endpoint = StorageRestockSupport.resolve(player).orElse(null);
            if (endpoint == null) return PullResult.NO_NETWORK;
            if (!StorageRestockSupport.canExtract(endpoint, player)) return PullResult.NO_PERMISSION;

            var extraction = StorageRestockSupport.extractOutcome(endpoint, player, template, amount);
            ItemStack extracted = extraction.stack();
            if (extracted.isEmpty()) {
                return extraction.confirmedMissing() ? PullResult.NOT_FOUND : PullResult.FAILED;
            }
            int delivered = deliver.applyAsInt(extracted.copyWithCount(Math.min(amount, extracted.getCount())));
            if (delivered < extracted.getCount()) {
                returnToNetwork(endpoint, player, extracted.copyWithCount(extracted.getCount() - delivered));
            }
            return delivered > 0 ? PullResult.DELIVERED : PullResult.FAILED;
        } catch (RuntimeException | LinkageError exception) {
            // Failed calls may already have changed storage or delivered items.
            Main.Logger.warn("RSI creative cursor retrieval failed for {}", player.getGameProfile().getName(), exception);
            return PullResult.FAILED;
        }
    }

    private static void returnToNetwork(CraftStorageEndpoint endpoint, ServerPlayer player, ItemStack stack) {
        var remainder = endpoint.insert(player, stack, false).remainder();
        if (remainder.isEmpty()) {
            Main.Logger.warn("RSI could not confirm the return of {} for {}", stack, player.getGameProfile().getName());
            return;
        }
        ItemStack unreturned = remainder.get();
        if (!unreturned.isEmpty()) player.drop(unreturned, false);
    }

    private static Result tryDeliver(Request request) {
        ServerPlayer player = request.player();
        Inventory inventory = player.getInventory();
        boolean worldPick = request.scene() == Scene.WORLD_PICK;
        if (worldPick) {
            if (!Config.enableWorldEmcPick.get() || request.destination() != Destination.SELECTED_HOTBAR_SLOT
                    || player.isCreative() || player.isSpectator() || player.containerMenu != player.inventoryMenu
                    || inventory.selected != request.destinationIndex()
                    || !inventory.getSelected().isEmpty()) return Result.PASS;
        } else if (request.scene() != Scene.JEI || !Config.enableJeiEmcRetrieval.get()
                || request.destination() != Destination.PLAYER_INVENTORY || player.isSpectator()) {
            return Result.PASS;
        }
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(player)) return Result.PASS;

        ItemStack target = request.target();
        ItemStack output = Util.cleanStack(target);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (output.isEmpty() || !target.areCapsCompatible(output)
                || !IntegrationResourceKey.item(target).equals(IntegrationResourceKey.item(output))
                || provider == null || !provider.hasKnowledge(output)) return Result.PASS;
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0) return Result.PASS;
        BigInteger balance = provider.getEmc();
        BigInteger price = BigInteger.valueOf(value);
        int limit = Math.min(request.requestedCount(), output.getMaxStackSize());
        int amount = balance.divide(price).min(BigInteger.valueOf(limit)).intValue();
        if (amount <= 0) return Result.PASS;

        ItemStack delivery = output.copyWithCount(amount);
        int delivered = amount;
        if (worldPick) {
            inventory.setItem(inventory.selected, delivery);
        } else {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(inventory), delivery, false);
            delivered -= remainder.getCount();
            if (delivered == 0) return Result.PASS;
        }

        BigInteger cost = price.multiply(BigInteger.valueOf(delivered));
        provider.setEmc(balance.subtract(cost));
        provider.syncEmc(player);
        player.sendSystemMessage(Component.translatable("text.projectexa.jei_transmuted",
                delivered, output.getHoverName(), cost.toString()));
        return new Delivered(delivered);
    }
}
