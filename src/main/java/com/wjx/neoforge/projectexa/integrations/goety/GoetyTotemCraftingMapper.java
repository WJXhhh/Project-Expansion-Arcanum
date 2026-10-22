package com.wjx.neoforge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

@RecipeTypeMapper(priority = 1, requiredMods = "goety")
public final class GoetyTotemCraftingMapper implements IRecipeTypeMapper {
    private static final ResourceLocation ROOT = ResourceLocation.fromNamespaceAndPath("goety", "totem_of_roots");
    @Override public String getTranslationKey() { return "projectexa.integration.goety.totem"; }
    @Override public String getName() { return "GoetyTotemCrafting"; }
    @Override public String getDescription() { return "Count the consumed Totem of Roots rather than treating it as a reusable crafting container."; }
    @Override public boolean canHandle(RecipeType<?> type) { return type == RecipeType.CRAFTING; }
    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> holder,
                                RegistryAccess access, INSSFakeGroupManager groups) {
        var ingredients = holder.value().getIngredients();
        if (ingredients.stream().noneMatch(GoetyTotemCraftingMapper::hasRoot)) return false;
        GoetyIngredientMapping.map(mapper, groups, holder.value().getResultItem(access), ingredients, GoetyTotemCraftingMapper::hasRoot);
        return true;
    }
    private static boolean hasRoot(Ingredient ingredient) {
        for (var stack : ingredient.getItems()) if (!stack.isEmpty() && ROOT.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
        return false;
    }
}
