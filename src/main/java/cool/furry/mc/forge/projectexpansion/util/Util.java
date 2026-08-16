package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.Blocks;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.event.PlayerAttemptLearnEvent;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.nbt.NBTManager;
import moze_intel.projecte.gameObjs.items.IFireProtector;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static moze_intel.projecte.api.capabilities.block_entity.IEmcStorage.EmcAction;

@SuppressWarnings("unused")
public class Util {
    // yes I know this exists in net.minecraft.util.Util but having to either type out that fully or
    // this package to import both is really annoying
    public static final UUID DUMMY_UUID = new UUID(0L, 0L);

    public static @Nullable
    ServerPlayer getPlayer(UUID uuid) {
        return ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    public static @Nullable
    ServerPlayer getPlayer(@Nullable Level level, UUID uuid) {
        return level == null || level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
    }

    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack stackCopy = ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (stackCopy.isDamageableItem()) stackCopy.setDamageValue(0);
        return NBTManager.getPersistentInfo(ItemInfo.fromStack(stackCopy)).createStack();
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, Item rawItem, Item cleanItem) {
        return addKnowledge(player, provider, ItemInfo.fromItem(rawItem), ItemInfo.fromItem(cleanItem));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemStack rawStack) {
        return addKnowledge(player, provider, rawStack, cleanStack(rawStack));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemStack rawStack, ItemStack cleanStack) {
        return addKnowledge(player, provider, ItemInfo.fromStack(rawStack), ItemInfo.fromStack(cleanStack));
    }

    public static AddKnowledgeResult addKnowledge(Player player, IKnowledgeProvider provider, ItemInfo rawInfo, ItemInfo cleanInfo) {
        if (cleanInfo.createStack().isEmpty()) return AddKnowledgeResult.FAIL;

        if (!provider.hasKnowledge(cleanInfo)) {
            if (MinecraftForge.EVENT_BUS.post(new PlayerAttemptLearnEvent(player, rawInfo, cleanInfo)))
                return AddKnowledgeResult.FAIL;

            provider.addKnowledge(cleanInfo);
            return AddKnowledgeResult.SUCCESS;
        }

        return AddKnowledgeResult.UNKNOWN;
    }

    public static boolean areStacksEqual(RegistryAccess registryAccess, ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        return ItemStack.isSameItemSameTags(first, second);
    }

    public static ItemStack returnToInventoryOrTransmutation(Inventory playerInv, Player player, IKnowledgeProvider provider,
                                                              ItemStack stack, boolean ignoreNbt, boolean force) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (IEMCProxy.INSTANCE.hasValue(stack) && (ignoreNbt || areStacksEqual(player.level().registryAccess(), stack, cleanStack(stack)))) {
            AddKnowledgeResult result = addKnowledge(player, provider, stack);
            if (result != AddKnowledgeResult.FAIL) {
                BigInteger value = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack));
                provider.setEmc(provider.getEmc().add(value.multiply(BigInteger.valueOf(stack.getCount()))));
                if (player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
                return ItemStack.EMPTY;
            }
        }
        return returnToInventory(playerInv, player, stack, force);
    }

    public static ItemStack returnToInventory(Inventory playerInv, Player player, ItemStack stack, boolean force) {
        ItemStack remaining = stack.copy();
        while (!remaining.isEmpty()) {
            int slot = playerInv.getSlotWithRemainingSpace(remaining);
            if (slot == -1) slot = playerInv.getFreeSlot();
            if (slot == -1) {
                if (force) {
                    player.drop(remaining, false);
                    remaining = ItemStack.EMPTY;
                }
                break;
            }

            ItemStack existing = playerInv.getItem(slot);
            int limit = Math.min(remaining.getMaxStackSize(), playerInv.getMaxStackSize());
            int canMove = Math.min(remaining.getCount(), existing.isEmpty() ? limit : limit - existing.getCount());
            if (canMove <= 0) {
                if (force) {
                    player.drop(remaining, false);
                    remaining = ItemStack.EMPTY;
                }
                break;
            }

            ItemStack moved = remaining.split(canMove);
            if (existing.isEmpty()) playerInv.setItem(slot, moved);
            else existing.grow(canMove);

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY,
                        0, slot, playerInv.getItem(slot)));
            }
        }
        return remaining;
    }

    public static int safeIntValue(BigInteger val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static int safeIntValue(BigDecimal val) {
        try {
            return val.intValueExact();
        } catch (ArithmeticException ignore) {
            return Integer.MAX_VALUE;
        }
    }

    public static long safeLongValue(BigInteger val) {
        try {
            return val.longValueExact();
        } catch (ArithmeticException ignore) {
            return Long.MAX_VALUE;
        }
    }

    public static int mod(int a, int b) {
        a = a % b;
        return a < 0 ? a + b : a;
    }

    public static void markDirty(BlockEntity block) {
        if (block.getLevel() != null) markDirty(block.getLevel(), block);
    }

    public static void markDirty(Level level, BlockEntity block) {
        markDirty(level, block.getBlockPos());
    }

    public static void markDirty(Level level, BlockPos pos) {
        level.getChunkAt(pos).setUnsaved(true);
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(UUID uuid) {
        @Nullable ServerPlayer player = getPlayer(uuid);
        if(player == null) {
            return null;
        }
        return getKnowledgeProvider(player);
    }

    public static @Nullable IKnowledgeProvider getKnowledgeProvider(Player player) {
        try {
            return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).orElseThrow(NullPointerException::new);
        } catch(NullPointerException ignore) {
            return null;
        }
    }

    public static long spreadEMC(long emc, List<IEmcStorage> storageList) {
        return spreadEMC(emc, storageList, null);
    }

    public static long spreadEMC(long emc, List<IEmcStorage> storageList, @Nullable Long maxPer) {
        return Util.safeLongValue(spreadEMC(BigInteger.valueOf(emc), storageList, maxPer));
    }

    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList) {
        return spreadEMC(emc, storageList, null);
    }

    /**
     * Spreads EMC evenly across multiple targets
     * @param emc EMC to spread
     * @param storageList List of targets
     * @param maxPer Maximum amount to insert per target
     * @return The remaining EMC
     */
    public static BigInteger spreadEMC(BigInteger emc, List<IEmcStorage> storageList, @Nullable Long maxPer) {
        if(emc.equals(BigInteger.ZERO) || storageList.isEmpty()) return emc;
        List<IEmcStorage> notAccepting = new ArrayList<>();
        emc = stepBigInteger(emc, (val) -> {
            long div = val / storageList.size();
            if(maxPer != null && maxPer < div) div = maxPer;
            parentLoop: while (val > 0 && notAccepting.size() < storageList.size()) {
                for(IEmcStorage storage : storageList) {
                    if(notAccepting.contains(storage)) continue;
                    if(val == 0) break parentLoop;
                    if(val < div) div = val;
                    long oldVal = val;
                    val -= storage.insertEmc(div, EmcAction.EXECUTE);
                    if(val.equals(oldVal)) notAccepting.add(storage);
                    if(maxPer != null && oldVal - val >= maxPer) notAccepting.add(storage);
                }
            }
            return val;
        });
        return emc;
    }

    public static BigInteger stepBigInteger(BigInteger value, Function<Long, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    public static BigInteger stepBigInteger(BigInteger value, Long step, Function<Long, Long> func) {
        return stepBigInteger(value, step, (a, b) -> func.apply(a));
    }

    public static BigInteger stepBigInteger(BigInteger value, BiFunction<Long, BigInteger, Long> func) {
        return stepBigInteger(value, Long.MAX_VALUE, func);
    }

    // consumer values: step, leftover
    // consumer return: leftover (from step)
    public static BigInteger stepBigInteger(BigInteger value, Long step, BiFunction<Long, BigInteger, Long> func) {
        if(value.equals(BigInteger.ZERO)) return value;
        long localValue;
        while((localValue = Math.min(step, safeLongValue(value))) > 0L) {
            value = value.subtract(BigInteger.valueOf(localValue));
            Long unused = func.apply(localValue, value);
            if(unused > 0L) {
                value = value.add(BigInteger.valueOf(unused));
                break;
            }
        }
        return value;
    }

    public enum AddKnowledgeResult {
        FAIL,
        UNKNOWN,
        SUCCESS,
    }

    public static @Nullable ServerLevel getDimension(ResourceKey<Level> dimension) {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dimension);
    }

    public static Style suggestCommand(Style style, String command) {
        return style
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command).withStyle(ChatFormatting.RED)));
    }

    public static BigDecimal divide(BigInteger dividend, BigInteger divisor) {
        return new BigDecimal(dividend).divide(new BigDecimal(divisor), 2, RoundingMode.HALF_EVEN);
    }

    public static int scaleToRedstone(long currentAmount, long max) {
        return scaleToRedstone(BigInteger.valueOf(currentAmount), BigInteger.valueOf(max));
    }

    /**
     * Scales this proportion into redstone, where 0 means none, 15 means full, and the rest are an appropriate scaling.
     */
    public static int scaleToRedstone(BigInteger currentAmount, BigInteger max) {
        double proportion = divide(currentAmount, max).doubleValue();
        if (currentAmount.compareTo(BigInteger.ZERO) <= 0) {
            return 0;
        }
        if (currentAmount.compareTo(max) >= 0) {
            return 15;
        }
        return (int) Math.round(proportion * 13 + 1);
    }

    public static boolean isImmuneToFire(ServerPlayer player) {
        return isImmuneToFire(player, 0);
    }

    /**
     * Checks if the player is immune to fire damage
     * @param player The player to check
     * @param timeTolerance If the player is immune to fire for this amount of time or less, they will be considered not immune
     * @return If the player is immune to fire
     */
    public static boolean isImmuneToFire(ServerPlayer player, int timeTolerance) {
        ItemStack chestplate = player.getInventory().getArmor(EquipmentSlot.CHEST.getIndex());
        if (chestplate.getItem() instanceof IFireProtector protector && protector.canProtectAgainstFire(chestplate, player)) {
            return true;
        }

        @Nullable MobEffectInstance fireResistance = player.getEffect(MobEffects.FIRE_RESISTANCE);
        if (fireResistance != null && fireResistance.getDuration() > timeTolerance) {
            return true;
        }

        return player.fireImmune();
    }

    public static String ucwords(String str) {
        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : str.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalize = true;
            } else if (capitalize) {
                c = Character.toTitleCase(c);
                capitalize = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static int getSunMultiplier() {
        float pfValue = (float) IEMCProxy.INSTANCE.getValue(Objects.requireNonNull(Matter.FINAL.getPowerFlower()));
        float sunValue = (float) IEMCProxy.INSTANCE.getValue(Blocks.COMPACT_SUN.get());
        if (pfValue == 0 || sunValue == 0) return Config.compactSunBonus.get();
        int diff = (int) Math.ceil(sunValue / pfValue) + 1;
        // round to nearest 10
        return ((int) Math.ceil(diff / 10.0) * 10);
    }

    public static int getSunBonus() {
        if (Config.sunMultiplierPriceCompensation.get()) {
            int priceBonus = getSunMultiplier();
            int staticBonus = Config.compactSunBonus.get();
            return Math.max(priceBonus, staticBonus);
        } else {
            return Config.compactSunBonus.get();
        }
    }
}
