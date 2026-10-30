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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RSI-only virtual backpack. Never installed on the player's general item capability. */
public final class ArcaneCraftingInventory implements IItemHandler {
    private final ServerPlayer player;
    private final List<ItemStack> templates;
    private final List<IItemHandler> physicalBackpacks;
    private final Map<CraftingResolver.StackKey, Long> pending;

    public ArcaneCraftingInventory(ServerPlayer player, List<IItemHandler> backpacks,
                                  Map<CraftingResolver.StackKey, Long> pending) {
        this.player = player;
        this.physicalBackpacks = backpacks.stream()
                .filter(handler -> !(handler instanceof ArcaneCraftingInventory)).toList();
        this.pending = pending;
        Map<CraftingResolver.StackKey, ItemStack> unique = new LinkedHashMap<>();
        IKnowledgeProvider provider = provider();
        if (provider != null) {
            for (ItemInfo info : provider.hasFullKnowledge()
                    ? EMCMappingHandler.getMappedItems() : provider.getKnowledge()) {
                ItemStack stack = Util.cleanStack(info.createStack());
                // RSI reads maximum damage before its strict runtime identity comparison.
                // Some mods create empty NBT during that read, even for ordinary ingredients.
                stack.getMaxDamage();
                if (safeTemplate(stack, provider)) unique.putIfAbsent(key(stack), stack);
            }
        }
        this.templates = List.copyOf(unique.values());
    }

    public static CraftingResolver.StackKey key(ItemStack stack) {
        return new CraftingResolver.StackKey(stack.getItem(), stack.hasTag() ? stack.getTag().toString() : null);
    }

    /** Replaces an unbudgeted virtual view with one aware of this ledger's pending reservations. */
    public static List<IItemHandler> append(ServerPlayer player, List<IItemHandler> sources,
                                           Map<CraftingResolver.StackKey, Long> pending) {
        List<IItemHandler> result = new ArrayList<>();
        for (IItemHandler source : sources) {
            if (!(source instanceof ArcaneCraftingInventory)) result.add(source);
        }
        ArcaneCraftingInventory virtual = new ArcaneCraftingInventory(player, result, pending);
        if (virtual.getSlots() > 0) result.add(virtual);
        return result;
    }

    private IKnowledgeProvider provider() {
        if (player.getServer() == null || !player.getServer().isSameThread()
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return null;
        return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElse(null);
    }

    private static boolean safeTemplate(ItemStack stack, IKnowledgeProvider provider) {
        if (stack.isEmpty()) return false;
        ItemStack clean = Util.cleanStack(stack);
        // ProjectE treats empty NBT and absent NBT as the same identity. Keep capability
        // compatibility and every nonempty tag check; only this representation differs.
        return ItemInfo.fromStack(stack).equals(ItemInfo.fromStack(clean))
                && stack.areCapsCompatible(clean)
                && provider.hasKnowledge(stack) && IEMCProxy.INSTANCE.getValue(stack) > 0
                && !protectedStack(stack);
    }

    /** Read-only quote using exactly the same knowledge and NBT policy as extraction. */
    public static long transmutationValue(ServerPlayer player, ItemStack stack) {
        if (player.getServer() == null || !player.getServer().isSameThread()
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) return 0;
        IKnowledgeProvider provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElse(null);
        return provider != null && safeTemplate(stack, provider) ? IEMCProxy.INSTANCE.getValue(stack) : 0;
    }

    // RSI's policy class is package-private. Mirror its small vanilla-item predicate;
    // a static invoker would still fail JVM class visibility checks in a real server.
    static boolean protectedStack(ItemStack stack) {
        return stack.isDamageableItem()
                && (stack.isEnchanted() || stack.getDamageValue() > 0 || stack.hasCustomHoverName());
    }

    private long physicalCount(ItemStack template) {
        return physicalCount(player, physicalBackpacks, template);
    }

    public static long physicalCount(ServerPlayer player, List<IItemHandler> backpacks, ItemStack template) {
        long total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, template)) total += stack.getCount();
        }
        for (IItemHandler backpack : backpacks) {
            if (backpack instanceof ArcaneCraftingInventory) continue;
            for (int slot = 0; slot < backpack.getSlots(); slot++) {
                ItemStack stack = backpack.getStackInSlot(slot);
                if (ItemStack.isSameItemSameTags(stack, template)
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
                long price = IEMCProxy.INSTANCE.getValue(other);
                if (price <= 0 || !safeTemplate(other, provider)) return BigInteger.ZERO;
                budget = budget.subtract(BigInteger.valueOf(price).multiply(BigInteger.valueOf(missing)));
            }
        }
        return budget.max(BigInteger.ZERO);
    }

    @Override
    public int getSlots() {
        return templates.size();
    }

    /** Includes unaffordable templates so a quote can still describe the required EMC. */
    ItemStack templateForQuote(int slot) {
        return templates.get(slot).copy();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        IKnowledgeProvider provider = provider();
        if (provider == null || slot < 0 || slot >= templates.size()) return ItemStack.EMPTY;
        ItemStack template = templates.get(slot);
        if (!safeTemplate(template, provider)) return ItemStack.EMPTY;
        BigInteger count = budgetFor(template, provider).divide(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(template)));
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
            IKnowledgeProvider provider = provider();
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
