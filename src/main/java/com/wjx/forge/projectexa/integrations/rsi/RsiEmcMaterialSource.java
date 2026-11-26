package com.wjx.forge.projectexa.integrations.rsi;

import com.google.common.collect.MapMaker;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class RsiEmcMaterialSource implements Source {
    static final ResourceLocation ID = Main.rl("arcane_emc");
    // A ledger owns one Context instance. Equal contexts from different ledgers must not share a quote.
    private final Map<Context, BatchQuote> batches = new MapMaker().weakKeys().makeMap();

    private RsiEmcMaterialSource() {}

    public static void register() {
        RSICraftingMaterialSources.register(ID, RSIPlanExtensions.Source.BACKPACKS, new RsiEmcMaterialSource());
    }

    @Override
    public Snapshot snapshot(Context context) {
        IKnowledgeProvider provider = provider(context.player());
        if (provider == null) return Snapshot.EMPTY;
        BigInteger balance = provider.getEmc();
        List<Offer> offers = affordableOffers(provider, balance,
                item -> context.itemTypes().isEmpty() || context.itemTypes().contains(item),
                (template, price) -> new Offer(template, Long.MAX_VALUE, ID, price));
        return offers.isEmpty() ? Snapshot.EMPTY : new Snapshot(Map.of(ID, balance), offers);
    }

    static <T> List<T> affordableOffers(IKnowledgeProvider provider, BigInteger balance, Predicate<Item> wanted,
                                        BiFunction<ItemStack, BigInteger, T> makeOffer) {
        if (balance.signum() <= 0) return List.of();
        Map<ItemInfo, T> offers = new LinkedHashMap<>();
        for (ItemInfo info : provider.hasFullKnowledge()
                ? EMCMappingHandler.getMappedItems() : provider.getKnowledge()) {
            if (!wanted.test(info.getItem())) continue;
            ItemStack stack = info.createStack();
            // A fresh untagged, non-damageable template already has its persistent identity.
            ItemStack template = stack.getTag() == null && !stack.isDamageableItem() ? stack : Util.cleanStack(stack);
            ItemInfo identity = ItemInfo.fromStack(template);
            if (offers.containsKey(identity)) continue;
            long value = unitValue(provider, template);
            if (value > 0) {
                BigInteger price = BigInteger.valueOf(value);
                if (balance.compareTo(price) >= 0) offers.put(identity, makeOffer.apply(template, price));
            }
        }
        return List.copyOf(offers.values());
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
        BatchQuote batch = batches.computeIfAbsent(context, ignored -> new BatchQuote());
        BigInteger previous = batch.total;
        if (earlier.isEmpty() || batch.size != earlier.size()) {
            // Rebuild from RSI's authoritative list after a rollback or a change of ledger context.
            previous = BigInteger.ZERO;
            for (Allocation allocation : earlier) {
                long earlierValue = unitValue(provider, allocation.target());
                if (earlierValue == 0) return Optional.empty();
                previous = previous.add(cost(earlierValue, allocation.count()));
            }
        }
        BigInteger total = previous.add(cost);
        if (provider.getEmc().compareTo(total) < 0) return Optional.empty();
        batch.size = earlier.size() + 1;
        batch.total = total;
        return Optional.of(new EmcReservation(player, target, (int) requested.count(), value, cost, total, batch));
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

    /** Called after checking the live balance on the server thread. False means the debit is uncertain. */
    static boolean debitEmc(ServerPlayer player, IKnowledgeProvider provider, BigInteger cost) {
        try {
            provider.setEmc(provider.getEmc().subtract(cost));
        } catch (RuntimeException failure) {
            Main.Logger.warn("Could not confirm an RSI EMC debit for {}", player.getUUID(), failure);
            return false;
        }
        try {
            provider.syncEmc(player);
        } catch (RuntimeException failure) {
            Main.Logger.warn("Could not sync EMC after an RSI debit for {}", player.getUUID(), failure);
        }
        return true;
    }

    private static final class BatchQuote {
        int size;
        BigInteger total = BigInteger.ZERO;
    }

    private static final class EmcReservation implements Reservation {
        private final ServerPlayer player;
        private final ItemStack target;
        private final int count;
        private final long unitValue;
        private final BigInteger cost;
        private final BigInteger batchCostSoFar;
        private final BatchQuote batch;
        private boolean closed;

        private EmcReservation(ServerPlayer player, ItemStack target, int count, long unitValue,
                               BigInteger cost, BigInteger batchCostSoFar, BatchQuote batch) {
            this.player = player;
            this.target = target;
            this.count = count;
            this.unitValue = unitValue;
            this.cost = cost;
            this.batchCostSoFar = batchCostSoFar;
            this.batch = batch;
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
            release();
            if (provider == null) return new Extraction(List.of(), false);
            Extraction extraction = new Extraction(List.of(target.copyWithCount(count)), false);
            return debitEmc(player, provider, cost) ? extraction : new Extraction(List.of(), true);
        }

        @Override
        public void release() {
            if (closed) return;
            closed = true;
            batch.size = -1;
            batch.total = BigInteger.ZERO;
        }
    }
}
