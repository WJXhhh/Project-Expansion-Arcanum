package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.gui.GUIStoneTable;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class StoneTableJeiHandler implements IGuiContainerHandler<GUIStoneTable> {
    @Override
    public List<Rect2i> getGuiExtraAreas(GUIStoneTable screen) {
        return List.of();
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(GUIStoneTable screen, double mouseX, double mouseY) {
        ItemStack stack = screen.targetAtScreen(mouseX, mouseY);
        Rect2i area = screen.outputAreaAtScreen(mouseX, mouseY);
        if (stack.isEmpty() || area == null) {
            return Optional.empty();
        }

        return Optional.of(new ClickableIngredient(stack.copy(), area));
    }

    @Override
    public Collection<IGuiClickableArea> getGuiClickableAreas(GUIStoneTable screen, double mouseX, double mouseY) {
        return List.of();
    }

    private record ClickableIngredient(ItemStack stack, Rect2i area) implements IClickableIngredient<ItemStack> {
        @Override
        public ITypedIngredient<ItemStack> getTypedIngredient() {
            return new TypedIngredient(stack);
        }

        @Override
        public Rect2i getArea() {
            return area;
        }
    }

    private record TypedIngredient(ItemStack stack) implements ITypedIngredient<ItemStack> {
        @Override
        public IIngredientType<ItemStack> getType() {
            return VanillaTypes.ITEM_STACK;
        }

        @Override
        public ItemStack getIngredient() {
            return stack;
        }
    }
}
