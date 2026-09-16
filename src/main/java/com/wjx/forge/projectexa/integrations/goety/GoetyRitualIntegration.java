package com.wjx.forge.projectexa.integrations.goety;

import com.wjx.forge.projectexa.util.Util;
import com.Polarice3.Goety.common.crafting.ModRecipeSerializer;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Server-side Goety ritual integration. Every request is validated again here. */
public final class GoetyRitualIntegration {
    private GoetyRitualIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeId) {
        if (recipeId == null || player.level().getServer() == null) {
            return;
        }

        RitualRecipe recipe = findRecipe(player.level().getServer().getRecipeManager(), recipeId);
        if (recipe == null) {
            return;
        }

        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            return;
        }

        TransmutationInventory transmutation = new TransmutationInventory(player);
        BigInteger availableEmc = transmutation.getAvailableEmc();
        List<ItemStack> inventory = copyInventory(player);

        for (Ingredient ingredient : requiredIngredients(recipe)) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }

            // Reserve one real stack from a snapshot so duplicate ingredients are
            // counted correctly without treating items generated earlier in this
            // request as pre-existing inventory.
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
    private static RitualRecipe findRecipe(RecipeManager recipeManager, ResourceLocation recipeId) {
        return recipeManager.getAllRecipesFor(ModRecipeSerializer.RITUAL_TYPE.get()).stream()
                .filter(recipe -> recipe.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
    }

    private static List<Ingredient> requiredIngredients(RitualRecipe recipe) {
        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients().size() + 1);
        ingredients.add(recipe.getActivationItem());
        ingredients.addAll(recipe.getIngredients());
        return ingredients;
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
