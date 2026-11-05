package com.wjx.forge.projectexa.integrations.rsi;

import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** Per-craft observations, kept separate from ProjectE knowledge until successful settlement. */
public final class RsiCraftingKnowledge {
    private static final ThreadLocal<RsiCraftingKnowledge> ACTIVE = new ThreadLocal<>();

    private final UUID playerId;
    private final Set<ItemInfo> products = new LinkedHashSet<>();
    private boolean settled;

    public RsiCraftingKnowledge(UUID playerId) {
        this.playerId = playerId;
    }

    public boolean capture(BooleanSupplier action) {
        RsiCraftingKnowledge previous = ACTIVE.get();
        ACTIVE.set(this);
        try {
            return action.getAsBoolean();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static void recordCrafted(ServerPlayer player, ItemStack stack) {
        RsiCraftingKnowledge active = ACTIVE.get();
        if (active == null || active.settled || player == null || stack == null || stack.isEmpty()
                || !active.playerId.equals(player.getUUID()) || !enabledFor(player)) return;
        // Copy before RSI merges or consumes this output in a later recursive step.
        active.products.add(ItemInfo.fromStack(stack.copyWithCount(1)));
    }

    public void complete(ServerPlayer player) {
        if (settled) return;
        settled = true;
        try {
            if (player == null || !playerId.equals(player.getUUID()) || !enabledFor(player)) return;
            IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            if (provider == null) return;
            Set<ItemInfo> visited = new LinkedHashSet<>();
            for (ItemInfo raw : products) {
                ItemStack cleanStack = Util.cleanStack(raw.createStack());
                if (cleanStack.isEmpty() || IEMCProxy.INSTANCE.getValue(cleanStack) <= 0) continue;
                ItemInfo clean = ItemInfo.fromStack(cleanStack);
                if (!visited.add(clean) || provider.hasKnowledge(clean)) continue;
                if (!MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, raw, clean))
                        && provider.addKnowledge(clean)) {
                    provider.syncKnowledgeChange(player, clean, true);
                }
            }
        } finally {
            products.clear();
        }
    }

    public void discard() {
        settled = true;
        products.clear();
    }

    private static boolean enabledFor(ServerPlayer player) {
        return Config.rsiCraftingAutoLearn.get() && PacketOpenArcaneTransmutationTablet.hasTablet(player);
    }
}
