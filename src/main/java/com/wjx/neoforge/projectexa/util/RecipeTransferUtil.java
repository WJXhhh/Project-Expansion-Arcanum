package com.wjx.neoforge.projectexa.util;

import com.wjx.neoforge.projectexa.config.Config;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RecipeTransferUtil {
    public static final int MAX_CANDIDATES_PER_SLOT = 256;

    private RecipeTransferUtil() {}

    public static boolean serverAllowsLargeCandidates() {
        return Config.server.Spec.isLoaded() && Config.server.allowLargeRecipeTransferCandidates.get();
    }

    public static StreamCodec<RegistryFriendlyByteBuf, List<List<ItemStack>>> candidateListCodec(int maxSlots) {
        var bounded = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CANDIDATES_PER_SLOT))
                .apply(ByteBufCodecs.list(maxSlots));
        var unrestricted = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list(maxSlots));
        // Client handlers size their lists using the synced setting. Decode against the server's own setting.
        return StreamCodec.of(unrestricted::encode,
                buffer -> (serverAllowsLargeCandidates() ? unrestricted : bounded).decode(buffer));
    }

    public static List<ItemStack> filterAvailableCandidates(List<ItemStack> candidates,
            ContainerArcaneTransmutationTablet container, boolean allowLargeCandidates) {
        if (candidates.isEmpty()) return List.of(ItemStack.EMPTY);
        if (allowLargeCandidates || candidates.size() <= MAX_CANDIDATES_PER_SLOT) return candidates;

        var owned = ItemStackLinkedSet.createTypeAndComponentsSet();
        var inventory = container.getPlayer().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) owned.add(Util.cleanStack(stack));
        }
        // Clearing the grid can return ingredients to inventory or convert them into EMC.
        for (ItemStack stack : container.getCraftingInputs()) {
            if (!stack.isEmpty()) owned.add(Util.cleanStack(stack));
        }

        var seen = ItemStackLinkedSet.createTypeAndComponentsSet();
        List<ItemStack> options = new ArrayList<>(MAX_CANDIDATES_PER_SLOT);
        List<EmcCandidate> learned = new ArrayList<>();
        var provider = container.getProvider();
        for (ItemStack candidate : candidates) {
            ItemStack cleaned = Util.cleanStack(candidate);
            if (cleaned.isEmpty() || !seen.add(cleaned)) continue;
            if (owned.contains(cleaned)) {
                options.add(cleaned);
            } else {
                long value = learnedPrice(cleaned, provider);
                if (value > 0) learned.add(new EmcCandidate(cleaned, value));
            }
        }
        // The server checks affordability after clearing the grid and consuming other inputs.
        learned.sort(Comparator.comparingLong(EmcCandidate::value));
        learned.stream().limit(MAX_CANDIDATES_PER_SLOT - options.size())
                .map(EmcCandidate::stack).forEachOrdered(options::add);
        return options.isEmpty() ? candidates.subList(0, 16) : options;
    }

    public static List<ItemStack> prioritizeSmithingCandidates(List<ItemStack> candidates, ItemStack planned,
            List<ItemStack> inventory, Predicate<ItemStack> ingredient, @Nullable IKnowledgeProvider provider,
            boolean allowLargeCandidates) {
        var ownedItems = inventory.stream().filter(stack -> !stack.isEmpty() && ingredient.test(stack))
                .map(ItemStack::getItem).collect(Collectors.toSet());
        var ordered = ItemStackLinkedSet.createTypeAndComponentsSet();
        if (!planned.isEmpty()) {
            ordered.add(planned);
            ownedItems.remove(planned.getItem());
        }

        List<EmcCandidate> learned = new ArrayList<>();
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack candidate : candidates) {
            if (candidate.isEmpty() || ordered.contains(candidate)) continue;
            if (ownedItems.remove(candidate.getItem())) {
                ordered.add(candidate);
                continue;
            }
            ItemStack cleaned = Util.cleanStack(candidate);
            long value = learnedPrice(cleaned, provider);
            if (value > 0 && ingredient.test(cleaned)) learned.add(new EmcCandidate(candidate, value));
            else remaining.add(candidate);
        }
        learned.sort(Comparator.comparingLong(EmcCandidate::value));
        learned.forEach(candidate -> ordered.add(candidate.stack()));
        ordered.addAll(remaining);
        return ordered.stream().limit(allowLargeCandidates ? Long.MAX_VALUE : MAX_CANDIDATES_PER_SLOT).toList();
    }

    private static long learnedPrice(ItemStack cleaned, @Nullable IKnowledgeProvider provider) {
        return provider != null && !cleaned.isEmpty() && provider.hasKnowledge(cleaned)
                ? IEMCProxy.INSTANCE.getValue(cleaned) : 0;
    }

    private record EmcCandidate(ItemStack stack, long value) {}
}
