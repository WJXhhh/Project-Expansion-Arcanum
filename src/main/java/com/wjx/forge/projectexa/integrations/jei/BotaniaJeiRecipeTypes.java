package com.wjx.forge.projectexa.integrations.jei;

import vazkii.botania.api.recipe.BotanicalBreweryRecipe;
import vazkii.botania.api.recipe.ElvenTradeRecipe;
import vazkii.botania.api.recipe.ManaInfusionRecipe;
import vazkii.botania.api.recipe.OrechidRecipe;
import vazkii.botania.api.recipe.PetalApothecaryRecipe;
import vazkii.botania.api.recipe.PureDaisyRecipe;
import vazkii.botania.api.recipe.RunicAltarRecipe;
import vazkii.botania.api.recipe.TerrestrialAgglomerationRecipe;
import vazkii.botania.common.crafting.MarimorphosisRecipe;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/** Resource-location checks for the recipe categories exposed by Botania's JEI plugin. */
public final class BotaniaJeiRecipeTypes {
    public static final String NAMESPACE = "botania";

    public static final ResourceLocation BREWERY = new ResourceLocation(NAMESPACE, "brewery");
    public static final ResourceLocation ELVEN_TRADE = new ResourceLocation(NAMESPACE, "elven_trade");
    public static final ResourceLocation MANA_POOL = new ResourceLocation(NAMESPACE, "mana_pool");
    public static final ResourceLocation PETALS = new ResourceLocation(NAMESPACE, "petals");
    public static final ResourceLocation PURE_DAISY = new ResourceLocation(NAMESPACE, "pure_daisy");
    public static final ResourceLocation RUNIC_ALTAR = new ResourceLocation(NAMESPACE, "runic_altar");
    public static final ResourceLocation TERRA_PLATE = new ResourceLocation(NAMESPACE, "terra_plate");
    public static final ResourceLocation ORECHID = new ResourceLocation(NAMESPACE, "orechid");
    public static final ResourceLocation ORECHID_IGNEM = new ResourceLocation(NAMESPACE, "orechid_ignem");
    public static final ResourceLocation MARIMORPHOSIS = new ResourceLocation(NAMESPACE, "marimorphosis");

    private BotaniaJeiRecipeTypes() {
    }

    public static boolean isSupported(ResourceLocation recipeType) {
        return recipeType != null
                && NAMESPACE.equals(recipeType.getNamespace())
                && switch (recipeType.getPath()) {
            case "brewery", "elven_trade", "mana_pool", "petals", "pure_daisy", "runic_altar",
                 "terra_plate", "orechid", "orechid_ignem", "marimorphosis" -> true;
            default -> false;
        };
    }

    /** Returns the JEI category id used for a recipe object, or {@code null} if unsupported. */
    @Nullable
    public static ResourceLocation getTypeForRecipe(Object recipe) {
        if (recipe instanceof BotanicalBreweryRecipe) {
            return BREWERY;
        }
        if (recipe instanceof ElvenTradeRecipe) {
            return ELVEN_TRADE;
        }
        if (recipe instanceof ManaInfusionRecipe) {
            return MANA_POOL;
        }
        if (recipe instanceof PetalApothecaryRecipe) {
            return PETALS;
        }
        if (recipe instanceof PureDaisyRecipe) {
            return PURE_DAISY;
        }
        if (recipe instanceof RunicAltarRecipe) {
            return RUNIC_ALTAR;
        }
        if (recipe instanceof TerrestrialAgglomerationRecipe) {
            return TERRA_PLATE;
        }
        if (recipe instanceof MarimorphosisRecipe) {
            return MARIMORPHOSIS;
        }
        if (recipe instanceof vazkii.botania.common.crafting.OrechidIgnemRecipe) {
            return ORECHID_IGNEM;
        }
        if (recipe instanceof OrechidRecipe) {
            return ORECHID;
        }
        return null;
    }
}
