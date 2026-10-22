package com.wjx.neoforge.projectexa.integrations.jei;

import com.hollingsworth.arsnouveau.common.crafting.recipes.ArmorUpgradeRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantmentRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.BuddingConversionRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ScryRitualRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.CrushRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Resource-location checks for the Ars Nouveau JEI categories handled by ProjectExA. */
public final class ArsNouveauJeiRecipeTypes {
    public static final String NAMESPACE = "ars_nouveau";

    public static final ResourceLocation GLYPH = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "glyph");
    public static final ResourceLocation APPARATUS = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "enchanting_apparatus");
    public static final ResourceLocation ENCHANTMENT = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "enchantment");
    public static final ResourceLocation ARMOR_UPGRADE = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "armor_upgrade");
    public static final ResourceLocation IMBUEMENT = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "imbuement");
    public static final ResourceLocation CRUSH = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "crush");
    public static final ResourceLocation BUDDING_CONVERSION = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "budding_conversion");
    public static final ResourceLocation SCRY_RITUAL = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "scry_ritual");

    private ArsNouveauJeiRecipeTypes() {
    }

    public static boolean isSupported(ResourceLocation recipeType) {
        return recipeType != null
                && NAMESPACE.equals(recipeType.getNamespace())
                && switch (recipeType.getPath()) {
            case "glyph", "enchanting_apparatus", "enchantment", "armor_upgrade",
                 "imbuement", "crush", "budding_conversion", "scry_ritual" -> true;
            default -> false;
        };
    }

    /** Returns the JEI category id used for a recipe object, or {@code null} if it is not supported. */
    @Nullable
    public static ResourceLocation getTypeForRecipe(Object recipe) {
        if (recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) recipe = holder.value();
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
        if (recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder) recipe = holder.value();
        return recipe instanceof EnchantmentRecipe || recipe instanceof ArmorUpgradeRecipe;
    }
}
