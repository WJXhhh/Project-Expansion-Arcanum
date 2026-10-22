package com.wjx.neoforge.projectexa.integrations.goety;

import com.Polarice3.Goety.common.crafting.BrewingRecipe;
import com.Polarice3.Goety.common.crafting.CauldronRecipe;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.wjx.neoforge.projectexa.integrations.jei.GoetyJeiRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import java.util.ArrayList;
import java.util.List;

public final class GoetyRecipeIntegration {
    private GoetyRecipeIntegration() {}

    public static List<Ingredient> ingredients(Recipe<?> recipe, ResourceLocation category) {
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        if (type == null || !"goety".equals(type.getNamespace())) return List.of();
        List<Ingredient> result = new ArrayList<>(recipe.getIngredients());
        if (GoetyJeiRecipeTypes.isRitual(category) && recipe instanceof RitualRecipe ritual) result.add(0, ritual.getActivationItem());
        else if (!GoetyJeiRecipeTypes.isStaticRecipeType(category) || !category.equals(type)) return List.of();
        if (recipe instanceof CauldronRecipe cauldron && cauldron.getTakeWith() != null && !cauldron.getTakeWith().isEmpty()) result.add(cauldron.getTakeWith());
        return result;
    }

    public static List<Ingredient> brewingIngredients(RecipeManager manager, ItemStack catalyst) {
        if (catalyst.isEmpty()) return List.of();
        boolean known = manager.getRecipes().stream().anyMatch(holder -> holder.value() instanceof BrewingRecipe brew && brew.getInput().test(catalyst));
        boolean builtIn = BrewEffects.INSTANCE != null && BrewEffects.INSTANCE.getEffectFromCatalyst(catalyst.getItem()) != null;
        if (!known && !builtIn) return List.of();
        return List.of(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(catalyst.copyWithCount(1)));
    }
}
