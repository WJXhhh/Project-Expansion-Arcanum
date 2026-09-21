package com.wjx.forge.projectexa.integrations.jei;

import com.hollingsworth.arsnouveau.api.enchanting_apparatus.ArmorUpgradeRecipe;
import com.hollingsworth.arsnouveau.api.enchanting_apparatus.EnchantmentRecipe;
import com.hollingsworth.arsnouveau.api.enchanting_apparatus.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.api.recipe.BuddingConversionRecipe;
import com.hollingsworth.arsnouveau.api.recipe.ScryRitualRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.CrushRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Resource-location checks for the Ars Nouveau JEI categories handled by ProjectExA. */
public final class ArsNouveauJeiRecipeTypes {
    public static final String NAMESPACE = "ars_nouveau";

    public static final ResourceLocation GLYPH = new ResourceLocation(NAMESPACE, "glyph_recipe");
    public static final ResourceLocation APPARATUS = new ResourceLocation(NAMESPACE, "enchanting_apparatus");
    public static final ResourceLocation ENCHANTMENT = new ResourceLocation(NAMESPACE, "enchantment_apparatus");
    public static final ResourceLocation ARMOR_UPGRADE = new ResourceLocation(NAMESPACE, "armor_upgrade");
    public static final ResourceLocation IMBUEMENT = new ResourceLocation(NAMESPACE, "imbuement");
    public static final ResourceLocation CRUSH = new ResourceLocation(NAMESPACE, "crush");
    public static final ResourceLocation BUDDING_CONVERSION = new ResourceLocation(NAMESPACE, "budding_conversion");
    public static final ResourceLocation SCRY_RITUAL = new ResourceLocation(NAMESPACE, "scry_ritual");

    private ArsNouveauJeiRecipeTypes() {
    }

    public static boolean isSupported(ResourceLocation recipeType) {
        return recipeType != null
                && NAMESPACE.equals(recipeType.getNamespace())
                && switch (recipeType.getPath()) {
            case "glyph_recipe", "enchanting_apparatus", "enchantment_apparatus", "armor_upgrade",
                 "imbuement", "crush", "budding_conversion", "scry_ritual" -> true;
            default -> false;
        };
    }

    /** Returns the JEI category id used for a recipe object, or {@code null} if it is not supported. */
    @Nullable
    public static ResourceLocation getTypeForRecipe(Object recipe) {
        if (recipe instanceof GlyphRecipe) {
            return GLYPH;
        }
        if (recipe instanceof CrushRecipe) {
            return CRUSH;
        }
        if (recipe instanceof ImbuementRecipe) {
            return IMBUEMENT;
        }
        if (recipe instanceof BuddingConversionRecipe) {
            return BUDDING_CONVERSION;
        }
        if (recipe instanceof ScryRitualRecipe) {
            return SCRY_RITUAL;
        }
        if (recipe instanceof EnchantmentRecipe) {
            return ENCHANTMENT;
        }
        if (recipe instanceof ArmorUpgradeRecipe) {
            return ARMOR_UPGRADE;
        }
        if (recipe instanceof EnchantingApparatusRecipe) {
            return APPARATUS;
        }
        return null;
    }

    /** Whether the first JEI input is a dynamic target rather than a fixed recipe ingredient. */
    public static boolean hasDynamicInput(Object recipe) {
        return recipe instanceof EnchantmentRecipe || recipe instanceof ArmorUpgradeRecipe;
    }
}
