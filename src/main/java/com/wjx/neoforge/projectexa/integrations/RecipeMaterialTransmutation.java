package com.wjx.neoforge.projectexa.integrations;

import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Supply missing material items only. Never craft a machine/ritual result or bypass its non-item costs. */
public final class RecipeMaterialTransmutation {
    public static final int MAX_MATERIALS = 4096;
    private RecipeMaterialTransmutation() {}

    public static void supply(ServerPlayer player, List<Ingredient> ingredients) {
        if (!player.server.isSameThread() || player.isSpectator() || !ArcaneTabletAccess.hasTablet(player)
                || ingredients == null || ingredients.isEmpty() || ingredients.size() > MAX_MATERIALS) return;
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) return;
        List<ItemStack> reserved = new ArrayList<>(player.getInventory().getContainerSize());
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) reserved.add(player.getInventory().getItem(i).copy());
        TransmutationInventory transmutation = new TransmutationInventory(player);
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty() || reserveExisting(ingredient, reserved)) continue;
            ItemStack cheapest = ItemStack.EMPTY;
            long cheapestCost = Long.MAX_VALUE;
            for (ItemStack option : ingredient.getItems()) {
                ItemStack clean = Util.cleanStack(option);
                // Component-sensitive ingredients must still accept the persistent form we actually create.
                if (clean.isEmpty() || !ingredient.test(clean) || !provider.hasKnowledge(clean)) continue;
                long cost = IEMCProxy.INSTANCE.getValue(clean);
                if (cost > 0 && cost <= cheapestCost) { cheapest = clean; cheapestCost = cost; }
            }
            if (cheapest.isEmpty()) continue;
            BigInteger cost = BigInteger.valueOf(cheapestCost);
            if (transmutation.getAvailableEmc().compareTo(cost) < 0) continue;
            transmutation.removeEmc(cost);
            Util.returnToInventory(player.getInventory(), player, cheapest.copyWithCount(1), true);
        }
        provider.syncEmc(player);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean reserveExisting(Ingredient ingredient, List<ItemStack> reserved) {
        for (ItemStack stack : reserved) {
            if (!stack.isEmpty() && ingredient.test(stack)) { stack.shrink(1); return true; }
        }
        return false;
    }
}
