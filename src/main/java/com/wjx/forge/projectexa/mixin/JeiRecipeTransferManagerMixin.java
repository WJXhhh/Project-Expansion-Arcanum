package com.wjx.forge.projectexa.mixin;

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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Adds the tablet transfer handler when JEI has no container-specific one. */
@Pseudo
@Mixin(targets = "mezz.jei.library.recipes.RecipeTransferManager", remap = false)
public class JeiRecipeTransferManagerMixin {
    @Inject(method = "getRecipeTransferHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void projectexa$addUniversalCraftingTransferHandler(
            AbstractContainerMenu container, IRecipeCategory<?> recipeCategory,
            CallbackInfoReturnable<Optional<?>> callback) {
        if (callback.getReturnValue().isPresent()) {
            return;
        }

        if (recipeCategory.getRecipeType() == RecipeTypes.CRAFTING) {
            callback.setReturnValue(Optional.of(UniversalArcaneCraftingRecipeTransferHandler.INSTANCE));
            return;
        }

        ResourceLocation recipeType = recipeCategory.getRecipeType().getUid();
        if (ModList.get().isLoaded("goety") && GoetyJeiRecipeTypes.isRitual(recipeType)) {
            callback.setReturnValue(Optional.of(GoetyRitualTransferHandler.INSTANCE));
        } else if (ModList.get().isLoaded("goety") && GoetyJeiRecipeTypes.isStaticRecipeType(recipeType)) {
            callback.setReturnValue(Optional.of(GoetyRecipeTransferHandler.INSTANCE));
        } else if (ModList.get().isLoaded("ars_nouveau") && ArsNouveauJeiRecipeTypes.isSupported(recipeType)) {
            callback.setReturnValue(Optional.of(ArsNouveauRecipeTransferHandler.INSTANCE));
        } else if (ModList.get().isLoaded("botania") && BotaniaJeiRecipeTypes.isSupported(recipeType)) {
            callback.setReturnValue(Optional.of(BotaniaRecipeTransferHandler.INSTANCE));
        } else if (ModList.get().isLoaded("create") && CreateJeiRecipeTypes.isSupported(recipeType)) {
            callback.setReturnValue(Optional.of(CreateRecipeTransferHandler.INSTANCE));
        }
    }
}
