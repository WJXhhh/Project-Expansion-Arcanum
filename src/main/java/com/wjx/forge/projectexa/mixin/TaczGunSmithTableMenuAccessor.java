package com.wjx.forge.projectexa.mixin;

import com.tacz.guns.crafting.GunSmithTableRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

/** Exposes TACZ's server-side recipe and table/filter validation to the integration. */
@Pseudo
@Mixin(targets = "com.tacz.guns.inventory.GunSmithTableMenu", remap = false)
public interface TaczGunSmithTableMenuAccessor {
    @Invoker("getRecipe")
    @Nullable
    GunSmithTableRecipe projectexa$getRecipe(ResourceLocation recipeId, RecipeManager recipeManager);
}
