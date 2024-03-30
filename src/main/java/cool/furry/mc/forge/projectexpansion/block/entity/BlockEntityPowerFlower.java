package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.block.BlockPowerFlower;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.math.BigInteger;

@SuppressWarnings("unused")
public class BlockEntityPowerFlower extends BlockEntityOwnable implements IHasMatter, IHasSunBonus {
    public BigInteger emc = BigInteger.ZERO;
    public BlockEntityPowerFlower(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.POWER_FLOWER.get(), pos, state);
    }

    @Override
    @SuppressWarnings("unused")
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TagNames.STORED_EMC, emc.toString());
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityPowerFlower be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityPowerFlower blockEntity) {
        if (level.isClientSide || (level.getGameTime() % Config.tickDelay.get()) != Util.mod(hashCode(), Config.tickDelay.get())) return;
        BigInteger res = getMatter().getPowerFlowerOutputForTicks(Config.tickDelay.get());
        if(hasSunBonus() && getSunBonus() != null) {
            res = res.multiply(BigInteger.valueOf(getSunBonus()));
        }
        ServerPlayer player = Util.getPlayer(level, owner);

        if (player != null) {
            PowerFlowerCollector.add(player, emc.add(res));
            emc = BigInteger.ZERO;
            Util.markDirty(this);
        } else {
            emc = emc.add(res);
            Util.markDirty(this);
        }
    }

    @Override
    public @NotNull Matter getMatter() {
        return ((BlockPowerFlower) getBlockState().getBlock()).getMatter();
    }

    @Override
    public boolean hasSunBonus() {
        return BlockCompactSun.adjacent(level, worldPosition, Direction.DOWN);
    }
}
