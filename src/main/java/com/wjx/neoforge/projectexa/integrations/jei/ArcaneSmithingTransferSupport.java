package com.wjx.neoforge.projectexa.integrations.jei;

import com.wjx.neoforge.projectexa.config.JeiEmcClientState;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import com.wjx.neoforge.projectexa.util.Util;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** Read-only, single-craft availability shared by the arcane tablet smithing transfer. */
public final class ArcaneSmithingTransferSupport {
    private ArcaneSmithingTransferSupport() {
    }

    public static Status check(IRecipeSlotsView slots, Player player, AbstractContainerMenu container, SmithingRecipe recipe) {
        List<Predicate<ItemStack>> ingredients = ingredients(recipe);
        List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
        List<List<ItemStack>> options = new ArrayList<>(3);
        int emptyMask = 0;
        int size = Math.min(inputs.size(), 3);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        BigInteger budget = provider == null ? BigInteger.ZERO : availableEmc(provider);
        BigInteger[] costs = new BigInteger[size];
        ItemStack[] transmuted = new ItemStack[size];
        for (int index = 0; index < size; index++) {
            List<ItemStack> candidates = inputs.get(index).getItemStacks()
                    .filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList();
            options.add(candidates);
            if (candidates.isEmpty()) emptyMask |= 1 << index;
            for (ItemStack option : candidates) {
                ItemStack candidate = Util.cleanStack(option);
                if (candidate.isEmpty() || !ingredients.get(index).test(candidate)) continue;
                long value = IEMCProxy.INSTANCE.getValue(candidate);
                if (provider != null && value > 0 && provider.hasKnowledge(candidate)
                        && (costs[index] == null || costs[index].compareTo(BigInteger.valueOf(value)) > 0)) {
                    costs[index] = BigInteger.valueOf(value);
                    transmuted[index] = candidate;
                }
            }
        }

        // At most three inputs: subset matching avoids both double-counting a stack and
        // spending a scarce alternative on a slot that could use a different item.
        ItemStack[][] assignments = new ItemStack[1 << size][];
        assignments[emptyMask] = new ItemStack[size];
        for (ItemStack stack : availableItems(player, container)) {
            if (stack.isEmpty()) continue;
            int matches = 0;
            for (int index = 0; index < size; index++) {
                if (!ingredients.get(index).test(stack)) continue;
                for (ItemStack candidate : options.get(index)) {
                    if (candidate.getItem() == stack.getItem()) {
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
                        assignments[next][Integer.numberOfTrailingZeros(bit)] = stack.copyWithCount(1);
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
        List<List<ItemStack>> choices = new ArrayList<>(3);
        List<List<ItemStack>> plannedRecipe = new ArrayList<>(3);
        for (int index = 0; index < 3; index++) {
            List<ItemStack> candidates = index < size ? options.get(index) : List.of();
            ItemStack selected = index < size ? bestItems[index] : null;
            if (index < size) {
                if ((bestMask & (1 << index)) == 0) missing.add(index);
            }
            choices.add(candidates.isEmpty() ? List.of(ItemStack.EMPTY) : candidates);
            plannedRecipe.add(List.of(selected == null ? ItemStack.EMPTY : selected));
        }
        return new Status(missing, choices, plannedRecipe);
    }

    private static List<Predicate<ItemStack>> ingredients(SmithingRecipe recipe) {
        return List.of(recipe::isTemplateIngredient, recipe::isBaseIngredient, recipe::isAdditionIngredient);
    }

    private static List<ItemStack> availableItems(Player player, AbstractContainerMenu container) {
        List<ItemStack> items = new ArrayList<>();
        var inventory = player.getInventory();
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            items.add(inventory.getItem(index));
        }
        // Existing smithing inputs are returned to inventory before filling the recipe.
        if (container instanceof ContainerArcaneTransmutationTablet) {
            for (int index = 0; index < 3; index++) {
                items.add(container.getSlot(ContainerArcaneTransmutationTablet.SMITHING + index).getItem());
            }
        }
        return items;
    }

    private static BigInteger availableEmc(IKnowledgeProvider provider) {
        BigInteger available = provider.getEmc();
        var inputLocks = provider.getInputAndLocks();
        for (int index = 0; index < inputLocks.getSlots(); index++) {
            if (index == 8) continue; // The lock is a template, not usable stored EMC.
            ItemStack stack = inputLocks.getStackInSlot(index);
            if (stack.isEmpty()) continue;
            var holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
            if (holder != null) available = available.add(BigInteger.valueOf(Math.max(0, holder.getStoredEmc(stack))));
        }
        return available;
    }

    public record Status(List<Integer> missing, List<List<ItemStack>> recipe,
                         List<List<ItemStack>> plannedRecipe) implements IRecipeTransferError {
        public List<List<ItemStack>> recipe(boolean transferAll, Player player, AbstractContainerMenu container,
                SmithingRecipe smithingRecipe) {
            // A normal click must follow the exact feasible allocation, otherwise a
            // broad ingredient can steal the only item reserved for a narrower one.
            // Shift keeps the existing best-effort bulk/partial transfer behavior.
            if (!transferAll && missing.isEmpty()) return plannedRecipe;
            var items = availableItems(player, container);
            var ingredients = ingredients(smithingRecipe);
            var provider = Util.getKnowledgeProvider(player);
            boolean allowLargeCandidates = JeiEmcClientState.allowsLargeRecipeTransferCandidates();
            return IntStream.range(0, 3).mapToObj(index -> RecipeTransferUtil.prioritizeSmithingCandidates(
                    recipe.get(index), plannedRecipe.get(index).getFirst(), items, ingredients.get(index), provider,
                    allowLargeCandidates)).toList();
        }

        public boolean canTransfer(boolean transferAll) {
            return missing.isEmpty() || transferAll;
        }

        @Override
        public Type getType() {
            return canTransfer(Screen.hasShiftDown()) ? Type.COSMETIC : Type.USER_FACING;
        }

        @Override
        public int getButtonHighlightColor() {
            return 0;
        }

        @Override
        public int getMissingCountHint() {
            return missing.size();
        }

        @SuppressWarnings("removal") // Retain tooltips on older JEI releases as well.
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
