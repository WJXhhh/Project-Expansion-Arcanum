package com.wjx.forge.projectexa.gui.container;

import com.wjx.forge.projectexa.mixin.TransmutationInventoryAccessor;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.utils.EMCHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Keeps the expensive knowledge work separate from page and EMC updates. */
public final class ArcaneTransmutationInventory extends TransmutationInventory {
    private static final int MATERIAL_SLOTS = 12;
    private static final int FUEL_SLOTS = 4;

    private boolean initialized;
    private boolean knowledgeDirty = true;
    private List<KnownItem> sortedKnowledge = List.of();
    private List<KnownItem> filteredKnowledge = List.of();
    private int[] materialIndices = new int[0];
    private int[] fuelIndices = new int[0];
    private String cachedFilter;
    private ItemInfo cachedLock;
    private int remainingKnowledge;

    public ArcaneTransmutationInventory(Player player) {
        super(player);
        // The superclass calls updateClientTargets before our fields are initialized.
        initialized = true;
        updateClientTargets();
    }

    public void invalidateKnowledge() {
        knowledgeDirty = true;
        updateClientTargets();
    }

    @Override
    public void itemLearned() {
        knowledgeDirty = true;
        super.itemLearned();
    }

    @Override
    public void itemUnlearned() {
        knowledgeDirty = true;
        super.itemUnlearned();
    }

    @Override
    public int getKnowledgeSize() {
        return remainingKnowledge;
    }

    @Override
    public void updateClientTargets() {
        if (!initialized || isServer()) return;

        if (knowledgeDirty) {
            Set<Item> fuels = new HashSet<>(FuelMapper.getFuelMap());
            List<KnownItem> knowledge = new ArrayList<>();
            for (ItemInfo info : provider.getKnowledge()) {
                long value = EMCHelper.getEmcValue(info);
                if (value > 0) knowledge.add(new KnownItem(info, value, fuels.contains(info.getItem())));
            }
            // Cache the value once per item, rather than looking it up in every comparison.
            knowledge.sort(Comparator.comparingLong(KnownItem::value).reversed());
            sortedKnowledge = knowledge;
            knowledgeDirty = false;
            cachedFilter = null;
        }

        ItemStack lockStack = getStackInSlot(8);
        ItemInfo lock = lockStack.isEmpty() ? null : NBTManager.getPersistentInfo(ItemInfo.fromStack(lockStack));
        if (!Objects.equals(cachedFilter, filter) || !Objects.equals(cachedLock, lock)) {
            List<KnownItem> matches = new ArrayList<>();
            for (KnownItem item : sortedKnowledge) {
                if (!item.info().equals(lock) && (filter.isEmpty()
                        || ((TransmutationInventoryAccessor) (Object) this).projectexa$matchesFilter(item.info()))) {
                    matches.add(item);
                }
            }
            // Append matches instead of removing from an ArrayList while iterating it.
            filteredKnowledge = matches;
            int[] materials = new int[matches.size()];
            int[] fuels = new int[matches.size()];
            int materialCount = 0;
            int fuelCount = 0;
            for (int index = 0; index < matches.size(); index++) {
                if (matches.get(index).fuel()) fuels[fuelCount++] = index;
                else materials[materialCount++] = index;
            }
            materialIndices = Arrays.copyOf(materials, materialCount);
            fuelIndices = Arrays.copyOf(fuels, fuelCount);
            cachedFilter = filter;
            cachedLock = lock;
        }

        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            outputs.setStackInSlot(slot, ItemStack.EMPTY);
        }
        remainingKnowledge = 0;

        BigInteger available = getAvailableEmc();
        long limit = available.signum() <= 0 ? 0
                : available.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0 ? Long.MAX_VALUE : available.longValue();
        if (lock != null) {
            long lockValue = EMCHelper.getEmcValue(lock);
            if (available.compareTo(BigInteger.valueOf(lockValue)) < 0) return;
            limit = Math.min(limit, lockValue);
        }

        // The list is sorted by descending EMC. Affordability only changes its start index.
        int low = 0;
        int high = filteredKnowledge.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (filteredKnowledge.get(middle).value() > limit) low = middle + 1;
            else high = middle;
        }
        int affordableCount = filteredKnowledge.size() - low;
        // Retain ProjectE's shared 12-item page offset for both output categories.
        int offset = (int) Math.min((long) Math.max(0, searchpage) * MATERIAL_SLOTS, affordableCount);
        int start = low + offset;
        remainingKnowledge = filteredKnowledge.size() - start;

        int materialSlot = 0;
        int fuelSlot = 0;
        if (lock != null && provider.hasKnowledge(lock)) {
            ItemStack stack = lock.createStack();
            if (FuelMapper.isStackFuel(stack)) outputs.setStackInSlot(MATERIAL_SLOTS + fuelSlot++, stack);
            else outputs.setStackInSlot(materialSlot++, stack);
        }
        fillOutputs(materialIndices, start, materialSlot, MATERIAL_SLOTS);
        fillOutputs(fuelIndices, start, MATERIAL_SLOTS + fuelSlot, MATERIAL_SLOTS + FUEL_SLOTS);
    }

    private void fillOutputs(int[] indices, int start, int slot, int endSlot) {
        int index = Arrays.binarySearch(indices, start);
        if (index < 0) index = -index - 1;
        // Separate category indices avoid scanning all knowledge to find a rare fuel.
        while (index < indices.length && slot < endSlot) {
            KnownItem item = filteredKnowledge.get(indices[index++]);
            outputs.setStackInSlot(slot++, item.info().createStack());
        }
    }

    private record KnownItem(ItemInfo info, long value, boolean fuel) {
    }
}
