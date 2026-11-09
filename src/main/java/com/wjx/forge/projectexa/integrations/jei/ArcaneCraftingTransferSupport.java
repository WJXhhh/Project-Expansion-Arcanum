package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.forge.projectexa.gui.container.slots.PXCraftingSlot;
import com.wjx.forge.projectexa.util.Util;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Read-only, single-craft availability shared by every arcane crafting entry point. */
public final class ArcaneCraftingTransferSupport {
    private ArcaneCraftingTransferSupport() {
    }

    public static Status check(IRecipeSlotsView slots, Player player, AbstractContainerMenu container) {
        List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        List<List<ItemStack>> options = new ArrayList<>(9);
        int emptyMask = 0;
        int size = Math.min(inputs.size(), 9);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        BigInteger budget = provider == null ? BigInteger.ZERO : availableEmc(provider);
        BigInteger[] costs = new BigInteger[size];
        ItemStack[] transmuted = new ItemStack[size];
        for (int index = 0; index < size; index++) {
            List<ItemStack> candidates = inputs.get(index).getItemStacks()
                    .filter(stack -> !stack.isEmpty()).map(Util::cleanStack).toList();
            options.add(candidates);
            if (candidates.isEmpty()) emptyMask |= 1 << index;
            for (ItemStack candidate : candidates) {
                long value = IEMCProxy.INSTANCE.getValue(candidate);
                if (provider != null && value > 0 && provider.hasKnowledge(candidate)
                        && (costs[index] == null || costs[index].compareTo(BigInteger.valueOf(value)) > 0)) {
                    costs[index] = BigInteger.valueOf(value);
                    transmuted[index] = candidate;
                }
            }
        }

        List<ItemStack> inventory = new ArrayList<>();
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            inventory.add(player.getInventory().getItem(index));
        }
        // A transfer first clears the tablet's existing grid, making these real items reusable.
        if (container instanceof ContainerArcaneTransmutationTablet) {
            for (Slot slot : container.slots) {
                if (slot instanceof PXCraftingSlot) inventory.add(slot.getItem());
            }
        }

        // At most nine inputs: subset matching avoids both double-counting a stack and
        // spending a scarce alternative on a slot that could use a different item.
        ItemStack[][] assignments = new ItemStack[1 << size][];
        assignments[emptyMask] = new ItemStack[size];
        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) continue;
            ItemStack clean = Util.cleanStack(stack);
            int matches = 0;
            for (int index = 0; index < size; index++) {
                for (ItemStack candidate : options.get(index)) {
                    if (Util.areStacksEqual(player.level().registryAccess(), candidate, clean)) {
                        matches |= 1 << index;
                        break;
                    }
                }
            }
            if (matches == 0) continue;
            for (int unit = 0; unit < Math.min(stack.getCount(), size); unit++) {
                // Descending masks ensure this physical unit is assigned only once.
                for (int mask = assignments.length - 1; mask >= 0; mask--) {
                    if (assignments[mask] == null) continue;
                    for (int remaining = matches & ~mask; remaining != 0; remaining &= remaining - 1) {
                        int bit = Integer.lowestOneBit(remaining);
                        int next = mask | bit;
                        if (assignments[next] != null) continue;
                        assignments[next] = assignments[mask].clone();
                        assignments[next][Integer.numberOfTrailingZeros(bit)] = clean;
                    }
                }
            }
        }

        Integer[] order = new Integer[size];
        Arrays.setAll(order, index -> index);
        Arrays.sort(order, Comparator.comparing(index -> costs[index], Comparator.nullsLast(Comparator.naturalOrder())));
        int bestMask = emptyMask;
        BigInteger bestSpent = null;
        ItemStack[] bestItems = new ItemStack[size];
        for (int mask = 0; mask < assignments.length; mask++) {
            if (assignments[mask] == null) continue;
            int covered = mask;
            BigInteger remaining = budget;
            ItemStack[] items = assignments[mask].clone();
            for (int index : order) {
                if ((covered & (1 << index)) == 0 && costs[index] != null
                        && remaining.compareTo(costs[index]) >= 0) {
                    remaining = remaining.subtract(costs[index]);
                    covered |= 1 << index;
                    items[index] = transmuted[index];
                }
            }
            BigInteger spent = budget.subtract(remaining);
            if (bestSpent == null || Integer.bitCount(covered) > Integer.bitCount(bestMask)
                    || (Integer.bitCount(covered) == Integer.bitCount(bestMask) && spent.compareTo(bestSpent) < 0)) {
                bestMask = covered;
                bestSpent = spent;
                bestItems = items;
            }
        }

        List<Integer> missing = new ArrayList<>();
        List<List<ItemStack>> recipe = new ArrayList<>(9);
        List<List<ItemStack>> plannedRecipe = new ArrayList<>(9);
        for (int index = 0; index < 9; index++) {
            List<ItemStack> candidates = index < size ? options.get(index) : List.of();
            ItemStack selected = index < size ? bestItems[index] : null;
            if (index < size) {
                if ((bestMask & (1 << index)) == 0) missing.add(index);
            }
            recipe.add(candidates.isEmpty() ? List.of(ItemStack.EMPTY) : candidates);
            plannedRecipe.add(List.of(selected == null ? ItemStack.EMPTY : selected));
        }
        return new Status(missing, recipe, plannedRecipe);
    }

    private static BigInteger availableEmc(IKnowledgeProvider provider) {
        BigInteger available = provider.getEmc();
        IItemHandler inputLocks = provider.getInputAndLocks();
        for (int index = 0; index < inputLocks.getSlots(); index++) {
            if (index == 8) continue; // The lock is a template, not usable stored EMC.
            ItemStack stack = inputLocks.getStackInSlot(index);
            if (stack.isEmpty()) continue;
            IItemEmcHolder holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve().orElse(null);
            if (holder != null) available = available.add(BigInteger.valueOf(Math.max(0, holder.getStoredEmc(stack))));
        }
        return available;
    }

    public record Status(List<Integer> missing, List<List<ItemStack>> recipe,
                         List<List<ItemStack>> plannedRecipe) implements IRecipeTransferError {
        public List<List<ItemStack>> recipe(boolean transferAll) {
            // A normal click must follow the exact feasible allocation, otherwise a
            // broad ingredient can steal the only item reserved for a narrower one.
            // Shift keeps the existing best-effort bulk/partial transfer behavior.
            return !transferAll && missing.isEmpty() ? plannedRecipe : recipe;
        }

        public boolean canTransfer() {
            return missing.isEmpty() || Screen.hasShiftDown();
        }

        @Override
        public Type getType() {
            return canTransfer() ? Type.COSMETIC : Type.USER_FACING;
        }

        @Override
        public int getButtonHighlightColor() {
            return 0;
        }

        @Override
        public int getMissingCountHint() {
            return missing.size();
        }

        @SuppressWarnings("removal") // Retain tooltips on older JEI 15 releases as well.
        @Override
        public List<Component> getTooltip() {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("jei.projectexa.transfer.to_arcane_transmutation_table"));
            if (missing.isEmpty()) {
                lines.add(Component.translatable("jei.projectexa.transfer.materials_ready").withStyle(ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable("jei.projectexa.transfer.force").withStyle(ChatFormatting.GRAY));
            }
            return lines;
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip) {
            getTooltip().forEach(tooltip::add);
        }

        @Override
        public void showError(GuiGraphics graphics, int mouseX, int mouseY, IRecipeSlotsView slots, int recipeX, int recipeY) {
            graphics.pose().pushPose();
            graphics.pose().translate(recipeX, recipeY, 0);
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            for (int index : missing) {
                if (index < inputs.size()) inputs.get(index).drawHighlight(graphics, 0x66FF0000);
            }
            graphics.pose().popPose();
        }
    }
}
