package com.wjx.forge.projectexa.integrations.goety;

import com.Polarice3.Goety.common.crafting.BrewingRecipe;
import com.Polarice3.Goety.common.effects.brew.BrewEffects;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Server-side transmutation support for Goety's non-ritual JEI recipes. */
public final class GoetyRecipeIntegration {
    private static final String GOETY = GoetyIntegration.MOD_ID;

    private GoetyRecipeIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeTypeId,
                                 @Nullable ResourceLocation recipeId,
                                 @Nullable ItemStack brewingCatalyst) {
        if (recipeTypeId == null || !GOETY.equals(recipeTypeId.getNamespace())
                || player.level().getServer() == null) {
            return;
        }

        List<Ingredient> ingredients = requiredIngredients(
                player.level().getServer().getRecipeManager(), recipeTypeId, recipeId, brewingCatalyst);
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }

        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            return;
        }

        TransmutationInventory transmutation = new TransmutationInventory(player);
        BigInteger availableEmc = transmutation.getAvailableEmc();
        List<ItemStack> inventory = copyInventory(player);

        for (Ingredient ingredient : ingredients) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }

            // Reserve one real stack from a snapshot so duplicate ingredients are
            // counted correctly without treating generated items as pre-existing.
            if (consumeInventoryMatch(ingredient, inventory)) {
                continue;
            }

            Candidate candidate = findCheapestTransmutable(ingredient, provider);
            if (candidate == null || availableEmc.compareTo(candidate.cost()) < 0) {
                continue;
            }

            transmutation.removeEmc(candidate.cost());
            availableEmc = availableEmc.subtract(candidate.cost());
            Util.returnToInventory(player.getInventory(), player, candidate.stack().copyWithCount(1), true);
        }
    }

    @Nullable
    private static List<Ingredient> requiredIngredients(RecipeManager recipeManager,
                                                        ResourceLocation recipeTypeId,
                                                        @Nullable ResourceLocation recipeId,
                                                        @Nullable ItemStack brewingCatalyst) {
        String path = recipeTypeId.getPath();
        if ("brewing".equals(path)) {
            return brewingIngredients(recipeManager, brewingCatalyst);
        }

        if (recipeId == null) {
            return null;
        }

        Recipe<?> recipe = findRecipe(recipeManager, path, recipeId);
        if (recipe == null) {
            return null;
        }

        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
        Ingredient takeWith = GoetyRecipeTypeResolver.getCauldronTakeWith(recipe);
        if (takeWith != null && !takeWith.isEmpty()) {
            // Goety consumes this item when the cauldron product is collected.
            ingredients.add(takeWith);
        }
        return ingredients;
    }

    @Nullable
    private static Recipe<?> findRecipe(RecipeManager recipeManager, String path, ResourceLocation recipeId) {
        return GoetyRecipeTypeResolver.getRecipes(recipeManager, path).stream()
                .filter(recipe -> recipe.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    private static List<Ingredient> brewingIngredients(RecipeManager recipeManager,
                                                       @Nullable ItemStack catalyst) {
        if (catalyst == null || catalyst.isEmpty()) {
            return null;
        }

        boolean hasRecipe = GoetyRecipeTypeResolver.getRecipes(recipeManager, "brewing").stream()
                .filter(recipe -> recipe instanceof BrewingRecipe)
                .map(recipe -> (BrewingRecipe) recipe)
                .anyMatch(recipe -> recipe.getInput().test(catalyst));
        boolean hasBuiltInEffect = BrewEffects.INSTANCE != null
                && BrewEffects.INSTANCE.getEffectFromCatalyst(catalyst.getItem()) != null;
        if (!hasRecipe && !hasBuiltInEffect) {
            return null;
        }

        // The bottle is the actual input shown by WitchBrewCategory. The
        // catalyst is sent as a concrete stack so a tag does not silently turn
        // into a different, cheaper catalyst on the server.
        return List.of(Ingredient.of(Items.GLASS_BOTTLE), Ingredient.of(catalyst.copyWithCount(1)));
    }

    private static List<ItemStack> copyInventory(ServerPlayer player) {
        List<ItemStack> inventory = new ArrayList<>(player.getInventory().getContainerSize());
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            inventory.add(player.getInventory().getItem(index).copy());
        }
        return inventory;
    }

    private static boolean consumeInventoryMatch(Ingredient ingredient, List<ItemStack> inventory) {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Candidate findCheapestTransmutable(Ingredient ingredient, IKnowledgeProvider provider) {
        Candidate best = null;
        for (ItemStack option : ingredient.getItems()) {
            ItemStack clean = Util.cleanStack(option);
            if (clean.isEmpty() || !provider.hasKnowledge(clean)) {
                continue;
            }

            long value = IEMCProxy.INSTANCE.getValue(clean);
            if (value <= 0) {
                continue;
            }

            Candidate candidate = new Candidate(clean, BigInteger.valueOf(value));
            if (best == null || candidate.cost().compareTo(best.cost()) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private record Candidate(ItemStack stack, BigInteger cost) {
    }
}
