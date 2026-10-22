package com.wjx.neoforge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;
import net.minecraft.world.item.crafting.RecipeType;

@RecipeTypeMapper(requiredMods = "goety")
public class BrazierRecipeMapper extends BaseRecipeTypeMapper {
    @Override public String getTranslationKey() { return "projectexa.integration.goety.brazier"; }
    @Override
    public String getName() {
        return "GoetyBrazierRecipe";
    }

    @Override
    public String getDescription() {
        return "Adds EMC conversions for Goety brazier recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        return GoetyRecipeTypeResolver.is(recipeType, "brazier");
    }
}
