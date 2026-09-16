package com.wjx.forge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RecipeTypeMapper(priority = 1, requiredMods = GoetyIntegration.MOD_ID)
public class GoetyTotemCraftingMapper implements moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper {
    private static final ResourceLocation ROOT_TOTEM_ID =
            new ResourceLocation(GoetyIntegration.MOD_ID, "totem_of_roots");

    @Override
    public String getName() {
        return "GoetyTotemCrafting";
    }

    @Override
    public String getDescription() {
        return "Adds EMC conversions for Goety crafting recipes that use a Totem of Roots.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return recipeType == RecipeType.CRAFTING;
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper,
                                Recipe<?> recipe,
                                RegistryAccess registryAccess,
                                INSSFakeGroupManager fakeGroupManager) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (!containsRootTotem(ingredients)) {
            return false;
        }

        ItemStack result = recipe.getResultItem(registryAccess);
        if (result.isEmpty()) {
            return true;
        }

        Map<NormalizedSimpleStack, Integer> inputMap = new HashMap<>();
        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks = ingredient.getItems();
            if (matchingStacks.length == 0) {
                continue;
            }

            if (containsRootTotem(matchingStacks)) {
                addIngredient(inputMap, NSSItem.createItem(ROOT_TOTEM_ID), 1);
            } else if (matchingStacks.length == 1) {
                addConsumedItem(inputMap, matchingStacks[0]);
            } else {
                addCandidateGroup(mapper, inputMap, matchingStacks, fakeGroupManager);
            }
        }

        mapper.addConversion(result.getCount(), NSSItem.createItem(result), inputMap);
        return true;
    }

    private static boolean containsRootTotem(Iterable<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            if (containsRootTotem(ingredient.getItems())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsRootTotem(ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (isRootTotem(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRootTotem(ItemStack stack) {
        return !stack.isEmpty() && ROOT_TOTEM_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static void addCandidateGroup(IMappingCollector<NormalizedSimpleStack, Long> mapper,
                                          Map<NormalizedSimpleStack, Integer> inputMap,
                                          ItemStack[] matchingStacks,
                                          INSSFakeGroupManager fakeGroupManager) {
        Set<NormalizedSimpleStack> candidates = new HashSet<>();
        List<ItemStack> uniqueStacks = new ArrayList<>();
        for (ItemStack stack : matchingStacks) {
            if (!stack.isEmpty()) {
                NormalizedSimpleStack normalized = NSSItem.createItem(stack);
                if (candidates.add(normalized)) {
                    uniqueStacks.add(stack);
                }
            }
        }

        if (uniqueStacks.isEmpty()) {
            return;
        }
        if (uniqueStacks.size() == 1) {
            addConsumedItem(inputMap, uniqueStacks.get(0));
            return;
        }

        Tuple<NormalizedSimpleStack, Boolean> fakeGroup =
                fakeGroupManager.getOrCreateFakeGroup(candidates);
        addIngredient(inputMap, fakeGroup.getA(), 1);
        if (fakeGroup.getB()) {
            for (ItemStack stack : uniqueStacks) {
                Map<NormalizedSimpleStack, Integer> candidateMap = new HashMap<>();
                addConsumedItem(candidateMap, stack);
                mapper.addConversion(1, fakeGroup.getA(), candidateMap);
            }
        }
    }

    private static void addConsumedItem(Map<NormalizedSimpleStack, Integer> inputMap, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        Item item = stack.getItem();
        if (item.hasCraftingRemainingItem(stack)) {
            ItemStack remainingItem = item.getCraftingRemainingItem(stack);
            if (!remainingItem.isEmpty()) {
                addIngredient(inputMap, NSSItem.createItem(remainingItem), -1);
            }
        }
        addIngredient(inputMap, NSSItem.createItem(stack), 1);
    }

    private static void addIngredient(Map<NormalizedSimpleStack, Integer> inputMap,
                                      NormalizedSimpleStack ingredient,
                                      int amount) {
        inputMap.merge(ingredient, amount, Integer::sum);
    }
}
