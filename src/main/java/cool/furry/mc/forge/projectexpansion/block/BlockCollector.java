package cool.furry.mc.forge.projectexpansion.block;

import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.blocks.BlockDirection;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class BlockCollector extends BlockDirection implements IHasMatter, EntityBlock {
    private final Matter matter;

    public BlockCollector(Matter matter) {
        super(Block.Properties.of(Material.STONE).strength(0.3F, 0.9F).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15)));
        this.matter = matter;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCollector(pos, state);
    }

    @Override
    public @NotNull Matter getMatter() {
        return matter;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
        list.add(Lang.Blocks.COLLECTOR_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.COLLECTOR_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.COLLECTOR_STACK_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.tickDelay.get()).multiply(BigInteger.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.Blocks.COLLECTOR_MAX_STORAGE.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(Fuel.getCollectorEMCLimit(getMatter())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.COLLECTOR.get() && !level.isClientSide) return BlockEntityCollector::tickServer;
        return null;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public MaterialColor getMapColor(BlockState state, BlockGetter level, BlockPos pos, MaterialColor defaultColor) {
        MaterialColor color = matter.materialColor == null ? null : matter.materialColor.get();
        return color != null ? color : super.getMapColor(state, level, pos, defaultColor);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
        if (collector != null) {
            NetworkHooks.openScreen((ServerPlayer) player, collector, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @Deprecated
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos, true);
        if (collector == null) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        Optional<IItemHandler> cap = collector.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).resolve();
        if (cap.isEmpty()) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        ItemStack charging = cap.get().getStackInSlot(BlockEntityCollector.UPGRADING_SLOT);
        if (!charging.isEmpty()) {
            Optional<IItemEmcHolder> holderCapability = charging.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).resolve();
            if (holderCapability.isPresent()) {
                IItemEmcHolder emcHolder = holderCapability.get();
                return Util.scaleToRedstone(emcHolder.getStoredEmc(charging), emcHolder.getMaximumEmc(charging));
            }
            return Util.scaleToRedstone(collector.getStoredEmcBigInteger(), BigInteger.valueOf(collector.getEmcToNextGoal()));
        }
        return Util.scaleToRedstone(collector.getStoredEmcBigInteger(), collector.getMaximumEmcBigInteger());
    }

    @Override
    @Deprecated
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
            if (collector != null) {
                //Clear the ghost slot so calling super doesn't drop the item in it
                collector.clearLocked();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
