package cool.furry.mc.forge.projectexpansion.block.entity;

import cool.furry.mc.forge.projectexpansion.util.IEmcStorageBigInteger;
import cool.furry.mc.forge.projectexpansion.util.TagNames;
import cool.furry.mc.forge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.capability.managing.ICapabilityResolver;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public abstract class BlockEntityEMC extends BlockEntity implements IEmcStorageBigInteger {

    @Nullable private LazyOptional<IEmcStorage> emcStorageCapability;

    @Nullable protected ICapabilityResolver<IItemHandler> itemHandlerResolver;
    private BigInteger maximumEMC;
    private BigInteger emc = BigInteger.ZERO;
    private boolean updateComparators;
    public static final Direction[] DIRECTIONS = Direction.values();

    public BlockEntityEMC(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, BigInteger.valueOf(Constants.BLOCK_ENTITY_MAX_EMC));
    }

    public BlockEntityEMC(BlockEntityType<?> type, BlockPos pos, BlockState state, BigInteger maximumEMC) {
        super(type, pos, state);
        setMaximumEMC(maximumEMC);
    }

    /***************
     * Comparators *
     ***************/

    protected void updateComparators() {
        //Only update the comparator state if we need to update comparators
        //Note: We call this at the end of child implementations to try and update any changes immediately instead
        // of them having to be delayed a tick
        if (updateComparators) {
            BlockState state = getBlockState();
            if (!state.isAir()) {
                Objects.requireNonNull(level).updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            }
            updateComparators = false;
        }
    }

    protected boolean emcAffectsComparators() {
        return false;
    }

    /*******
     * EMC *
     *******/

    public final void setMaximumEMC(BigInteger max) {
        maximumEMC = max;
        if (getStoredEmcBigInteger().compareTo(getMaximumEmcBigInteger()) > 0) {
            emc = getMaximumEmcBigInteger();
            storedEmcChanged();
        }
    }

    @Override
    public BigInteger getStoredEmcBigInteger() {
        return emc;
    }

    @Override
    public BigInteger getMaximumEmcBigInteger() {
        return maximumEMC;
    }

    @Range(from = 0, to = Long.MAX_VALUE)
    protected long getEmcInsertLimit() {
        return getNeededEmc();
    }

    protected BigInteger getEmcInsertLimitBigInteger() {
        return getNeededEmcBigInteger();
    }

    @Range(from = 0, to = Long.MAX_VALUE)
    protected long getEmcExtractLimit() {
        return getStoredEmc();
    }

    protected BigInteger getEmcExtractLimitBigInteger() {
        return getStoredEmcBigInteger();
    }

    protected boolean canAcceptEmc() {
        return true;
    }

    protected boolean canProvideEmc() {
        return true;
    }

    @Override
    public BigInteger extractEmcBigInteger(BigInteger toExtract, EmcAction action) {
        if (toExtract.compareTo(BigInteger.ZERO) < 0) {
            return insertEmcBigInteger(toExtract.negate(), action);
        }
        if (canProvideEmc()) {
            return forceExtractEmcBigInteger(getStoredEmcBigInteger().min(toExtract), action);
        }
        return BigInteger.ZERO;
    }

    @Override
    public BigInteger insertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.compareTo(BigInteger.ZERO) < 0) {
            return extractEmcBigInteger(toAccept.negate(), action);
        }
        if (canAcceptEmc()) {
            return forceInsertEmcBigInteger(getEmcInsertLimitBigInteger().min(toAccept), action);
        }
        return BigInteger.ZERO;
    }

    protected long forceExtractEmc(long toExtract, EmcAction action) {
        return Util.safeLongValue(forceExtractEmcBigInteger(BigInteger.valueOf(toExtract), action));
    }

    protected BigInteger forceExtractEmcBigInteger(BigInteger toExtract, EmcAction action) {
        if (toExtract.compareTo(BigInteger.ZERO) < 0) {
            return forceInsertEmcBigInteger(toExtract.negate(), action);
        }
        BigInteger toRemove = getStoredEmcBigInteger().min(toExtract);
        if (action.execute()) {
            emc = emc.subtract(toRemove);
            storedEmcChanged();
        }
        return toRemove;
    }

    protected long forceInsertEmc(long toAccept, EmcAction action) {
        return Util.safeLongValue(forceInsertEmcBigInteger(BigInteger.valueOf(toAccept), action));
    }

    protected BigInteger forceInsertEmcBigInteger(BigInteger toAccept, EmcAction action) {
        if (toAccept.compareTo(BigInteger.ZERO) < 0) {
            return forceExtractEmcBigInteger(toAccept.negate(), action);
        }
        BigInteger toAdd = getNeededEmcBigInteger().min(toAccept);
        if (action.execute()) {
            emc = emc.add(toAdd);
            storedEmcChanged();
        }
        return toAdd;
    }

    protected void sendToAllAcceptors(BigInteger emc) {
        sendToAllAcceptors(emc, Long.MAX_VALUE);
    }

    protected void sendToAllAcceptors(BigInteger emc, long transferLimit) {
        if(level == null || !canProvideEmc()) {
            return;
        }
        emc = getEmcExtractLimitBigInteger().min(emc);

        List<IEmcStorage> targets = new ArrayList<>(1);

        for (Direction dir : DIRECTIONS) {
            BlockPos neighbor = worldPosition.relative(dir);
            if (level.isLoaded(neighbor)) {
                BlockEntity be = level.getBlockEntity(neighbor);
                if (be != null) {
                    be.getCapability(PECapabilities.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent((storage) -> {
                        if ((!isRelay() || !storage.isRelay()) && storage.insertEmc(1L, IEmcStorage.EmcAction.SIMULATE) > 0L) {
                            targets.add(storage);
                        }
                    });
                }
            }
        }

        BigInteger remaining = Util.spreadEMC(emc, targets, transferLimit);
        forceExtractEmcBigInteger(emc.subtract(remaining), EmcAction.EXECUTE);
    }


    protected void storedEmcChanged() {
        markDirty(emcAffectsComparators());
    }

    @Override
    public void setChanged() {
        markDirty(true);
    }


    /*******
     * NBT *
     *******/

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (getStoredEmcBigInteger().compareTo(getMaximumEmcBigInteger()) > 0) {
            emc = getMaximumEmcBigInteger();
        }
        tag.putString(TagNames.STORED_EMC, getStoredEmcBigInteger().toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        BigInteger set = new BigInteger(tag.getString(TagNames.STORED_EMC));
        if (set.compareTo(getMaximumEmcBigInteger()) > 0) {
            set = getMaximumEmcBigInteger();
        }
        emc = set;
    }

    public void markDirty(boolean recheckComparators) {
        if (level != null) {
            if (level.hasChunkAt(worldPosition)) {
                level.getChunkAt(worldPosition).setUnsaved(true);
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            if (recheckComparators && !level.isClientSide) {
                updateComparators = true;
            }
        }
    }

    @Override
    public final CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /****************
     * Capabilities *
     ****************/

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == PECapabilities.EMC_STORAGE_CAPABILITY) {
            if (emcStorageCapability == null || !emcStorageCapability.isPresent()) {
                //If the capability has not been retrieved yet, or it is not valid then recreate it
                emcStorageCapability = LazyOptional.of(() -> this);
            }
            return emcStorageCapability.cast();
        }

        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && itemHandlerResolver != null) {
            return itemHandlerResolver.getCapabilityUnchecked(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (emcStorageCapability != null && emcStorageCapability.isPresent()) {
            emcStorageCapability.invalidate();
            emcStorageCapability = null;
        }

        if (itemHandlerResolver != null) {
            itemHandlerResolver.invalidateAll();
        }
    }

    protected class StackHandler extends ItemStackHandler {

        protected StackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }

    protected class CompactableStackHandler extends StackHandler {

        //Start as needing to check for compacting when loaded
        private boolean needsCompacting = true;
        private boolean empty;

        protected CompactableStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            needsCompacting = true;
        }

        public void compact() {
            if (needsCompacting) {
                if (level != null && !level.isClientSide) {
                    empty = ItemHelper.compactInventory(this);
                }
                needsCompacting = false;
            }
        }

        @Override
        protected void onLoad() {
            super.onLoad();
            empty = IntStream.range(0, getSlots()).allMatch(slot -> getStackInSlot(slot).isEmpty());
        }

        /**
         * @apiNote Only use this on the server
         */
        public boolean isEmpty() {
            return empty;
        }
    }
}
