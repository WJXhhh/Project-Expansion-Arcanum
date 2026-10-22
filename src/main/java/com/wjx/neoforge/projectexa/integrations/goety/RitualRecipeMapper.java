package com.wjx.neoforge.projectexa.integrations.goety;

import com.Polarice3.Goety.common.crafting.RitualRecipe;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import moze_intel.projecte.emc.mappers.recipe.BaseRecipeTypeMapper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;

@RecipeTypeMapper(requiredMods = "goety")
public final class RitualRecipeMapper extends BaseRecipeTypeMapper {
    @Override public String getTranslationKey() { return "projectexa.integration.goety.ritual"; }
    @Override public String getName() { return "GoetyRitualRecipe"; }
    @Override public String getDescription() { return "Goety rituals, including their consumed activation item, without pricing souls or entities."; }
    @Override public boolean canHandle(RecipeType<?> type) { return GoetyRecipeTypeResolver.is(type, "ritual"); }
    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> holder,
                                RegistryAccess access, INSSFakeGroupManager groups) {
        if (!(holder.value() instanceof RitualRecipe ritual)) return false;
        if (ritual.isSummoning() || ritual.isConversion()) return true;
        var ingredients = new ArrayList<Ingredient>(ritual.getIngredients());
        Ingredient activation = ritual.getActivationItem();
        ingredients.add(activation);
        GoetyIngredientMapping.map(mapper, groups, ritual.getResultItem(access), ingredients, ingredient -> ingredient == activation);
        return true;
    }
}
