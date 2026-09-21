package com.wjx.forge.projectexa.integrations.goety;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Resolves Goety recipe types without linking against Goety's version-specific
 * {@code ModRecipeSerializer} fields.
 *
 * <p>Goety 2.5.52.1 does not define the cauldron recipe type at all, while
 * newer releases do. Looking up the registered ID keeps the optional
 * integration binary-compatible with both API shapes.</p>
 */
public final class GoetyRecipeTypeResolver {
    private static final String NAMESPACE = GoetyIntegration.MOD_ID;

    private GoetyRecipeTypeResolver() {
    }

    /**
     * Returns the registered Goety recipe type, or {@code null} when this
     * Goety release does not provide that type.
     */
    @Nullable
    public static RecipeType<?> get(String path) {
        ResourceLocation id = new ResourceLocation(NAMESPACE, path);
        RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(id);
        return id.equals(ForgeRegistries.RECIPE_TYPES.getKey(type)) ? type : null;
    }

    public static boolean is(@Nullable RecipeType<?> type, String path) {
        if (type == null) {
            return false;
        }
        return new ResourceLocation(NAMESPACE, path).equals(ForgeRegistries.RECIPE_TYPES.getKey(type));
    }

    /**
     * Uses a wildcard/raw bridge because RecipeManager's recipe type generic
     * is tied to the container type declared by each optional mod recipe.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<? extends Recipe<?>> getRecipes(RecipeManager recipeManager, String path) {
        RecipeType<?> type = get(path);
        if (type == null) {
            return List.of();
        }
        return (List) recipeManager.getAllRecipesFor((RecipeType) type);
    }

    /**
     * Reads the newer cauldron recipe's optional take-with ingredient without
     * putting CauldronRecipe in this class's linkage table. Older Goety builds
     * do not ship that class.
     */
    @Nullable
    public static Ingredient getCauldronTakeWith(@Nullable Recipe<?> recipe) {
        if (recipe == null || !is(recipe.getType(), "cauldron")) {
            return null;
        }

        try {
            Object value = recipe.getClass().getMethod("getTakeWith").invoke(recipe);
            return value instanceof Ingredient ingredient ? ingredient : null;
        } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
            return null;
        }
    }
}
