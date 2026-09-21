package com.wjx.forge.projectexa.integrations.arsnouveau;

import com.hollingsworth.arsnouveau.api.enchanting_apparatus.ArmorUpgradeRecipe;
import com.hollingsworth.arsnouveau.api.enchanting_apparatus.EnchantmentRecipe;
import com.hollingsworth.arsnouveau.api.enchanting_apparatus.EnchantingApparatusRecipe;
import com.hollingsworth.arsnouveau.api.recipe.BuddingConversionRecipe;
import com.hollingsworth.arsnouveau.api.recipe.ScryRitualRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.CrushRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ImbuementRecipe;
import com.hollingsworth.arsnouveau.setup.registry.RecipeRegistry;
import com.wjx.forge.projectexa.integrations.jei.ArsNouveauJeiRecipeTypes;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Server-side transmutation support for Ars Nouveau's JEI recipes. */
public final class ArsNouveauRecipeIntegration {
    private ArsNouveauRecipeIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeTypeId,
                                 ResourceLocation recipeId) {
        if (recipeTypeId == null || !ArsNouveauJeiRecipeTypes.isSupported(recipeTypeId)
                || recipeId == null || !ArsNouveauJeiRecipeTypes.NAMESPACE.equals(recipeId.getNamespace())
                || player.level().getServer() == null) {
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
        if (recipe instanceof GlyphRecipe glyphRecipe) {
            ingredients.addAll(glyphRecipe.inputs);
        } else if (recipe instanceof CrushRecipe crushRecipe) {
            ingredients.add(crushRecipe.input);
        } else if (recipe instanceof ImbuementRecipe imbuementRecipe) {
            ingredients.add(imbuementRecipe.input);
            ingredients.addAll(imbuementRecipe.pedestalItems);
        } else if (recipe instanceof EnchantingApparatusRecipe apparatusRecipe) {
            // Enchantment and armor-upgrade recipes use a dynamic center item;
            // only their fixed pedestal ingredients belong in this conversion.
            if (!(apparatusRecipe instanceof EnchantmentRecipe)
                    && !(apparatusRecipe instanceof ArmorUpgradeRecipe)
                    && apparatusRecipe.reagent != null
                    && !apparatusRecipe.reagent.isEmpty()) {
                ingredients.add(apparatusRecipe.reagent);
            }
            ingredients.addAll(apparatusRecipe.pedestalItems);
        } else if (recipe instanceof BuddingConversionRecipe buddingRecipe) {
            Item input = buddingRecipe.input().asItem();
            if (input != Items.AIR) {
                ingredients.add(Ingredient.of(input));
            }
        } else if (recipe instanceof ScryRitualRecipe scryRecipe) {
            // The highlighted blocks are world targets, while the augment tag is
            // the actual item supplied by the player.
            ingredients.add(Ingredient.of(scryRecipe.augment()));
        }
        return ingredients;
    }

    @Nullable
    private static Recipe<?> findRecipe(RecipeManager recipeManager, String path, ResourceLocation recipeId) {
        return switch (path) {
            case "glyph_recipe" -> findRecipe(recipeManager, RecipeRegistry.GLYPH_TYPE.get(), recipeId);
            case "crush" -> findRecipe(recipeManager, RecipeRegistry.CRUSH_TYPE.get(), recipeId);
            case "imbuement" -> findRecipe(recipeManager, RecipeRegistry.IMBUEMENT_TYPE.get(), recipeId);
            case "enchanting_apparatus" -> findRecipe(recipeManager,
                    RecipeRegistry.APPARATUS_TYPE.get(), recipeId);
            case "enchantment_apparatus" -> findRecipe(recipeManager,
                    RecipeRegistry.ENCHANTMENT_TYPE.get(), recipeId);
            case "armor_upgrade" -> findRecipe(recipeManager,
                    RecipeRegistry.ARMOR_UPGRADE_TYPE.get(), recipeId);
            case "budding_conversion" -> findRecipe(recipeManager,
                    RecipeRegistry.BUDDING_CONVERSION_TYPE.get(), recipeId);
            case "scry_ritual" -> findRecipe(recipeManager,
                    RecipeRegistry.SCRY_RITUAL_TYPE.get(), recipeId);
            default -> null;
        };
    }

    @Nullable
    private static Recipe<?> findRecipe(RecipeManager recipeManager, RecipeType<?> recipeType,
                                        ResourceLocation recipeId) {
        return recipeManager.getRecipes().stream()
                .filter(recipe -> recipe.getType().equals(recipeType)
                        && recipe.getId().equals(recipeId))
                .findFirst()
                .orElse(null);
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
