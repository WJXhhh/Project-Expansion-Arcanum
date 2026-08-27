package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.gui.GUIStoneTable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;

@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {
    @Override
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

    private static <C extends AbstractContainerMenu> void registerCraftingHandler(
            IRecipeTransferRegistration registration, Class<? extends C> containerClass, MenuType<C> menuType) {
        registration.addRecipeTransferHandler(
                new ArcaneCraftingRecipeTransferHandler<>(containerClass, menuType),
                RecipeTypes.CRAFTING
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(GUIStoneTable.class, new StoneTableJeiHandler());
    }
}
