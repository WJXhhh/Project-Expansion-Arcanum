package com.wjx.forge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Collection;

@RecipeTypeMapper(requiredMods = GoetyIntegration.MOD_ID)
public class CauldronRecipeMapper extends BaseRecipeTypeMapper {
    @Override
    public String getName() {
        return "GoetyCauldronRecipe";
    }

    @Override
    public String getDescription() {
        return "Adds EMC conversions for Goety cauldron recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return GoetyRecipeTypeResolver.is(recipeType, "cauldron");
    }

    @Override
    protected Collection<Ingredient> getIngredients(Recipe<?> recipe) {
        Collection<Ingredient> ingredients = new ArrayList<>(super.getIngredients(recipe));
        Ingredient takeWith = GoetyRecipeTypeResolver.getCauldronTakeWith(recipe);
        if (takeWith != null && !takeWith.isEmpty()) {
            // Goety consumes the take_with item when the cauldron product is collected.
            ingredients.add(takeWith);
        }
        // The cauldron's soulCost is deliberately omitted: souls do not have EMC.
        return ingredients;
    }
}
