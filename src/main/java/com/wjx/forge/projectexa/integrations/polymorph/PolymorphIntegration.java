package com.wjx.forge.projectexa.integrations.polymorph;

import com.illusivesoulworks.polymorph.common.crafting.RecipeSelection;
import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class PolymorphIntegration {
    private PolymorphIntegration() {}

    public static Optional<CraftingRecipe> getRecipe(ContainerArcaneTransmutationTablet menu, Level level,
                                                      Player player, CraftingContainer crafting) {
        return RecipeSelection.getPlayerRecipe(menu, RecipeType.CRAFTING, crafting, level, player);
    }
}
