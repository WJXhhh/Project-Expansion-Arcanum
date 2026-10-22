package com.wjx.neoforge.projectexa.integrations.jei;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.gui.GUIStoneTable;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.world.inventory.*;
import com.wjx.neoforge.projectexa.util.SearchSync;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    public static IJeiRuntime RUNTIME;

    public ResourceLocation getPluginUid() {
        return Main.rl("jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ArcaneCraftingTransferHandler(), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new PlayerCraftingTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
        registerCraftingHandler(registration, FurnaceMenu.class, MenuType.FURNACE);
        registerCraftingHandler(registration, BlastFurnaceMenu.class, MenuType.BLAST_FURNACE);
        registerCraftingHandler(registration, SmokerMenu.class, MenuType.SMOKER);
        registerCraftingHandler(registration, StonecutterMenu.class, MenuType.STONECUTTER);
        registerCraftingHandler(registration, AnvilMenu.class, MenuType.ANVIL);
        registerCraftingHandler(registration, SmithingMenu.class, MenuType.SMITHING);
        registerCraftingHandler(registration, BrewingStandMenu.class, MenuType.BREWING_STAND);
    }

    private static <C extends AbstractContainerMenu> void registerCraftingHandler(IRecipeTransferRegistration registration, Class<? extends C> type, MenuType<C> menuType) {
        registration.addRecipeTransferHandler(new ArcaneCraftingRecipeTransferHandler<>(type, menuType), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GUIStoneTable.class, new StoneTableJeiHandler());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        RUNTIME = jeiRuntime;
        SearchSync.register(new SearchSync("jei", text -> {
            if (RUNTIME != null) RUNTIME.getIngredientFilter().setFilterText(text);
        }));
    }

    @Override
    public void onRuntimeUnavailable() {
        RUNTIME = null;
    }
}
