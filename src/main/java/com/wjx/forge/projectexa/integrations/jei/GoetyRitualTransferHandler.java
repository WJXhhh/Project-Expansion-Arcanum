package com.wjx.forge.projectexa.integrations.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketGoetyRitualTransmutation;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.util.Util;
import com.Polarice3.Goety.common.crafting.RitualRecipe;
import com.Polarice3.Goety.compat.jei.JeiRecipeTypes;
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Adds the ProjectExA transmutation button to Goety's JEI ritual categories. */
public final class GoetyRitualTransferHandler
        implements IRecipeTransferHandler<AbstractContainerMenu, RitualRecipe> {
    public static final GoetyRitualTransferHandler INSTANCE = new GoetyRitualTransferHandler();

    private static final int GREEN_HIGHLIGHT = 0x6600FF00;
    private static final int YELLOW_HIGHLIGHT = 0x66FFFF00;
    private static final int RED_HIGHLIGHT = 0x66FF0000;

    private static final IRecipeTransferError HIDDEN = new IRecipeTransferError() {
        @Override
        public Type getType() {
            return Type.INTERNAL;
        }
    };

    private GoetyRitualTransferHandler() {
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
    public RecipeType<RitualRecipe> getRecipeType() {
        return JeiRecipeTypes.RITUAL;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(AbstractContainerMenu container, RitualRecipe recipe,
                                                          IRecipeSlotsView slots, Player player, boolean transferAll,
                                                          boolean doTransfer) {
        if (recipe == null || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return HIDDEN;
        }

        if (doTransfer) {
            PacketHandler.sendToServer(new PacketGoetyRitualTransmutation(recipe.getId()));
            return null;
        }

        return new StatusRenderer(slots, classifyInputs(slots, player));
    }

    private static List<Status> classifyInputs(IRecipeSlotsView slots, Player player) {
        List<Status> statuses = new ArrayList<>();
        List<ItemStack> inventory = copyInventory(player);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        BigInteger availableEmc = provider == null ? BigInteger.ZERO : getAvailableEmc(provider);

        for (IRecipeSlotView slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> candidates = slot.getItemStacks().filter(stack -> !stack.isEmpty()).toList();
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

    private record StatusRenderer(IRecipeSlotsView slots, List<Status> statuses) implements IRecipeTransferError {
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
            List<IRecipeSlotView> inputs = slots.getSlotViews(RecipeIngredientRole.INPUT);
            int count = Math.min(inputs.size(), statuses.size());
            for (int index = 0; index < count; index++) {
                Status status = statuses.get(index);
                if (status.highlight != 0) {
                    inputs.get(index).drawHighlight(graphics, status.highlight);
                }
            }
            pose.popPose();
        }
    }
}
