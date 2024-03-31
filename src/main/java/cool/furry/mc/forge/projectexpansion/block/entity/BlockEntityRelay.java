package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.block.BlockCollector;
import cool.furry.mc.forge.projectexpansion.block.BlockRelay;
import cool.furry.mc.forge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.forge.projectexpansion.util.IHasMatter;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import cool.furry.mc.forge.projectexpansion.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class BlockEntityRelay extends BlockEntityEMC implements IHasMatter {
    public Matter matter;
    public static final Direction[] DIRECTIONS = Direction.values();
    public BlockEntityRelay(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.RELAY.get(), pos, state);
    }

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityRelay be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityRelay blockEntity) {
        // we can't use the user defined value due to emc duplication possibilities
        if ((level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;

        sendToAllAcceptors(getStoredEmcBigInteger(), Util.safeLongValue(getMatter().getRelayTransfer()));
    }


    @Override
    public @NotNull Matter getMatter() {
        BlockRelay block = (BlockRelay) getBlockState().getBlock();
        if (block.getMatter() != matter) {
            this.matter = block.getMatter();
        }
        return matter;
    }

    @Override
    public boolean isRelay() {
        return true;
    }

    public void addBonus() {
        if (getBlockState().getBlock() instanceof BlockRelay block) Util.stepBigInteger(getMatter().getRelayBonus(), (val) -> insertEmc(val, EmcAction.EXECUTE));
    }
}
