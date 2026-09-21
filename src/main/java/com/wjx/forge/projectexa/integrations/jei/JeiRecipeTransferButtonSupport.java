package com.wjx.forge.projectexa.integrations.jei;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

/** Shared recipe-category check used by the version-specific JEI button mixins. */
public final class JeiRecipeTransferButtonSupport {
    private JeiRecipeTransferButtonSupport() {
    }

    public static boolean isSupportedRecipe(IRecipeLayoutDrawable<?> recipeLayout) {
        ResourceLocation uid = recipeLayout.getRecipeCategory().getRecipeType().getUid();
        return GoetyJeiRecipeTypes.isSupported(uid)
                || (ModList.get().isLoaded("ars_nouveau") && ArsNouveauJeiRecipeTypes.isSupported(uid))
                || (ModList.get().isLoaded("botania") && BotaniaJeiRecipeTypes.isSupported(uid))
                || (ModList.get().isLoaded("create") && CreateJeiRecipeTypes.isSupported(uid));
    }
}
