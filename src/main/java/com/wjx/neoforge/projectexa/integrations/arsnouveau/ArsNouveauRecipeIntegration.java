package com.wjx.neoforge.projectexa.integrations.arsnouveau;

import com.hollingsworth.arsnouveau.common.crafting.recipes.*;
import com.wjx.neoforge.projectexa.integrations.jei.ArsNouveauJeiRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import java.util.ArrayList;
import java.util.List;

/** Ars 5.x moved the apparatus/scry/budding types to common.crafting.recipes. */
public final class ArsNouveauRecipeIntegration {
    private ArsNouveauRecipeIntegration() {}

    public static List<Ingredient> ingredients(Recipe<?> recipe, ResourceLocation category) {
        if (!category.equals(ArsNouveauJeiRecipeTypes.getTypeForRecipe(recipe))) return List.of();
        List<Ingredient> result = new ArrayList<>();
        if (recipe instanceof GlyphRecipe glyph) result.addAll(glyph.getInputs());
        else if (recipe instanceof CrushRecipe crush) result.add(crush.input());
        else if (recipe instanceof ImbuementRecipe imbuement) {
            result.add(imbuement.getInput()); result.addAll(imbuement.getPedestalItems());
        } else if (recipe instanceof EnchantingApparatusRecipe apparatus) {
            // The center item in these two categories is a dynamic target, not a fixed material.
            if (!(recipe instanceof EnchantmentRecipe) && !(recipe instanceof ArmorUpgradeRecipe)) result.add(apparatus.reagent());
            result.addAll(apparatus.pedestalItems());
        } else if (recipe instanceof BuddingConversionRecipe budding && budding.input().asItem() != Items.AIR) {
            result.add(Ingredient.of(budding.input().asItem()));
        } else if (recipe instanceof ScryRitualRecipe scry) result.add(Ingredient.of(scry.augment()));
        return result;
    }
}
