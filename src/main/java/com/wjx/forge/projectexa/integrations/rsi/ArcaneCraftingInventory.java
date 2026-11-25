package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.crafting.CraftingResolver;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** RSI-only virtual backpack. Never installed on the player's general item capability. */
public final class ArcaneCraftingInventory implements IItemHandler {
    private final ServerPlayer player;
    private final boolean mainInventory;
    private final List<ItemStack> templates;
    private final List<IItemHandler> physicalBackpacks;
    private final Map<CraftingResolver.StackKey, Long> pending;

    public ArcaneCraftingInventory(ServerPlayer player, boolean mainInventory, List<IItemHandler> backpacks,
                                  Map<CraftingResolver.StackKey, Long> pending) {
        this.player = player;
        this.mainInventory = mainInventory;
        this.physicalBackpacks = backpacks.stream()
                .filter(handler -> !(handler instanceof ArcaneCraftingInventory)).toList();
        this.pending = pending;
        Map<CraftingResolver.StackKey, ItemStack> unique = new LinkedHashMap<>();
        IKnowledgeProvider provider = provider(player);
        if (provider != null) {
            for (ItemInfo info : knownItems(provider)) {
                ItemStack stack = Util.cleanStack(info.createStack());
                if (templateValue(stack, provider) > 0) unique.putIfAbsent(key(stack), stack);
            }
        }
        this.templates = List.copyOf(unique.values());
    }

    public static CraftingResolver.StackKey key(ItemStack stack) {
        return CraftingResolver.StackKey.of(stack, true);
    }

    private static Iterable<ItemInfo> knownItems(IKnowledgeProvider provider) {
        return provider.hasFullKnowledge() ? EMCMappingHandler.getMappedItems() : provider.getKnowledge();
    }

    /** Add a read-only EMC quote to the exact physical counts RSI just collected. */
    public static Map<CraftingResolver.StackKey, Long> withArcaneCounts(ServerPlayer player,
            @Nullable Set<Item> itemTypes, Map<CraftingResolver.StackKey, Long> physicalCounts) {
        if (itemTypes != null && itemTypes.isEmpty()) return physicalCounts;
        IKnowledgeProvider provider = provider(player);
        if (provider == null) return physicalCounts;
        BigInteger balance = provider.getEmc();
        if (balance.signum() <= 0) return physicalCounts;

        Map<CraftingResolver.StackKey, Long> counts = physicalCounts;
        Set<CraftingResolver.StackKey> seen = new HashSet<>();
        for (ItemInfo info : knownItems(provider)) {
            if (itemTypes != null && !itemTypes.contains(info.getItem())) continue;
            ItemStack template = Util.cleanStack(info.createStack());
            long price = templateValue(template, provider);
            if (price <= 0) continue;
            CraftingResolver.StackKey key = key(template);
            if (!seen.add(key)) continue;
            long physical = physicalCounts.getOrDefault(key, 0L);
            if (physical >= Integer.MAX_VALUE) continue;
            long virtual = balance.divide(BigInteger.valueOf(price))
                    .min(BigInteger.valueOf(Integer.MAX_VALUE - physical)).longValueExact();
            if (virtual == 0) continue;
            if (counts == physicalCounts) counts = new HashMap<>(physicalCounts);
            counts.put(key, physical + virtual);
        }
        return counts;
    }

    /** Replaces an unbudgeted virtual view with one aware of this ledger's pending reservations. */
    public static List<IItemHandler> append(ServerPlayer player, boolean mainInventory, List<IItemHandler> sources,
                                           Map<CraftingResolver.StackKey, Long> pending) {
        List<IItemHandler> result = new ArrayList<>();
        for (IItemHandler source : sources) {
            if (!(source instanceof ArcaneCraftingInventory)) result.add(source);
        }
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player, mainInventory, result, pending);
        if (virtual.getSlots() > 0) result.add(virtual);
        return result;
    }

    private static IKnowledgeProvider provider(ServerPlayer player) {
        if (player.getServer() == null || !player.getServer().isSameThread()
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return null;
        return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElse(null);
    }

    private static long templateValue(ItemStack stack, IKnowledgeProvider provider) {
        if (stack.isEmpty()) return 0;
        ItemStack clean = Util.cleanStack(stack);
        // ProjectE treats empty NBT and absent NBT as the same identity. Keep capability
        // compatibility and every nonempty tag check; only this representation differs.
        if (!ItemInfo.fromStack(stack).equals(ItemInfo.fromStack(clean)) || !stack.areCapsCompatible(clean)
                || !provider.hasKnowledge(stack) || protectedStack(stack)) return 0;
        return Math.max(0L, IEMCProxy.INSTANCE.getValue(stack));
    }

    /** Read-only quote using exactly the same knowledge and NBT policy as extraction. */
    public static long transmutationValue(ServerPlayer player, ItemStack stack) {
        IKnowledgeProvider provider = provider(player);
        return provider == null ? 0 : templateValue(stack, provider);
    }

    // RSI's policy class is package-private. Mirror its small vanilla-item predicate;
    // a static invoker would still fail JVM class visibility checks in a real server.
    static boolean protectedStack(ItemStack stack) {
        return stack.isDamageableItem()
                && (stack.isEnchanted() || stack.getDamageValue() > 0 || stack.hasCustomHoverName());
    }

    private long physicalCount(ItemStack template) {
        CraftingResolver.StackKey expected = key(template);
        long total = 0;
        if (mainInventory) {
            for (ItemStack stack : player.getInventory().items) {
                if (expected.equals(key(stack))) total += stack.getCount();
            }
        }
        for (IItemHandler backpack : physicalBackpacks) {
            for (int slot = 0; slot < backpack.getSlots(); slot++) {
                ItemStack stack = backpack.getStackInSlot(slot);
                if (expected.equals(key(stack))
                        && !protectedStack(stack)) total += stack.getCount();
            }
        }
        return Math.min(Integer.MAX_VALUE, total);
    }

    /** Reserve one shared EMC pool, not an independent balance for every known item. */
    private BigInteger budgetFor(ItemStack template, IKnowledgeProvider provider) {
        BigInteger budget = provider.getEmc();
        CraftingResolver.StackKey current = key(template);
        for (var entry : pending.entrySet()) {
            if (entry.getKey().equals(current)) continue; // RSI subtracts this key's pending count itself.
            ItemStack other = entry.getKey().toStack();
            long missing = Math.max(0L, entry.getValue() - physicalCount(other));
            if (missing > 0) {
                long price = templateValue(other, provider);
                if (price <= 0) return BigInteger.ZERO;
                budget = budget.subtract(BigInteger.valueOf(price).multiply(BigInteger.valueOf(missing)));
            }
        }
        return budget.max(BigInteger.ZERO);
    }

    @Override
    public int getSlots() {
        return templates.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IKnowledgeProvider provider = provider(player);
        if (provider == null || slot < 0 || slot >= templates.size()) return ItemStack.EMPTY;
        ItemStack template = templates.get(slot);
        long price = templateValue(template, provider);
        if (price <= 0) return ItemStack.EMPTY;
        BigInteger count = budgetFor(template, provider).divide(BigInteger.valueOf(price));
        // RSI adds physical and virtual counts using ints; reserve headroom for physical stock.
        int available = count.min(BigInteger.valueOf(Integer.MAX_VALUE - physicalCount(template))).intValue();
        return available > 0 ? template.copyWithCount(available) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack available = getStackInSlot(slot);
        if (available.isEmpty()) return ItemStack.EMPTY;
        int count = Math.min(amount, available.getCount());
        if (!simulate) {
            IKnowledgeProvider provider = provider(player);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger cost = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(available)).multiply(BigInteger.valueOf(count));
            if (provider.getEmc().compareTo(cost) < 0) return ItemStack.EMPTY;
            provider.setEmc(provider.getEmc().subtract(cost));
            provider.syncEmc(player);
        }
        return available.copyWithCount(count);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack; // Refunds remain real items in RSI's existing settlement ledger.
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }
}
