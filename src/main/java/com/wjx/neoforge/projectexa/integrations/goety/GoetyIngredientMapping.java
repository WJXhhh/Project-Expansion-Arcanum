package com.wjx.neoforge.projectexa.integrations.goety;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** 1.21 collector maps are fastutil Object2IntMap; fake groups are records rather than Tuple. */
final class GoetyIngredientMapping {
    private GoetyIngredientMapping() {}
    static boolean map(IMappingCollector<NormalizedSimpleStack, Long> mapper, INSSFakeGroupManager groups,
                       ItemStack output, List<Ingredient> ingredients, Predicate<Ingredient> consumedWithoutRemainder) {
        if (output.isEmpty()) return false;
        Object2IntMap<NormalizedSimpleStack> inputs = new Object2IntOpenHashMap<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) continue;
            List<ItemStack> options = new ArrayList<>();
            Set<NormalizedSimpleStack> normalized = new HashSet<>();
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty() && normalized.add(NSSItem.createItem(stack))) options.add(stack);
            }
            if (options.isEmpty()) return false;
            boolean includeRemainder = !consumedWithoutRemainder.test(ingredient);
            if (options.size() == 1) add(inputs, options.getFirst(), includeRemainder);
            else {
                var group = groups.getOrCreateFakeGroup(normalized);
                inputs.mergeInt(group.dummy(), 1, Integer::sum);
                if (group.created()) {
                    for (ItemStack stack : options) {
                        Object2IntMap<NormalizedSimpleStack> alternative = new Object2IntOpenHashMap<>();
                        add(alternative, stack, includeRemainder);
                        mapper.addConversion(1, group.dummy(), alternative);
                    }
                }
            }
        }
        if (inputs.isEmpty()) return false;
        mapper.addConversion(output.getCount(), NSSItem.createItem(output), inputs);
        return true;
    }

    private static void add(Object2IntMap<NormalizedSimpleStack> inputs, ItemStack stack, boolean includeRemainder) {
        inputs.mergeInt(NSSItem.createItem(stack), 1, Integer::sum);
        if (includeRemainder && stack.hasCraftingRemainingItem()) {
            ItemStack remainder = stack.getCraftingRemainingItem();
            if (!remainder.isEmpty()) inputs.mergeInt(NSSItem.createItem(remainder), -1, Integer::sum);
        }
    }
}
