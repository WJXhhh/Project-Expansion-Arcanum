package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.IntegrationResourceKey;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Delivered;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Destination;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Request;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Result;
import com.huanghuang.rsintegration.api.RSIItemRetrievalFallback.Scene;
import com.huanghuang.rsintegration.mods.jei.JeiStoragePullPacket;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.mixin.RsiJeiStoragePullInvoker;
import com.wjx.forge.projectexa.mixin.RsiWorldPickInvoker;
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
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class RsiItemRetrieval {
    private static final ThreadLocal<NativeAttempt> NATIVE_ATTEMPT = new ThreadLocal<>();

    private RsiItemRetrieval() {}

    public static void register() {
        RSIItemRetrievalFallback.register(Main.rl("emc_retrieval"), RsiItemRetrieval::tryDeliver);
    }

    public static void pullJei(ServerPlayer player, ItemStack stack, BooleanSupplier transmute) {
        NativeAttempt attempt = new NativeAttempt(player, Scene.JEI);
        Component failure = runNative(attempt, () -> {
            RsiJeiStoragePullInvoker.projectexa$handlePull(new JeiStoragePullPacket(stack), player);
            return attempt.failure;
        });
        if (failure != null && (!attempt.canTransmute || !transmute.getAsBoolean())) {
            player.sendSystemMessage(failure);
        }
    }

    public static boolean captureJeiFailure(ServerPlayer player, String key, Object[] args) {
        NativeAttempt attempt = NATIVE_ATTEMPT.get();
        if (attempt == null || attempt.player != player || attempt.scene != Scene.JEI) return false;
        attempt.failure = Component.translatable(key, args);
        attempt.canTransmute = switch (key) {
            case "rsi.jei.pull.no_network", "rsi.jei.pull.no_permission",
                 "rsi.jei.pull.inventory_full", "rsi.jei.pull.not_found" -> true;
            default -> false;
        };
        return true;
    }

    public static boolean tryWorldPick(ServerPlayer player, ItemStack stack) {
        try {
            return runNative(new NativeAttempt(player, Scene.WORLD_PICK),
                    () -> !RsiWorldPickInvoker.projectexa$pickBlock(player, stack).isEmpty());
        } catch (RuntimeException exception) {
            // An extraction that threw may already have affected storage or the selected slot.
            Main.Logger.warn("RSI world pick failed for {}", player.getGameProfile().getName(), exception);
            player.displayClientMessage(Component.translatable("rsi.world_pick.failed", stack.getHoverName()), true);
            return true;
        }
    }

    private static <T> T runNative(NativeAttempt attempt, Supplier<T> action) {
        NativeAttempt previous = NATIVE_ATTEMPT.get();
        NATIVE_ATTEMPT.set(attempt);
        try {
            return action.get();
        } finally {
            if (previous == null) NATIVE_ATTEMPT.remove();
            else NATIVE_ATTEMPT.set(previous);
        }
    }

    private static Result tryDeliver(Request request) {
        ServerPlayer player = request.player();
        NativeAttempt attempt = NATIVE_ATTEMPT.get();
        // The coordinated packet already has its own fallback, with the original delivery options.
        if (attempt != null && attempt.player == player && attempt.scene == request.scene()) return Result.PASS;
        Inventory inventory = player.getInventory();
        boolean worldPick = request.scene() == Scene.WORLD_PICK;
        if (worldPick) {
            if (!Config.enableWorldEmcPick.get() || request.destination() != Destination.SELECTED_HOTBAR_SLOT
                    || player.isCreative() || player.isSpectator() || player.containerMenu != player.inventoryMenu
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

    private static final class NativeAttempt {
        private final ServerPlayer player;
        private final Scene scene;
        private Component failure;
        private boolean canTransmute;

        private NativeAttempt(ServerPlayer player, Scene scene) {
            this.player = player;
            this.scene = scene;
        }
    }
}
