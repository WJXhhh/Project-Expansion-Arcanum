package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.IntegrationResourceKey;
import com.huanghuang.rsintegration.api.RSICraftingMaterialSources;
import com.huanghuang.rsintegration.api.RSICraftingMaterialSources.*;
import com.huanghuang.rsintegration.api.RSIPlanExtensions;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RsiEmcMaterialSource implements Source {
    static final ResourceLocation ID = Main.rl("arcane_emc");

    private RsiEmcMaterialSource() {}

    public static void register() {
        RSICraftingMaterialSources.register(ID, RSIPlanExtensions.Source.BACKPACKS, new RsiEmcMaterialSource());
    }

    @Override
    public Snapshot snapshot(Context context) {
        IKnowledgeProvider provider = provider(context.player());
        if (provider == null || provider.getEmc().signum() <= 0) return Snapshot.EMPTY;
        BigInteger balance = provider.getEmc();
        Map<IntegrationResourceKey, Offer> offers = new LinkedHashMap<>();
        for (ItemInfo info : provider.hasFullKnowledge()
                ? EMCMappingHandler.getMappedItems() : provider.getKnowledge()) {
            ItemStack stack = info.createStack();
            if (!context.itemTypes().isEmpty() && !context.itemTypes().contains(stack.getItem())) continue;
            ItemStack template = Util.cleanStack(stack);
            long value = unitValue(provider, template);
            if (value > 0 && balance.compareTo(BigInteger.valueOf(value)) >= 0) {
                offers.putIfAbsent(IntegrationResourceKey.item(template),
                        new Offer(template, Long.MAX_VALUE, ID, BigInteger.valueOf(value)));
            }
        }
        return offers.isEmpty() ? Snapshot.EMPTY : new Snapshot(Map.of(ID, balance), List.copyOf(offers.values()));
    }

    @Override
    public Optional<Reservation> reserve(Context context, Allocation requested, List<Allocation> earlier) {
        ServerPlayer player = context.player();
        IKnowledgeProvider provider = provider(player);
        if (provider == null || requested.count() > Integer.MAX_VALUE) return Optional.empty();
        ItemStack target = requested.target();
        long value = unitValue(provider, target);
        if (value == 0) return Optional.empty();
        BigInteger cost = cost(value, requested.count());
        BigInteger total = cost;
        for (Allocation allocation : earlier) {
            long earlierValue = unitValue(provider, allocation.target());
            if (earlierValue == 0) return Optional.empty();
            total = total.add(cost(earlierValue, allocation.count()));
        }
        if (provider.getEmc().compareTo(total) < 0) return Optional.empty();
        return Optional.of(new EmcReservation(player, target, (int) requested.count(), value, cost, total));
    }

    static IKnowledgeProvider provider(ServerPlayer player) {
        if (player.getServer() == null || !player.getServer().isSameThread()
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return null;
        return Util.getKnowledgeProvider(player);
    }

    static long unitValue(IKnowledgeProvider provider, ItemStack stack) {
        if (stack.isEmpty() || (stack.isDamageableItem()
                && (stack.isEnchanted() || stack.getDamageValue() > 0 || stack.hasCustomHoverName()))) return 0;
        ItemStack clean = Util.cleanStack(stack);
        if (clean.isEmpty() || !ItemInfo.fromStack(stack).equals(ItemInfo.fromStack(clean))
                || !stack.areCapsCompatible(clean) || !provider.hasKnowledge(stack)) return 0;
        return Math.max(0, IEMCProxy.INSTANCE.getValue(stack));
    }

    private static BigInteger cost(long value, long count) {
        return BigInteger.valueOf(value).multiply(BigInteger.valueOf(count));
    }

    private static final class EmcReservation implements Reservation {
        private final ServerPlayer player;
        private final ItemStack target;
        private final int count;
        private final long unitValue;
        private final BigInteger cost;
        private final BigInteger batchCostSoFar;
        private boolean closed;

        private EmcReservation(ServerPlayer player, ItemStack target, int count, long unitValue,
                               BigInteger cost, BigInteger batchCostSoFar) {
            this.player = player;
            this.target = target;
            this.count = count;
            this.unitValue = unitValue;
            this.cost = cost;
            this.batchCostSoFar = batchCostSoFar;
        }

        private IKnowledgeProvider validProvider(BigInteger requiredBalance) {
            if (closed) return null;
            IKnowledgeProvider provider = provider(player);
            return provider != null && unitValue(provider, target) == unitValue
                    && provider.getEmc().compareTo(requiredBalance) >= 0 ? provider : null;
        }

        @Override
        public boolean validate() {
            return validProvider(batchCostSoFar) != null;
        }

        @Override
        public Extraction commit() {
            IKnowledgeProvider provider = validProvider(cost);
            closed = true;
            if (provider == null) return new Extraction(List.of(), false);
            Extraction extraction = new Extraction(List.of(target.copyWithCount(count)), false);
            try {
                provider.setEmc(provider.getEmc().subtract(cost));
            } catch (RuntimeException failure) {
                Main.Logger.warn("Could not confirm the EMC debit for an RSI reservation", failure);
                return new Extraction(List.of(), true);
            }
            try {
                provider.syncEmc(player);
            } catch (RuntimeException failure) {
                Main.Logger.warn("Could not sync EMC after an RSI material debit", failure);
            }
            return extraction;
        }

        @Override
        public void release() {
            closed = true;
        }
    }
}
