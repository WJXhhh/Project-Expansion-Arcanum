package com.wjx.neoforge.projectexa.integrations.create;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.wjx.neoforge.projectexa.integrations.RecipeMaterialTransmutation;
import com.wjx.neoforge.projectexa.integrations.jei.CreateJeiRecipeTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;

/** Uses authoritative recipes, including every item-consuming step of sequenced assembly. */
public final class CreateRecipeIntegration {
    private CreateRecipeIntegration() {}

    public static List<Ingredient> resolve(ServerPlayer player, RecipeHolder<?> holder, ResourceLocation category,
                                           ResourceLocation id, ItemStack context) {
        if (holder != null) return ingredients(holder.value(), category);
        // Create's potion mixing recipes are synthesized outside RecipeManager. Rebuild using its COMMON API.
        if ("automatic_brewing".equals(category.getPath())) {
            for (var generated : com.simibubi.create.content.fluids.potion.PotionMixingRecipes.createRecipes(player.level())) {
                if (generated.id().equals(id)) return new ArrayList<>(generated.value().getIngredients());
            }
            return List.of();
        }
        if ("deploying".equals(category.getPath()) && id.getPath().endsWith("_using_deployer")) {
            ResourceLocation original = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(0, id.getPath().length() - "_using_deployer".length()));
            var source = player.server.getRecipeManager().byKey(original).orElse(null);
            if (source != null) {
                var result = ingredients(source.value(), category);
                if (ResourceLocation.fromNamespaceAndPath("create", "sandpaper_polishing").equals(BuiltInRegistries.RECIPE_TYPE.getKey(source.value().getType()))) {
                    result.add(Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("create", "sandpaper"))));
                }
                return result;
            }
        }
        // This JEI-only recipe's ID is based on the stripped output, not on an entry in RecipeManager.
        if ("item_application".equals(category.getPath()) && context.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            var stripped = net.minecraft.world.item.AxeItem.getAxeStrippingState(blockItem.getBlock().defaultBlockState());
            if (stripped != null) {
                ResourceLocation output = BuiltInRegistries.ITEM.getKey(stripped.getBlock().asItem());
                ResourceLocation expected = ResourceLocation.fromNamespaceAndPath(output.getNamespace(), output.getPath() + "_via_vanilla_stripping");
                if (expected.equals(id)) return List.of(Ingredient.of(context.getItem()), Ingredient.of(Items.IRON_AXE));
            }
        }
        if (!"create".equals(id.getNamespace())) return List.of();
        // Fixed mystery conversions are a JEI-only table. These are their verified two source items.
        ResourceLocation itemId = context.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(context.getItem());
        if ("mystery_conversion".equals(category.getPath()) && id.getPath().startsWith("conversion_") && itemId != null
                && (itemId.equals(ResourceLocation.fromNamespaceAndPath("create", "empty_blaze_burner"))
                || itemId.equals(ResourceLocation.fromNamespaceAndPath("create", "peculiar_bell")))) return List.of(Ingredient.of(context.getItem()));
        // Context items are rechecked against common fluid capabilities. No JEI/client class is loaded on a server.
        if ("spout_filling".equals(category.getPath())) {
            if ("potions".equals(id.getPath())) return List.of(Ingredient.of(Items.GLASS_BOTTLE));
            if (!context.isEmpty() && com.simibubi.create.content.fluids.transfer.GenericItemFilling.canItemBeFilled(player.level(), context.copy())
                    && id.getPath().startsWith("fill_" + itemId.getNamespace() + "_" + itemId.getPath() + "_with_")) return List.of(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true, context));
        }
        if ("draining".equals(category.getPath()) && !context.isEmpty()
                && com.simibubi.create.content.fluids.transfer.GenericItemEmptying.canItemBeEmptied(player.level(), context.copy())
                && ("potions".equals(id.getPath()) || id.getPath().startsWith("empty_" + itemId.getNamespace() + "_" + itemId.getPath() + "_of_"))) return List.of(net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true, context));
        return List.of();
    }

    public static List<Ingredient> ingredients(Recipe<?> recipe, ResourceLocation category) {
        if (!CreateJeiRecipeTypes.isSupported(category)
                || !CreateJeiRecipeTypes.isSupportedRecipeType(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) return List.of();
        if (!(recipe instanceof SequencedAssemblyRecipe assembly)) return new ArrayList<>(recipe.getIngredients());
        int loops = assembly.getLoops();
        if (loops <= 0 || loops > RecipeMaterialTransmutation.MAX_MATERIALS) return List.of();
        List<Ingredient> result = new ArrayList<>();
        result.add(assembly.getIngredient());
        for (int loop = 0; loop < loops; loop++) {
            for (var step : assembly.getSequence()) {
                step.getAsAssemblyRecipe().addAssemblyIngredients(result);
                if (result.size() > RecipeMaterialTransmutation.MAX_MATERIALS) return List.of();
            }
        }
        // Fluids, heat, time and deployment tools remain the real machine's responsibility.
        return result;
    }
}
