package com.wjx.forge.projectexa.integrations.goety;

import com.Polarice3.Goety.common.crafting.RitualRecipe;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.emc.IngredientMap;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RecipeTypeMapper(requiredMods = GoetyIntegration.MOD_ID)
public class RitualRecipeMapper extends BaseRecipeTypeMapper {
    @Override
    public String getName() {
        return "GoetyRitualRecipe";
    }

    @Override
    public String getDescription() {
        return "Adds EMC conversions for Goety ritual recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return GoetyRecipeTypeResolver.is(recipeType, "ritual");
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper,
                                Recipe<?> recipe,
                                RegistryAccess registryAccess,
                                INSSFakeGroupManager fakeGroupManager) {
        RitualRecipe ritualRecipe = (RitualRecipe) recipe;
        if (ritualRecipe.isSummoning() || ritualRecipe.isConversion()) {
            return true;
        }

        ItemStack result = recipe.getResultItem(registryAccess);
        if (result.isEmpty()) {
            return false;
        }

        Collection<Ingredient> ingredients = new ArrayList<>(ritualRecipe.getIngredients());
        Ingredient activationItem = ritualRecipe.getActivationItem();
        if (activationItem != null) {
            ingredients.add(activationItem);
        }

        IngredientMap<NormalizedSimpleStack> ingredientMap = new IngredientMap<>();
        List<Tuple<NormalizedSimpleStack, List<IngredientMap<NormalizedSimpleStack>>>> alternatives = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            ItemStack[] matchingStacks;
            try {
                matchingStacks = ingredient.getItems();
            } catch (Exception exception) {
                addAlternativeConversions(mapper, alternatives);
                return true;
            }

            if (matchingStacks.length == 0) {
                addAlternativeConversions(mapper, alternatives);
                return false;
            }

            // Goety consumes the activation item when the ritual finishes. It is
            // not a crafting-container item, even when the item itself reports a
            // crafting remainder (for example, the Philosopher's Stone).
            boolean isActivationItem = ingredient == activationItem;
            if (matchingStacks.length == 1) {
                ItemStack stack = matchingStacks[0];
                if (stack.isEmpty()) {
                    addAlternativeConversions(mapper, alternatives);
                    return false;
                }
                if (addIngredient(ingredientMap, stack, !isActivationItem)) {
                    addAlternativeConversions(mapper, alternatives);
                    return true;
                }
                continue;
            }

            Set<NormalizedSimpleStack> matchingNSS = new HashSet<>();
            List<ItemStack> nonEmptyStacks = new ArrayList<>();
            for (ItemStack stack : matchingStacks) {
                if (!stack.isEmpty()) {
                    matchingNSS.add(NSSItem.createItem(stack));
                    nonEmptyStacks.add(stack);
                }
            }
            if (nonEmptyStacks.isEmpty()) {
                addAlternativeConversions(mapper, alternatives);
                return false;
            }
            if (nonEmptyStacks.size() == 1) {
                if (addIngredient(ingredientMap, nonEmptyStacks.get(0), !isActivationItem)) {
                    addAlternativeConversions(mapper, alternatives);
                    return true;
                }
                continue;
            }

            Tuple<NormalizedSimpleStack, Boolean> fakeGroup = fakeGroupManager.getOrCreateFakeGroup(matchingNSS);
            NormalizedSimpleStack fakeIngredient = fakeGroup.getA();
            ingredientMap.addIngredient(fakeIngredient, 1);
            if (fakeGroup.getB()) {
                List<IngredientMap<NormalizedSimpleStack>> alternativeMaps = new ArrayList<>();
                for (ItemStack stack : nonEmptyStacks) {
                    IngredientMap<NormalizedSimpleStack> alternativeMap = new IngredientMap<>();
                    if (addIngredient(alternativeMap, stack, !isActivationItem)) {
                        addAlternativeConversions(mapper, alternatives);
                        return true;
                    }
                    alternativeMaps.add(alternativeMap);
                }
                alternatives.add(new Tuple<>(fakeIngredient, alternativeMaps));
            }
        }

        mapper.addConversion(result.getCount(), NSSItem.createItem(result), ingredientMap.getMap());
        addAlternativeConversions(mapper, alternatives);
        return true;
    }

    private boolean addIngredient(IngredientMap<NormalizedSimpleStack> ingredients,
                                  ItemStack stack,
                                  boolean includeCraftingRemainder) {
        if (includeCraftingRemainder && stack.getItem().hasCraftingRemainingItem(stack)) {
            ItemStack remainder = stack.getItem().getCraftingRemainingItem(stack);
            if (remainder.isEmpty()) {
                return true;
            }
            ingredients.addIngredient(NSSItem.createItem(remainder), -1);
        }
        ingredients.addIngredient(NSSItem.createItem(stack), 1);
        return false;
    }

    private void addAlternativeConversions(
            IMappingCollector<NormalizedSimpleStack, Long> mapper,
            List<Tuple<NormalizedSimpleStack, List<IngredientMap<NormalizedSimpleStack>>>> alternatives) {
        for (Tuple<NormalizedSimpleStack, List<IngredientMap<NormalizedSimpleStack>>> alternative : alternatives) {
            for (IngredientMap<NormalizedSimpleStack> ingredientMap : alternative.getB()) {
                mapper.addConversion(1, alternative.getA(), ingredientMap.getMap());
            }
        }
    }
}
