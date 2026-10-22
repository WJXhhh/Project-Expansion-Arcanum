package com.wjx.neoforge.projectexa.integrations.jei;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketRecipeMaterialTransmutation;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import com.wjx.neoforge.projectexa.util.Util;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Captures the actual JEI category's type instead of pretending that all three mods use one raw recipe type. */
public final class ModRecipeMaterialTransferHandler implements IRecipeTransferHandler<AbstractContainerMenu, Object> {
    private final RecipeType<Object> type;
    private static final IRecipeTransferError HIDDEN = () -> IRecipeTransferError.Type.INTERNAL;
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ModRecipeMaterialTransferHandler(RecipeType<?> type) { this.type = (RecipeType) type; }
    @Override public Class<? extends AbstractContainerMenu> getContainerClass() { return AbstractContainerMenu.class; }
    @Override public Optional<MenuType<AbstractContainerMenu>> getMenuType() { return Optional.empty(); }
    @Override public RecipeType<Object> getRecipeType() { return type; }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu menu, Object recipe, IRecipeSlotsView slots,
                                                        Player player, boolean transferAll, boolean doTransfer) {
        if (!ArcaneTabletAccess.hasTablet(player)) return HIDDEN;
        ResourceLocation category = type.getUid();
        List<IRecipeSlotView> materials = new ArrayList<>(slots.getSlotViews(RecipeIngredientRole.INPUT));
        if ("ars_nouveau".equals(category.getNamespace())
                && ("enchantment".equals(category.getPath()) || "armor_upgrade".equals(category.getPath()))
                && !materials.isEmpty()) materials.removeFirst();
        ItemStack contextItem = materials.isEmpty() ? ItemStack.EMPTY : representative(materials.getFirst());
        ResourceLocation id = recipeId(recipe);
        if ("goety".equals(category.getNamespace()) && "brewing".equals(category.getPath())) {
            var catalysts = slots.getSlotViews(RecipeIngredientRole.CATALYST);
            if (catalysts.isEmpty()) return HIDDEN;
            materials.add(catalysts.getLast());
            contextItem = representative(catalysts.getLast());
            id = Main.rl("builtin_brewing");
        } else if ("goety".equals(category.getNamespace()) && "cauldron".equals(category.getPath())) {
            addCauldronTakeWith(recipe, slots, materials);
        }
        if (id == null || materials.isEmpty()) return HIDDEN;
        if (doTransfer) {
            // One click supplies one recipe's missing items; transferAll never bypasses material accounting.
            PacketDistributor.sendToServer(new PacketRecipeMaterialTransmutation(category, id, contextItem.copyWithCount(1)));
            return null;
        }
        return new StatusRenderer(materials, classify(materials, player));
    }

    private static ItemStack representative(IRecipeSlotView slot) {
        return slot.getDisplayedItemStack().orElseGet(() -> slot.getItemStacks().findFirst().orElse(ItemStack.EMPTY));
    }

    private static void addCauldronTakeWith(Object recipe, IRecipeSlotsView slots, List<IRecipeSlotView> materials) {
        Object value = recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe;
        try {
            Object takeWith = value.getClass().getMethod("getTakeWith").invoke(value);
            if (takeWith instanceof net.minecraft.world.item.crafting.Ingredient ingredient && !ingredient.isEmpty()) {
                var matching = slots.getSlotViews(RecipeIngredientRole.CATALYST).stream()
                        .filter(slot -> slot.getItemStacks().anyMatch(ingredient)).toList();
                if (!matching.isEmpty()) materials.add(matching.getLast());
            }
        } catch (ReflectiveOperationException | LinkageError ignored) { }
    }

    private static @Nullable ResourceLocation recipeId(Object recipe) {
        if (recipe instanceof RecipeHolder<?> holder) return holder.id();
        // Goety 3.2.1 still exposes raw recipes with public getId(), unlike Ars/Create's RecipeHolder APIs.
        try {
            Object id = recipe.getClass().getMethod("getId").invoke(recipe);
            if (id instanceof ResourceLocation location) return location;
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            for (var holder : level.getRecipeManager().getRecipes()) if (holder.value() == recipe) return holder.id();
        }
        return null;
    }

    private static List<Status> classify(List<IRecipeSlotView> slots, Player player) {
        List<ItemStack> reserved = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) reserved.add(player.getInventory().getItem(i).copy());
        var provider = Util.getKnowledgeProvider(player);
        BigInteger budget = provider == null ? BigInteger.ZERO : provider.getEmc();
        if (provider != null) {
            var holders = provider.getInputAndLocks();
            for (int i = 0; i < holders.getSlots(); i++) {
                ItemStack stack = holders.getStackInSlot(i);
                var holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
                if (holder != null) budget = budget.add(BigInteger.valueOf(Math.max(0, holder.getStoredEmc(stack))));
            }
        }
        List<Status> result = new ArrayList<>();
        for (IRecipeSlotView slot : slots) {
            List<ItemStack> options = slot.getItemStacks().filter(stack -> !stack.isEmpty()).toList();
            if (options.isEmpty()) { result.add(Status.NONE); continue; }
            int missing = Math.max(1, representative(slot).getCount());
            for (ItemStack owned : reserved) {
                if (owned.isEmpty()) continue;
                boolean match = options.stream().anyMatch(option -> Util.areStacksEqual(player.registryAccess(), Util.cleanStack(option), Util.cleanStack(owned)));
                if (match) {
                    int used = Math.min(missing, owned.getCount()); owned.shrink(used); missing -= used;
                    if (missing == 0) break;
                }
            }
            if (missing == 0) { result.add(Status.PRESENT); continue; }
            long cheapest = Long.MAX_VALUE;
            if (provider != null) {
                for (ItemStack option : options) {
                    ItemStack clean = Util.cleanStack(option);
                    long value = clean.isEmpty() ? 0 : IEMCProxy.INSTANCE.getValue(clean);
                    if (value > 0 && provider.hasKnowledge(clean)) cheapest = Math.min(cheapest, value);
                }
            }
            BigInteger cost = BigInteger.valueOf(cheapest).multiply(BigInteger.valueOf(missing));
            if (cheapest != Long.MAX_VALUE && budget.compareTo(cost) >= 0) { result.add(Status.TRANSMUTABLE); budget = budget.subtract(cost); }
            else result.add(Status.UNAVAILABLE);
        }
        return result;
    }

    private enum Status {
        NONE(0), PRESENT(0x6600FF00), TRANSMUTABLE(0x66FFFF00), UNAVAILABLE(0x66FF0000);
        final int color; Status(int color) { this.color = color; }
    }
    private record StatusRenderer(List<IRecipeSlotView> slots, List<Status> statuses) implements IRecipeTransferError {
        @Override public Type getType() { return Type.COSMETIC; }
        @Override public int getButtonHighlightColor() { return 0x6600AADD; }
        @Override public void showError(GuiGraphics graphics, int mouseX, int mouseY, IRecipeSlotsView view, int recipeX, int recipeY) {
            graphics.pose().pushPose(); graphics.pose().translate(recipeX, recipeY, 0);
            for (int i = 0; i < slots.size(); i++) if (statuses.get(i) != Status.NONE) slots.get(i).drawHighlight(graphics, statuses.get(i).color);
            graphics.pose().popPose();
            graphics.renderTooltip(Minecraft.getInstance().font, Component.translatable("jei.projectexa.transfer.materials_only"), mouseX, mouseY);
        }
    }
}
