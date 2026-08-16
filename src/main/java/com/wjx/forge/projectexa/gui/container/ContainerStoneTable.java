package com.wjx.forge.projectexa.gui.container;

import com.wjx.forge.projectexa.registries.Blocks;
import com.wjx.forge.projectexa.registries.MenuTypes;
import com.wjx.forge.projectexa.util.StoneTableHelper;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.math.BigInteger;

public class ContainerStoneTable extends ContainerBase {
    private static final int PLAYER_SLOT_COUNT = 36;

    private final Player player;
    private final IKnowledgeProvider provider;
    private final BlockPos pos;

    public ContainerStoneTable(int windowId, Inventory inventory, BlockPos pos) {
        super(MenuTypes.STONE_TABLE.get(), windowId, inventory);
        this.player = inventory.player;
        this.provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Player does not have ProjectE knowledge capability"));
        this.pos = pos;
        addPlayerInventory(8, 135);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).getBlock() == Blocks.STONE_TABLE.get()
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player.level().isClientSide || !stillValid(player) || slotIndex < 0 || slotIndex >= PLAYER_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        Slot slot = tryGetSlot(slotIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack raw = slot.getItem().copy();
        ItemStack clean = Util.cleanStack(raw);
        long value = IEMCProxy.INSTANCE.getValue(clean);
        if (value <= 0 || !StoneTableHelper.isItemValid(clean)) {
            return ItemStack.EMPTY;
        }

        Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, raw, clean);
        if (result == Util.AddKnowledgeResult.FAIL) {
            return ItemStack.EMPTY;
        }

        provider.setEmc(provider.getEmc().add(burnValue(clean, raw.getCount())));
        slot.set(ItemStack.EMPTY);
        slot.setChanged();
        syncAfterKnowledge(result, clean);
        syncEmc();
        return clean;
    }

    public void handleAction(Action action, ItemStack target, boolean shiftHeld) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer) || action == null || !stillValid(player)) {
            return;
        }

        switch (action) {
            case BURN -> burnItem(shiftHeld);
            case LEARN -> learnItem();
            case UNLEARN -> unlearnItem();
            case EXTRACT -> extractItem(target, shiftHeld);
        }
    }

    private void burnItem(boolean shiftHeld) {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }

        if (!StoneTableHelper.isItemValid(carried)) {
            return;
        }

        if (shiftHeld) {
            IItemEmcHolder holder = carried.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve().orElse(null);
            if (holder != null) {
                transferHolderEmc(carried, holder);
                return;
            }
        }

        ItemStack clean = Util.cleanStack(carried);
        long value = IEMCProxy.INSTANCE.getValue(clean);
        if (value <= 0 || !StoneTableHelper.isItemValid(clean)) {
            return;
        }

        Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, carried, clean);
        if (result == Util.AddKnowledgeResult.FAIL) {
            return;
        }

        provider.setEmc(provider.getEmc().add(burnValue(clean, carried.getCount())));
        setCarried(ItemStack.EMPTY);
        syncAfterKnowledge(result, clean);
        syncEmc();
    }

    private void transferHolderEmc(ItemStack carried, IItemEmcHolder holder) {
        long stored = Math.max(0L, holder.getStoredEmc(carried));
        if (stored > 0L) {
            long extracted = holder.extractEmc(carried, stored, IEmcStorage.EmcAction.EXECUTE);
            if (extracted > 0L) {
                provider.setEmc(provider.getEmc().add(BigInteger.valueOf(extracted)));
                syncEmc();
            }
            return;
        }

        long capacity = Math.max(0L, holder.getMaximumEmc(carried));
        long available = Util.safeLongValue(provider.getEmc());
        long amount = Math.min(capacity, available);
        if (amount <= 0L) {
            return;
        }

        long inserted = holder.insertEmc(carried, amount, IEmcStorage.EmcAction.EXECUTE);
        if (inserted > 0L) {
            provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(inserted)));
            syncEmc();
        }
    }

    private void learnItem() {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }

        ItemStack clean = Util.cleanStack(carried);
        if (IEMCProxy.INSTANCE.getValue(clean) <= 0 || !StoneTableHelper.isItemValid(clean)) {
            return;
        }

        Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, carried, clean);
        syncAfterKnowledge(result, clean);
    }

    private void unlearnItem() {
        ItemStack carried = getCarried();
        if (carried.isEmpty()) {
            return;
        }

        ItemStack clean = Util.cleanStack(carried);
        if (!StoneTableHelper.isItemValid(clean)) {
            return;
        }
        if (provider.removeKnowledge(clean) && player instanceof ServerPlayer serverPlayer) {
            provider.syncKnowledgeChange(serverPlayer, ItemInfo.fromStack(clean), false);
        }
    }

    private void extractItem(ItemStack requested, boolean pullStack) {
        if (requested == null || requested.isEmpty()) {
            return;
        }

        ItemStack output = Util.cleanStack(requested);
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0 || !StoneTableHelper.isItemValid(output) || !provider.hasKnowledge(output)) {
            return;
        }

        BigInteger unitCost = BigInteger.valueOf(value);
        if (pullStack) {
            extractStack(output, unitCost);
        } else {
            extractOne(output, unitCost);
        }
    }

    private void extractOne(ItemStack output, BigInteger unitCost) {
        if (provider.getEmc().compareTo(unitCost) < 0) {
            return;
        }

        ItemStack carried = getCarried();
        if (!carried.isEmpty() && (!Util.areStacksEqual(player.level().registryAccess(), carried, output)
                || carried.getCount() >= carried.getMaxStackSize())) {
            return;
        }

        if (carried.isEmpty()) {
            setCarried(output.copyWithCount(1));
        } else {
            carried.grow(1);
        }
        provider.setEmc(provider.getEmc().subtract(unitCost));
        syncEmc();
    }

    private void extractStack(ItemStack output, BigInteger unitCost) {
        BigInteger affordable = provider.getEmc().divide(unitCost);
        int amount = Math.min(output.getMaxStackSize(), Util.safeIntValue(affordable));
        if (amount <= 0) {
            return;
        }

        IItemHandler handler = player.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
        if (handler == null) {
            return;
        }

        ItemStack simulated = output.copyWithCount(amount);
        ItemStack simulatedRemainder = ItemHandlerHelper.insertItemStacked(handler, simulated, true);
        int insertable = amount - simulatedRemainder.getCount();
        if (insertable <= 0) {
            return;
        }

        ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, output.copyWithCount(insertable), false);
        int inserted = insertable - remainder.getCount();
        if (inserted <= 0) {
            return;
        }

        provider.setEmc(provider.getEmc().subtract(unitCost.multiply(BigInteger.valueOf(inserted))));
        if (!remainder.isEmpty()) {
            provider.setEmc(provider.getEmc().add(unitCost.multiply(BigInteger.valueOf(remainder.getCount()))));
            Util.returnToInventory(playerInv, player, remainder, true);
        }
        syncEmc();
    }

    private void syncAfterKnowledge(Util.AddKnowledgeResult result, ItemStack clean) {
        if (result == Util.AddKnowledgeResult.SUCCESS && player instanceof ServerPlayer serverPlayer) {
            provider.syncKnowledgeChange(serverPlayer, ItemInfo.fromStack(clean), true);
        }
    }

    private void syncEmc() {
        if (player instanceof ServerPlayer serverPlayer) {
            provider.syncEmc(serverPlayer);
        }
    }

    private static BigInteger burnValue(ItemStack clean, int count) {
        long sellValue = IEMCProxy.INSTANCE.getSellValue(clean);
        if (sellValue <= 0L || count <= 0) {
            return BigInteger.ZERO;
        }
        return BigInteger.valueOf(sellValue).multiply(BigInteger.valueOf(count));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }

        ItemStack carried = getCarried().copy();
        setCarried(ItemStack.EMPTY);
        if (!carried.isEmpty()) {
            if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
                player.drop(carried, false);
            } else {
                Util.returnToInventory(playerInv, player, carried, true);
            }
        }
    }

    public Player getPlayer() {
        return player;
    }

    public IKnowledgeProvider getProvider() {
        return provider;
    }

    public BlockPos getPos() {
        return pos;
    }

    public enum Action {
        BURN,
        LEARN,
        UNLEARN,
        EXTRACT
    }
}
