package com.wjx.neoforge.projectexa.integrations.goety;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public final class GoetyRecipeTypeResolver {
    private GoetyRecipeTypeResolver() {}
    public static boolean is(RecipeType<?> type, String path) {
        return type != null && ResourceLocation.fromNamespaceAndPath("goety", path).equals(BuiltInRegistries.RECIPE_TYPE.getKey(type));
    }
}
