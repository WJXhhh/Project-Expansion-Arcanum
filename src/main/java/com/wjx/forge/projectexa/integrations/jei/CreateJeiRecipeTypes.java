package com.wjx.forge.projectexa.integrations.jei;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Resource-location checks for the recipe categories exposed by Create's JEI plugin. */
public final class CreateJeiRecipeTypes {
    public static final String NAMESPACE = "create";

    private static final Set<String> CATEGORY_PATHS = Set.of(
            "milling",
            "crushing",
            "pressing",
            "fan_washing",
            "fan_smoking",
            "fan_blasting",
            "fan_haunting",
            "mixing",
            "automatic_shapeless",
            "automatic_brewing",
            "compacting",
            "packing",
            "automatic_packing",
            "sawing",
            "block_cutting",
            "sandpaper_polishing",
            "item_application",
            "deploying",
            "spout_filling",
            "draining",
            "automatic_shaped",
            "mechanical_crafting",
            "sequenced_assembly",
            "mystery_conversion"
    );

    private static final Set<String> CREATE_RECIPE_TYPES = Set.of(
            "basin",
            "crushing",
            "cutting",
            "milling",
            "mixing",
            "compacting",
            "pressing",
            "sandpaper_polishing",
            "splashing",
            "haunting",
            "deploying",
            "filling",
            "emptying",
            "item_application",
            "mechanical_crafting",
            "sequenced_assembly",
            "conversion"
    );

    private static final Set<String> VANILLA_RECIPE_TYPES = Set.of(
            "crafting",
            "smelting",
            "blasting",
            "smoking",
            "stonecutting"
    );

    private CreateJeiRecipeTypes() {
    }

    public static boolean isSupported(ResourceLocation recipeCategory) {
        return recipeCategory != null
                && NAMESPACE.equals(recipeCategory.getNamespace())
                && CATEGORY_PATHS.contains(recipeCategory.getPath());
    }

    /**
     * Returns whether a recipe type can be requested by the Create JEI handler.
     * Vanilla types are included because Create exposes some vanilla recipes in
     * its automatic crafting and fan-processing categories.
     */
    public static boolean isSupportedRecipeType(ResourceLocation recipeType) {
        if (recipeType == null) {
            return false;
        }
        if (NAMESPACE.equals(recipeType.getNamespace())) {
            return CREATE_RECIPE_TYPES.contains(recipeType.getPath());
        }
        return "minecraft".equals(recipeType.getNamespace())
                && VANILLA_RECIPE_TYPES.contains(recipeType.getPath());
    }
}
