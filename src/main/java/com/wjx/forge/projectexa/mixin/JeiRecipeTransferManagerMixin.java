package com.wjx.forge.projectexa.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wjx.forge.projectexa.integrations.jei.UniversalArcaneCraftingRecipeTransferHandler;
import com.wjx.forge.projectexa.integrations.jei.ArsNouveauJeiRecipeTypes;
import com.wjx.forge.projectexa.integrations.jei.ArsNouveauRecipeTransferHandler;
import com.wjx.forge.projectexa.integrations.jei.BotaniaJeiRecipeTypes;
import com.wjx.forge.projectexa.integrations.jei.BotaniaRecipeTransferHandler;
import com.wjx.forge.projectexa.integrations.jei.CreateJeiRecipeTypes;
import com.wjx.forge.projectexa.integrations.jei.CreateRecipeTransferHandler;
import com.wjx.forge.projectexa.integrations.jei.GoetyJeiRecipeTypes;
import com.wjx.forge.projectexa.integrations.jei.GoetyRecipeTransferHandler;
import com.wjx.forge.projectexa.integrations.jei.GoetyRitualTransferHandler;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.Optional;

/** Adds the tablet transfer handler when JEI has no container-specific one. */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public class JeiRecipeTransferManagerMixin {
    // Keep the entry arguments outside JEI's method body. Optimized JEI jars
    // reuse their parameter slots before RETURN, so a return injector cannot
    // safely capture container or recipeCategory there. No thread-local state
    // is needed, including for nested calls or exceptions.
    @WrapMethod(method = "getRecipeTransferHandler("
            + "Lnet/minecraft/world/inventory/AbstractContainerMenu;"
            + "Lmezz/jei/api/recipe/category/IRecipeCategory;)Ljava/util/Optional;", remap = false)
    private Optional<?> projectexa$addUniversalCraftingTransferHandler(
            AbstractContainerMenu container, IRecipeCategory<?> recipeCategory,
            Operation<Optional<?>> original) {
        Optional<?> handler = original.call(container, recipeCategory);
        if (handler.isPresent()) {
            return handler;
        }

        if (recipeCategory.getRecipeType() == RecipeTypes.CRAFTING) {
            return Optional.of(UniversalArcaneCraftingRecipeTransferHandler.INSTANCE);
        }

        ResourceLocation recipeType = recipeCategory.getRecipeType().getUid();
        if (ModList.get().isLoaded("goety") && GoetyJeiRecipeTypes.isRitual(recipeType)) {
            return Optional.of(GoetyRitualTransferHandler.INSTANCE);
        } else if (ModList.get().isLoaded("goety") && GoetyJeiRecipeTypes.isStaticRecipeType(recipeType)) {
            return Optional.of(GoetyRecipeTransferHandler.INSTANCE);
        } else if (ModList.get().isLoaded("ars_nouveau") && ArsNouveauJeiRecipeTypes.isSupported(recipeType)) {
            return Optional.of(ArsNouveauRecipeTransferHandler.INSTANCE);
        } else if (ModList.get().isLoaded("botania") && BotaniaJeiRecipeTypes.isSupported(recipeType)) {
            return Optional.of(BotaniaRecipeTransferHandler.INSTANCE);
        } else if (ModList.get().isLoaded("create") && CreateJeiRecipeTypes.isSupported(recipeType)) {
            return Optional.of(CreateRecipeTransferHandler.INSTANCE);
        }
        return handler;
    }
}
