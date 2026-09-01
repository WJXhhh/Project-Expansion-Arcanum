package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.config.Config;
import com.wjx.forge.projectexa.util.Lang;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.capability.EmcHolderItemCapabilityWrapper;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.PETags;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

import javax.annotation.Nullable;

public class ItemFinalStar extends ItemPE implements IItemEmcHolder, IPedestalItem {
    private static final long REPORTED_EMC = 1_000_000_000_000_000L;

    public ItemFinalStar() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        addItemCapability(EmcHolderItemCapabilityWrapper::new);
        addItemCapability(PedestalItemCapabilityWrapper::new);
        addItemCapability("curios", IntegrationHelper.CURIO_CAP_SUPPLIER);
    }

    /**
     * The Final Star is an effectively inexhaustible EMC provider. This mirrors
     * ProjectEX 1.12: the table can extract EMC from it, but the item never
     * stores the extracted amount in its own NBT.
     */
    @Override
    public long insertEmc(ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
        if (toInsert < 0L) {
            return extractEmc(stack, -toInsert, action);
        }
        return 0L;
    }

    @Override
    public long extractEmc(ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
        if (toExtract < 0L) {
            return insertEmc(stack, -toExtract, action);
        }
        return toExtract;
    }

    @Override
    public long getStoredEmc(ItemStack stack) {
        return REPORTED_EMC;
    }

    @Override
    public long getMaximumEmc(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    /**
     * Attempts to insert a copied dropped item into the first adjacent inventory
     * handler. The ProjectEX 1.12 implementation intentionally does not consume
     * the dropped item and produces a full stack on each update.
     */
    @Override
    public <PEDESTAL extends BlockEntity & IDMPedestal> boolean updateInPedestal(
            ItemStack star, Level level, BlockPos pos, PEDESTAL pedestal) {
        int updateInterval = Config.finalStarUpdateInterval.get();
        if (updateInterval <= 0 || level.isClientSide
                || level.getGameTime() % (long) updateInterval != Util.mod(pos.hashCode(), updateInterval)) {
            return false;
        }

        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                new AABB(pos).expandTowards(0D, 1D, 0D));
        if (items.isEmpty()) {
            return false;
        }

        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            IItemHandler handler = blockEntity == null ? null : blockEntity
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .resolve()
                    .orElse(null);
            if (handler == null) {
                continue;
            }

            ItemStack copied = items.get(level.random.nextInt(items.size())).getItem().copy();
            if (!Config.finalStarCopyAnyItem.get() && !IEMCProxy.INSTANCE.hasValue(copied)) {
                continue;
            }

            copied.setCount(copied.getMaxStackSize());
            if (copied.isDamageableItem()) {
                copied.setDamageValue(0);
            }

            if (!Config.finalStarCopyNBT.get() && copied.hasTag()
                    && !copied.is(PETags.Items.NBT_WHITELIST)) {
                copied.setTag(new CompoundTag());
            }

            // Match the original behavior: the source entity remains in place,
            // while the handler receives as much of a full stack as it accepts.
            ItemHandlerHelper.insertItem(handler, copied, false);
            return false;
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Lang.Items.FINAL_STAR_CURIO_TOOLTIP.translateColored(ChatFormatting.GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<Component> getPedestalDescription() {
        return List.of(Lang.Items.FINAL_STAR_TOOLTIP.translate());
    }
}

