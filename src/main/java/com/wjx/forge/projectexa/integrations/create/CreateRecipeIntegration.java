package com.wjx.forge.projectexa.integrations.create;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.wjx.forge.projectexa.integrations.jei.CreateJeiRecipeTypes;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Server-side transmutation support for Create's special JEI recipes. */
public final class CreateRecipeIntegration {
    private static final String CREATE = CreateJeiRecipeTypes.NAMESPACE;

    private CreateRecipeIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeTypeId,
                                 @Nullable ResourceLocation recipeId,
                                 List<List<ItemStack>> fallbackIngredients) {
        if (recipeTypeId == null || !CreateJeiRecipeTypes.isSupportedRecipeType(recipeTypeId)
                || player.level().getServer() == null) {
            return;
        }

        RecipeManager recipeManager = player.level().getServer().getRecipeManager();
        Recipe<?> recipe = findRecipe(recipeManager, recipeTypeId, recipeId);
        List<Ingredient> ingredients = recipe == null
                ? fallbackIngredients(fallbackIngredients)
                : requiredIngredients(recipe);
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
    private static Recipe<?> findRecipe(RecipeManager recipeManager, ResourceLocation recipeTypeId,
                                        @Nullable ResourceLocation recipeId) {
        if (recipeId == null) {
            return null;
        }

        Recipe<?> exact = recipeManager.getRecipes().stream()
                .filter(recipe -> recipeId.equals(recipe.getId()))
                .filter(recipe -> recipeTypeId.equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType())))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }

        // Create's automatic basin categories wrap a vanilla recipe while
        // retaining its id. Recover the authoritative vanilla recipe when the
        // client-side wrapper reports create:basin.
        if (CREATE.equals(recipeTypeId.getNamespace()) && "basin".equals(recipeTypeId.getPath())) {
            return recipeManager.getRecipes().stream()
                    .filter(recipe -> recipeId.equals(recipe.getId()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private static List<Ingredient> requiredIngredients(Recipe<?> recipe) {
        if (recipe instanceof SequencedAssemblyRecipe sequencedRecipe) {
            return sequencedIngredients(sequencedRecipe);
        }

        if (recipe instanceof ProcessingRecipe<?> processingRecipe) {
            return new ArrayList<>(processingRecipe.getIngredients());
        }
        return new ArrayList<>(recipe.getIngredients());
    }

    private static List<Ingredient> sequencedIngredients(SequencedAssemblyRecipe recipe) {
        List<Ingredient> ingredients = new ArrayList<>();
        addIngredient(ingredients, recipe.getIngredient());

        for (int loop = 0; loop < recipe.getLoops(); loop++) {
            for (SequencedRecipe<?> step : recipe.getSequence()) {
                IAssemblyRecipe assemblyRecipe = step.getAsAssemblyRecipe();
                assemblyRecipe.addAssemblyIngredients(ingredients);
            }
        }
        return ingredients;
    }

    private static List<Ingredient> fallbackIngredients(List<List<ItemStack>> fallbackIngredients) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (List<ItemStack> options : fallbackIngredients) {
            ItemStack[] stacks = options.stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(stack -> stack.copyWithCount(1))
                    .toArray(ItemStack[]::new);
            if (stacks.length > 0) {
                ingredients.add(Ingredient.of(stacks));
            }
        }
        return ingredients;
    }

    private static void addIngredient(List<Ingredient> ingredients, @Nullable Ingredient ingredient) {
        if (ingredient != null && !ingredient.isEmpty()) {
            ingredients.add(ingredient);
        }
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
