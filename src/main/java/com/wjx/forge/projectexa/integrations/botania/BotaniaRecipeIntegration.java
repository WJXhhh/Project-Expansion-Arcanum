package com.wjx.forge.projectexa.integrations.botania;

import com.wjx.forge.projectexa.integrations.jei.BotaniaJeiRecipeTypes;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import vazkii.botania.api.recipe.BotanicalBreweryRecipe;
import vazkii.botania.api.recipe.ElvenTradeRecipe;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.api.recipe.OrechidRecipe;
import vazkii.botania.api.recipe.PetalApothecaryRecipe;
import vazkii.botania.api.recipe.PureDaisyRecipe;
import vazkii.botania.api.recipe.RunicAltarRecipe;
import vazkii.botania.api.recipe.StateIngredient;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.crafting.BotaniaRecipeTypes;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Server-side transmutation support for Botania's special JEI recipes. */
public final class BotaniaRecipeIntegration {
    private BotaniaRecipeIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeTypeId,
                                 ResourceLocation recipeId) {
        if (recipeTypeId == null || !BotaniaJeiRecipeTypes.isSupported(recipeTypeId)
                || recipeId == null || player.level().getServer() == null) {
            return;
        }

        List<Ingredient> ingredients = requiredIngredients(
                player.level().getServer().getRecipeManager(), recipeTypeId, recipeId);
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
                                                        ResourceLocation recipeId) {
        Recipe<?> recipe = findRecipe(recipeManager, recipeTypeId.getPath(), recipeId);
        if (recipe == null) {
            return null;
        }

        List<Ingredient> ingredients = new ArrayList<>();
        if (recipe instanceof BotanicalBreweryRecipe breweryRecipe) {
            ingredients.addAll(breweryRecipe.getIngredients());
        } else if (recipe instanceof ElvenTradeRecipe elvenTradeRecipe) {
            ingredients.addAll(elvenTradeRecipe.getIngredients());
        } else if (recipe instanceof ManaInfusionRecipe manaRecipe) {
            ingredients.addAll(manaRecipe.getIngredients());
            addStateIngredient(ingredients, manaRecipe.getRecipeCatalyst());
        } else if (recipe instanceof PetalApothecaryRecipe petalRecipe) {
            // The apothecary is shown in JEI with water and the recipe reagent
            // before the recipe's actual petal inputs.
            ingredients.add(Ingredient.of(Items.WATER_BUCKET));
            addIngredient(ingredients, petalRecipe.getReagent());
            ingredients.addAll(petalRecipe.getIngredients());
        } else if (recipe instanceof PureDaisyRecipe pureDaisyRecipe) {
            addStateIngredient(ingredients, pureDaisyRecipe.getInput());
        } else if (recipe instanceof RunicAltarRecipe runicRecipe) {
            addIngredient(ingredients, runicRecipe.getReagent());
            ingredients.addAll(runicRecipe.getIngredients());
        } else if (recipe instanceof TerrestrialAgglomerationRecipe terraPlateRecipe) {
            ingredients.addAll(terraPlateRecipe.getIngredients());
        } else if (recipe instanceof OrechidRecipe orechidRecipe) {
            addStateIngredient(ingredients, orechidRecipe.getInput());
        }
        return ingredients;
    }

    @Nullable
    private static Recipe<?> findRecipe(RecipeManager recipeManager, String path, ResourceLocation recipeId) {
        return switch (path) {
            case "brewery" -> findRecipe(recipeManager, BotaniaRecipeTypes.BREW_TYPE, recipeId);
            case "elven_trade" -> findRecipe(recipeManager, BotaniaRecipeTypes.ELVEN_TRADE_TYPE, recipeId);
            case "mana_pool" -> findRecipe(recipeManager, BotaniaRecipeTypes.MANA_INFUSION_TYPE, recipeId);
            case "petals" -> findRecipe(recipeManager, BotaniaRecipeTypes.PETAL_TYPE, recipeId);
            case "pure_daisy" -> findRecipe(recipeManager, BotaniaRecipeTypes.PURE_DAISY_TYPE, recipeId);
            case "runic_altar" -> findRecipe(recipeManager, BotaniaRecipeTypes.RUNE_TYPE, recipeId);
            case "terra_plate" -> findRecipe(recipeManager, BotaniaRecipeTypes.TERRA_PLATE_TYPE, recipeId);
            case "orechid" -> findRecipe(recipeManager, BotaniaRecipeTypes.ORECHID_TYPE, recipeId);
            case "orechid_ignem" -> findRecipe(recipeManager, BotaniaRecipeTypes.ORECHID_IGNEM_TYPE, recipeId);
            case "marimorphosis" -> findRecipe(recipeManager, BotaniaRecipeTypes.MARIMORPHOSIS_TYPE, recipeId);
            default -> null;
        };
    }

    @Nullable
    private static <C extends Container, T extends Recipe<C>> T findRecipe(RecipeManager recipeManager,
                                                                            RecipeType<T> recipeType,
                                                                            ResourceLocation recipeId) {
        return recipeManager.getAllRecipesFor(recipeType).stream()
                .filter(recipe -> recipe.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
    }

    private static void addIngredient(List<Ingredient> ingredients, @Nullable Ingredient ingredient) {
        if (ingredient != null && !ingredient.isEmpty()) {
            ingredients.add(ingredient);
        }
    }

    private static void addStateIngredient(List<Ingredient> ingredients, @Nullable StateIngredient stateIngredient) {
        if (stateIngredient == null) {
            return;
        }

        List<ItemStack> displayedStacks = stateIngredient.getDisplayedStacks().stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithCount(1))
                .toList();
        if (!displayedStacks.isEmpty()) {
            ingredients.add(Ingredient.of(displayedStacks.toArray(ItemStack[]::new)));
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
