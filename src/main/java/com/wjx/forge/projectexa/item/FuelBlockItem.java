package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.util.Fuel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.util.Objects;

public class FuelBlockItem extends BlockItem {
    private final Fuel level;
    public FuelBlockItem(Fuel level) {
        super(Objects.requireNonNull(Objects.requireNonNull(level).getBlock()), new Properties().rarity(level.getRarity()));
        this.level = level;
    }


    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        // 9 single items combined
        return level.getBurnTime(recipeType) * 9;
    }
}
