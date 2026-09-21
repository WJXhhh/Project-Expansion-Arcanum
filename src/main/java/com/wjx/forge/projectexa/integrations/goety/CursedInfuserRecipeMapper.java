package com.wjx.forge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;
import net.minecraft.world.item.crafting.RecipeType;

@RecipeTypeMapper(requiredMods = GoetyIntegration.MOD_ID)
public class CursedInfuserRecipeMapper extends BaseRecipeTypeMapper {
    @Override
    public String getName() {
        return "GoetyCursedInfuserRecipe";
    }

    @Override
    public String getDescription() {
        return "Adds EMC conversions for Goety cursed infuser recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return GoetyRecipeTypeResolver.is(recipeType, "cursed_infuser");
    }
}
