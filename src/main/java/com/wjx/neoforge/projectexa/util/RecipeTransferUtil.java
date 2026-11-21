package com.wjx.neoforge.projectexa.util;

import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RecipeTransferUtil {
    private RecipeTransferUtil() {}

    /**
     * Filters candidate ingredients for a recipe slot by prioritizing items the player actually
     * has in their inventory or has learned in their transmutation knowledge with sufficient EMC.
     * If no feasible items exist, limits the candidate list to a small representative sample
     * to avoid blowing up the network packet when large tags (e.g. #c:ingots with 250+ variants)
     * are transferred.
     */
    public static List<ItemStack> filterAvailableCandidates(List<ItemStack> candidates, ContainerArcaneTransmutationTablet container, Player player) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of(ItemStack.EMPTY);
        }

        List<ItemStack> available = new ArrayList<>();
        long availableEmc = container.transmutationInventory.getAvailableEmcAsLong();
        var provider = container.getProvider();

        // 1. Prioritize items physically present in the player's inventory
        for (ItemStack candidate : candidates) {
            if (!candidate.isEmpty() && player.getInventory().contains(candidate)) {
                available.add(candidate);
            }
        }

        // 2. Next, include items learned in the tablet that the player can afford with current EMC
        if (provider != null) {
            for (ItemStack candidate : candidates) {
                if (candidate.isEmpty() || available.contains(candidate)) continue;
                ItemStack cleaned = Util.cleanStack(candidate);
                if (provider.hasKnowledge(cleaned)) {
                    long emc = IEMCProxy.INSTANCE.getValue(cleaned);
                    if (emc > 0 && emc <= availableEmc) {
                        available.add(candidate);
                    }
                }
            }
        }

        // If feasible items were found, only send those (capped at 64 to guard against pathological cases)
        if (!available.isEmpty()) {
            return available.size() > 64 ? available.subList(0, 64) : available;
        }

        // If no feasible items exist, only send a small prefix rather than hundreds of useless variants
        return candidates.size() > 16 ? candidates.subList(0, 16) : candidates;
    }
}
