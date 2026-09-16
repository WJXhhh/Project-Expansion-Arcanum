package com.wjx.forge.projectexa.integrations.jei;

import net.minecraft.resources.ResourceLocation;

/** Resource-location checks for the recipe categories exposed by Goety's JEI plugin. */
public final class GoetyJeiRecipeTypes {
    private GoetyJeiRecipeTypes() {
    }

    public static boolean isSupported(ResourceLocation recipeType) {
        return recipeType != null
                && "goety".equals(recipeType.getNamespace())
                && (isRitual(recipeType) || isStaticRecipeType(recipeType));
    }

    public static boolean isRitual(ResourceLocation recipeType) {
        if (recipeType == null || !"goety".equals(recipeType.getNamespace())) {
            return false;
        }
        String path = recipeType.getPath();
        return "ritual".equals(path) || path.startsWith("ritual_");
    }

    public static boolean isStaticRecipeType(ResourceLocation recipeType) {
        if (recipeType == null || !"goety".equals(recipeType.getNamespace())) {
            return false;
        }
        return switch (recipeType.getPath()) {
            case "cursed_infuser", "brazier", "cauldron", "pulverize", "soul_absorber", "brewing" -> true;
            default -> false;
        };
    }
}
