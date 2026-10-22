package com.wjx.neoforge.projectexa.item;

import com.wjx.neoforge.projectexa.config.Config;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.block_entity.IDMPedestal;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.proxy.IEMCProxy;
import com.wjx.neoforge.projectexa.util.IHasCapability;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;


public class ItemFinalStar extends ItemPE implements IItemEmcHolder, IPedestalItem, IHasCapability {
    private static final long REPORTED_EMC = 1_000_000_000_000_000L;

    public ItemFinalStar() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        IntegrationHelper.registerCuriosCapability(event, this);
        event.registerItem(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY, (stack, context) -> this, this);
        event.registerItem(PECapabilities.PEDESTAL_ITEM_CAPABILITY, (stack, context) -> this, this);
    }

    /**
     * The Final Star is an effectively inexhaustible EMC provider. This mirrors
     * ProjectEX 1.12: the table can extract EMC from it, but the item never
     * stores the extracted amount in its own NBT.
     */
    @Override
    public long insertEmc(ItemStack stack, long toInsert, IEmcStorage.EmcAction action) {
        return 0L;
    }

    @Override
    public long extractEmc(ItemStack stack, long toExtract, IEmcStorage.EmcAction action) {
        return Math.max(0L, toExtract);
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
        int updateInterval = Config.server.finalStarUpdateInterval.get();
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
            IItemHandler handler = blockEntity == null ? null : level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(direction), direction.getOpposite());
            if (handler == null) {
                continue;
            }

            ItemStack copied = items.get(level.random.nextInt(items.size())).getItem().copy();
            if (!Config.server.finalStarCopyAnyItem.get() && !IEMCProxy.INSTANCE.hasValue(copied)) {
                continue;
            }

            copied.setCount(copied.getMaxStackSize());
            if (copied.isDamageableItem()) {
                copied.setDamageValue(0);
            }

            if (!Config.server.finalStarCopyNBT.get()) {
                copied = Util.cleanStack(copied).copyWithCount(copied.getMaxStackSize());
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.projectexa.final_star.curio_tooltip").withStyle(ChatFormatting.GRAY));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<Component> getPedestalDescription(float tickRate) {
        return List.of(Component.translatable("item.projectexa.final_star.tooltip"));
    }
}

