package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

/**
 * Virtual inventory backed by one player's ProjectE knowledge and personal EMC.
 * Filter slots contain no physical items: insertion adds EMC and extraction spends it.
 */
public class PersonalEmcWarehouseHandler implements IItemHandler {
    private final ItemStack warehouse;
    private final @Nullable LivingEntity accessingEntity;

    public PersonalEmcWarehouseHandler(ItemStack warehouse, @Nullable LivingEntity accessingEntity) {
        this.warehouse = warehouse;
        this.accessingEntity = accessingEntity;
    }

    @Override
    public int getSlots() {
        return ItemPersonalEmcWarehouse.FILTER_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ItemStack template = getAvailableTemplate(slot);
        if (template.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Access access = getAccess();
        if (access == null) {
            // Client consumers get an optimistic view for interaction prediction. The
            // server still fails closed if the bound account is offline.
            return isClientSide() ? template.copyWithCount(template.getMaxStackSize()) : ItemStack.EMPTY;
        }
        if (access.player().isCreative()) {
            return template.copyWithCount(Integer.MAX_VALUE);
        }

        long value = IEMCProxy.INSTANCE.getValue(template);
        if (value <= 0 || !access.provider().hasKnowledge(template)) {
            return ItemStack.EMPTY;
        }
        int affordable = Util.safeIntValue(access.provider().getEmc().divide(BigInteger.valueOf(value)));
        return affordable <= 0 ? ItemStack.EMPTY : template.copyWithCount(affordable);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        if (isClientSide()) {
            return ItemStack.EMPTY;
        }
        Access access = getAccess();
        if (access == null) {
            return stack;
        }

        long value = IEMCProxy.INSTANCE.getSellValue(stack);
        if (value <= 0) {
            return stack;
        }
        if (!simulate && !access.player().level().isClientSide) {
            BigInteger added = BigInteger.valueOf(value).multiply(BigInteger.valueOf(stack.getCount()));
            updateEmc(access, access.provider().getEmc().add(added));
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack template = getAvailableTemplate(slot);
        if (template.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isClientSide()) {
            return template.copyWithCount(amount);
        }
        Access access = getAccess();
        if (access == null) {
            return ItemStack.EMPTY;
        }
        if (access.player().isCreative()) {
            return template.copyWithCount(amount);
        }

        long value = IEMCProxy.INSTANCE.getValue(template);
        if (value <= 0 || !access.provider().hasKnowledge(template)) {
            return ItemStack.EMPTY;
        }
        BigInteger unitCost = BigInteger.valueOf(value);
        int extracted = Math.min(amount, Util.safeIntValue(access.provider().getEmc().divide(unitCost)));
        if (extracted <= 0) {
            return ItemStack.EMPTY;
        }
        if (!simulate && !access.player().level().isClientSide) {
            BigInteger cost = unitCost.multiply(BigInteger.valueOf(extracted));
            updateEmc(access, access.provider().getEmc().subtract(cost));
        }
        return template.copyWithCount(extracted);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        ItemStack filter = ItemPersonalEmcWarehouse.getFilter(warehouse, slot);
        return !filter.isEmpty() && ItemStack.isSameItemSameTags(filter, stack);
    }

    private ItemStack getAvailableTemplate(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return ItemStack.EMPTY;
        }
        return ItemPersonalEmcWarehouse.getFilter(warehouse, slot);
    }

    private @Nullable Access getAccess() {
        if (isClientSide()) {
            return null;
        }
        ServerPlayer player = ItemPersonalEmcWarehouse.getOwner(warehouse).map(Util::getPlayer).orElse(null);
        if (player == null) {
            return null;
        }
        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        return provider == null ? null : new Access(player, provider);
    }

    private boolean isClientSide() {
        return accessingEntity != null ? accessingEntity.level().isClientSide : EffectiveSide.get().isClient();
    }

    private static void updateEmc(Access access, BigInteger emc) {
        BigInteger safeEmc = emc.max(BigInteger.ZERO);
        access.provider().setEmc(safeEmc);
        access.provider().syncEmc(access.player());
        PlayerHelper.updateScore(access.player(), PlayerHelper.SCOREBOARD_EMC, safeEmc);
    }

    private record Access(ServerPlayer player, IKnowledgeProvider provider) {
    }
}
