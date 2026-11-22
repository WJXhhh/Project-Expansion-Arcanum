package com.wjx.neoforge.projectexa.util;

import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RecipeTransferUtil {
    public static final int MAX_CANDIDATES_PER_SLOT = 256;

    private RecipeTransferUtil() {}

    public static List<ItemStack> filterAvailableCandidates(List<ItemStack> candidates, ContainerArcaneTransmutationTablet container) {
        if (candidates.isEmpty()) return List.of(ItemStack.EMPTY);
        if (candidates.size() <= MAX_CANDIDATES_PER_SLOT) return candidates;

        var owned = ItemStackLinkedSet.createTypeAndComponentsSet();
        var inventory = container.getPlayer().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) owned.add(Util.cleanStack(stack));
        }
        // Clearing the grid can return ingredients to inventory or convert them into EMC.
        for (ItemStack stack : container.getCraftingInputs()) {
            if (!stack.isEmpty()) owned.add(Util.cleanStack(stack));
        }

        var seen = ItemStackLinkedSet.createTypeAndComponentsSet();
        List<ItemStack> options = new ArrayList<>(MAX_CANDIDATES_PER_SLOT);
        List<EmcCandidate> learned = new ArrayList<>();
        var provider = container.getProvider();
        for (ItemStack candidate : candidates) {
            ItemStack cleaned = Util.cleanStack(candidate);
            if (cleaned.isEmpty() || !seen.add(cleaned)) continue;
            if (owned.contains(cleaned)) {
                options.add(cleaned);
            } else if (provider.hasKnowledge(cleaned)) {
                long value = IEMCProxy.INSTANCE.getValue(cleaned);
                if (value > 0) learned.add(new EmcCandidate(cleaned, value));
            }
        }
        // The server checks affordability after clearing the grid and consuming other inputs.
        learned.sort(Comparator.comparingLong(EmcCandidate::value));
        learned.stream().limit(MAX_CANDIDATES_PER_SLOT - options.size())
                .map(EmcCandidate::stack).forEachOrdered(options::add);
        return options.isEmpty() ? candidates.subList(0, 16) : options;
    }

    private record EmcCandidate(ItemStack stack, long value) {}
}
