package com.wjx.forge.projectexa.gui.container.slots;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PXResultSlot extends ResultSlot {
    private final CraftingContainer craftSlots;
    private final Player player;
    private final ContainerArcaneTransmutationTablet tablet;

    public PXResultSlot(Player player, CraftingContainer craftSlots, ResultContainer resultSlots,
                        ContainerArcaneTransmutationTablet tablet, int slot, int x, int y) {
        super(player, craftSlots, resultSlots, slot, x, y);
        this.craftSlots = craftSlots;
        this.player = player;
        this.tablet = tablet;
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        learnResult(stack);
        List<ItemStack> previousItems = craftSlots.getItems().stream().map(ItemStack::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        super.onTake(player, stack);
        refillAfterTake(previousItems);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        learnResult(stack);
        super.onQuickCraft(stack, amount);
    }

    private void learnResult(ItemStack stack) {
        if (!player.level().isClientSide && IEMCProxy.INSTANCE.hasValue(stack)) {
            tablet.transmutationInventory.handleKnowledge(stack);
        }
    }

    private void refillAfterTake(List<ItemStack> previousItems) {
        if (player.level().isClientSide || tablet.skipRefill) return;
        List<ItemStack> currentItems = craftSlots.getItems().stream().map(ItemStack::copy)
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < currentItems.size(); i++) {
            ItemStack current = currentItems.get(i);
            if (current.isEmpty() || previousItems.get(i).equals(current)) continue;

            if (IEMCProxy.INSTANCE.hasValue(current)) {
                tablet.transmutationInventory.handleKnowledge(current);
                BigInteger value = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(current))
                        .multiply(BigInteger.valueOf(current.getCount()));
                tablet.transmutationInventory.addEmc(value);
                craftSlots.setItem(i, ItemStack.EMPTY);
            } else {
                ItemStack remainder = Util.returnToInventory(tablet.getPlayer().getInventory(), player, current, false);
                craftSlots.setItem(i, remainder);
            }
        }

        List<List<ItemStack>> recipe = previousItems.stream().map(List::of)
                .map(ArrayList::new).collect(Collectors.toList());
        tablet.fillCraftingSlots(recipe, false);
    }

    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        tablet.isCrafting = !stack.isEmpty();
    }
}
