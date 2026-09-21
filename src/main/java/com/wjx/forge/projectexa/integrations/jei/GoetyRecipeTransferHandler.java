package com.wjx.forge.projectexa.integrations.jei;

import com.Polarice3.Goety.common.crafting.BrazierRecipe;
import com.Polarice3.Goety.common.crafting.CursedInfuserRecipes;
import com.Polarice3.Goety.common.crafting.PulverizeRecipe;
import com.Polarice3.Goety.common.crafting.SoulAbsorberRecipes;
import com.Polarice3.Goety.compat.jei.WitchBrewJeiRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.forge.projectexa.integrations.goety.GoetyRecipeTypeResolver;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketGoetyRecipeTransmutation;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Adds the ProjectExA transmutation button to Goety's other JEI categories. */
public final class GoetyRecipeTransferHandler
        implements IRecipeTransferHandler<AbstractContainerMenu, Object> {
    public static final GoetyRecipeTransferHandler INSTANCE = new GoetyRecipeTransferHandler();

    private static final ResourceLocation BREWING_TYPE = new ResourceLocation("goety", "brewing");

    private static final int GREEN_HIGHLIGHT = 0x6600FF00;
    private static final int YELLOW_HIGHLIGHT = 0x66FFFF00;
    private static final int RED_HIGHLIGHT = 0x66FF0000;

    private static final IRecipeTransferError HIDDEN = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.INTERNAL;
        }
    };

    private GoetyRecipeTransferHandler() {
    }

    @Override
    public Class<? extends AbstractContainerMenu> getContainerClass() {
        return AbstractContainerMenu.class;
    }

    @Override
    public Optional<MenuType<AbstractContainerMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<Object> getRecipeType() {
        // This handler is supplied directly by the transfer-manager mixin rather
        // than registered against one particular JEI recipe type.
        return RecipeType.create("goety", "cursed_infuser", Object.class);
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu container, Object recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        if (!isSupportedRecipe(recipe) || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return HIDDEN;
        }

        List<IRecipeSlotView> materialSlots = getMaterialSlots(recipe, slots, player);
        if (materialSlots.isEmpty()) {
            return HIDDEN;
        }

        if (doTransfer) {
            PacketGoetyRecipeTransmutation packet = createPacket(recipe);
            if (packet == null) {
                return HIDDEN;
            }
            PacketHandler.sendToServer(packet);
            return null;
        }

        return new StatusRenderer(materialSlots, classifyInputs(materialSlots, player));
    }

    private static boolean isSupportedRecipe(Object recipe) {
        return recipe instanceof CursedInfuserRecipes
                || recipe instanceof BrazierRecipe
                || isRecipeType(recipe, "cauldron")
                || recipe instanceof PulverizeRecipe
                || recipe instanceof SoulAbsorberRecipes
                || recipe instanceof WitchBrewJeiRecipe;
    }

    @Nullable
    private static PacketGoetyRecipeTransmutation createPacket(Object recipe) {
        if (recipe instanceof WitchBrewJeiRecipe brewRecipe) {
            return new PacketGoetyRecipeTransmutation(BREWING_TYPE, null, brewRecipe.getCatalyst());
        }
        if (!(recipe instanceof Recipe<?> minecraftRecipe)) {
            return null;
        }

        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(minecraftRecipe.getType());
        if (!GoetyJeiRecipeTypes.isStaticRecipeType(typeId)) {
            return null;
        }
        return new PacketGoetyRecipeTransmutation(typeId, minecraftRecipe.getId(), ItemStack.EMPTY);
    }

    private static List<IRecipeSlotView> getMaterialSlots(Object recipe, IRecipeSlotsView slots, Player player) {
        List<IRecipeSlotView> materialSlots = new ArrayList<>(slots.getSlotViews(RecipeIngredientRole.INPUT));

        if (isRecipeType(recipe, "cauldron") && recipe instanceof Recipe<?> minecraftRecipe) {
            Ingredient takeWith = GoetyRecipeTypeResolver.getCauldronTakeWith(minecraftRecipe);
            if (takeWith != null && !takeWith.isEmpty()) {
                findLastMatchingIngredient(slots.getSlotViews(RecipeIngredientRole.CATALYST), takeWith)
                        .ifPresent(materialSlots::add);
            }
        } else if (recipe instanceof WitchBrewJeiRecipe brewRecipe) {
            List<IRecipeSlotView> catalystSlots = slots.getSlotViews(RecipeIngredientRole.CATALYST);
            findLastMatchingStack(catalystSlots, brewRecipe.getCatalyst(), player)
                    .ifPresent(materialSlots::add);
        }

        return materialSlots;
    }

    private static boolean isRecipeType(Object recipe, String path) {
        return recipe instanceof Recipe<?> minecraftRecipe
                && GoetyRecipeTypeResolver.is(minecraftRecipe.getType(), path);
    }

    private static Optional<IRecipeSlotView> findLastMatchingIngredient(List<IRecipeSlotView> slots,
                                                                          Ingredient ingredient) {
        for (int index = slots.size() - 1; index >= 0; index--) {
            IRecipeSlotView slot = slots.get(index);
            if (slot.getItemStacks().anyMatch(stack -> !stack.isEmpty() && ingredient.test(stack))) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    private static Optional<IRecipeSlotView> findLastMatchingStack(List<IRecipeSlotView> slots,
                                                                     ItemStack target,
                                                                     Player player) {
        for (int index = slots.size() - 1; index >= 0; index--) {
            IRecipeSlotView slot = slots.get(index);
            if (slot.getItemStacks().anyMatch(stack ->
                    Util.areStacksEqual(player.level().registryAccess(), target, stack))) {
                return Optional.of(slot);
            }
        }
        return slots.isEmpty() ? Optional.empty() : Optional.of(slots.get(slots.size() - 1));
    }

    private static List<Status> classifyInputs(List<IRecipeSlotView> materialSlots, Player player) {
        List<Status> statuses = new ArrayList<>(materialSlots.size());
        List<ItemStack> inventory = copyInventory(player);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        BigInteger availableEmc = provider == null ? BigInteger.ZERO : getAvailableEmc(provider);

        for (IRecipeSlotView slot : materialSlots) {
            List<ItemStack> candidates = slot.getItemStacks()
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            if (candidates.isEmpty()) {
                statuses.add(Status.NONE);
                continue;
            }

            if (consumeInventoryMatch(candidates, inventory, player)) {
                statuses.add(Status.PRESENT);
                continue;
            }

            Candidate candidate = provider == null ? null : findCheapestTransmutable(candidates, provider);
            if (candidate != null && availableEmc.compareTo(candidate.cost()) >= 0) {
                statuses.add(Status.TRANSMUTABLE);
                availableEmc = availableEmc.subtract(candidate.cost());
            } else {
                statuses.add(Status.UNAVAILABLE);
            }
        }
        return statuses;
    }

    private static List<ItemStack> copyInventory(Player player) {
        List<ItemStack> inventory = new ArrayList<>(player.getInventory().getContainerSize());
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            inventory.add(player.getInventory().getItem(index).copy());
        }
        return inventory;
    }

    private static boolean consumeInventoryMatch(List<ItemStack> candidates, List<ItemStack> inventory, Player player) {
        for (ItemStack candidate : candidates) {
            ItemStack cleanCandidate = Util.cleanStack(candidate);
            if (cleanCandidate.isEmpty()) {
                continue;
            }

            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()
                        && Util.areStacksEqual(player.level().registryAccess(), cleanCandidate, Util.cleanStack(stack))) {
                    stack.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static Candidate findCheapestTransmutable(List<ItemStack> candidates, IKnowledgeProvider provider) {
        Candidate best = null;
        for (ItemStack option : candidates) {
            ItemStack clean = Util.cleanStack(option);
            if (clean.isEmpty() || !provider.hasKnowledge(clean)) {
                continue;
            }

            long value = IEMCProxy.INSTANCE.getValue(clean);
            if (value <= 0) {
                continue;
            }

            Candidate candidate = new Candidate(clean, BigInteger.valueOf(value));
            if (best == null || candidate.cost().compareTo(best.cost()) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static BigInteger getAvailableEmc(IKnowledgeProvider provider) {
        BigInteger available = provider.getEmc();
        IItemHandler inputLocks = provider.getInputAndLocks();
        for (int index = 0; index < inputLocks.getSlots(); index++) {
            if (index == 8) {
                continue;
            }

            ItemStack stack = inputLocks.getStackInSlot(index);
            if (stack.isEmpty()) {
                continue;
            }

            IItemEmcHolder holder = stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY)
                    .resolve().orElse(null);
            if (holder != null) {
                long stored = holder.getStoredEmc(stack);
                if (stored > 0) {
                    available = available.add(BigInteger.valueOf(stored));
                }
            }
        }
        return available;
    }

    private enum Status {
        NONE(0),
        PRESENT(GREEN_HIGHLIGHT),
        TRANSMUTABLE(YELLOW_HIGHLIGHT),
        UNAVAILABLE(RED_HIGHLIGHT);

        private final int highlight;

        Status(int highlight) {
            this.highlight = highlight;
        }
    }

    private record Candidate(ItemStack stack, BigInteger cost) {
    }

    private record StatusRenderer(List<IRecipeSlotView> slots, List<Status> statuses)
            implements IRecipeTransferError {
        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public int getButtonHighlightColor() {
            return 0;
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip) {
            tooltip.add(Component.translatable("jei.projectexa.goety.transmute"));
        }

        @Override
        public void showError(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
                              IRecipeSlotsView ignored, int recipeX, int recipeY) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(recipeX, recipeY, 0);
            int count = Math.min(slots.size(), statuses.size());
            for (int index = 0; index < count; index++) {
                Status status = statuses.get(index);
                if (status.highlight != 0) {
                    slots.get(index).drawHighlight(graphics, status.highlight);
                }
            }
            pose.popPose();
        }
    }
}
