package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.net.packets.to_client.PacketForgematicaTransmuteResult;
import com.wjx.forge.projectexa.util.Util;
import io.netty.handler.codec.DecoderException;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The schematic stays on the client. The server authorizes and prices each requested item. */
public record PacketForgematicaTransmute(List<Demand> demands, boolean easyPlace) implements IPacket {
    public PacketForgematicaTransmute(List<Demand> demands) {
        this(demands, false);
    }
    public static final int MAX_MATERIALS = 4096;
    private static final String MESSAGE_PREFIX = "text.projectexa.forgematica.";

    public record Demand(ItemStack stack, long needed, long shortage) {}

    private enum Failure {
        UNSUPPORTED_VARIANT, NO_EMC, INSUFFICIENT_EMC, NO_SPACE
    }

    private record FailureSummary(int count, Component firstMaterial) {}

    private record MaterialKey(Item item, CompoundTag tag) {
        private MaterialKey(ItemStack stack) {
            this(stack.getItem(), stack.getTag());
        }
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || player.isSpectator() || player.containerMenu != player.inventoryMenu
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            if (easyPlace) {
                PacketHandler.sendTo(new PacketForgematicaTransmuteResult(Component.translatable("text.projectexa.jei_transmute.provider_unavailable"), true, false), player);
            } else {
                reply(player, Component.translatable("text.projectexa.jei_transmute.provider_unavailable"));
            }
            return;
        }

        Inventory inventory = player.getInventory();
        Map<MaterialKey, Long> available = easyPlace ? countDirectAvailable(inventory) : countAvailable(inventory);

        BigInteger startingEmc = provider.getEmc();
        BigInteger remainingEmc = startingEmc;
        int deliveredTotal = 0;
        Map<Failure, FailureSummary> failures = new EnumMap<>(Failure.class);
        List<Component> unlearned = new ArrayList<>();
        for (Demand demand : demands) {
            MaterialKey key = new MaterialKey(demand.stack());
            long shortage = Math.min(demand.shortage(), demand.needed() - available.getOrDefault(key, 0L));
            if (shortage <= 0) {
                continue;
            }

            ItemStack output = Util.cleanStack(demand.stack());
            if (!ItemStack.isSameItemSameTags(output, demand.stack())) {
                recordFailure(failures, Failure.UNSUPPORTED_VARIANT, demand.stack());
                continue;
            }
            long value = IEMCProxy.INSTANCE.getValue(output);
            if (value <= 0) {
                recordFailure(failures, Failure.NO_EMC, output);
                continue;
            }
            if (!provider.hasKnowledge(output)) {
                unlearned.add(output.getHoverName());
                continue;
            }

            BigInteger unitCost = BigInteger.valueOf(value);
            int affordable = Util.safeIntValue(remainingEmc.max(BigInteger.ZERO).divide(unitCost));
            int amount = (int) Math.min(shortage, affordable);
            if (amount == 0) {
                recordFailure(failures, Failure.INSUFFICIENT_EMC, output);
                continue;
            }
            ItemStack leftover = Util.returnToInventory(inventory, player, output.copyWithCount(amount), false);
            int delivered = amount - leftover.getCount();
            if (delivered > 0) {
                if (getShulkerData(output) != null) {
                    available = countAvailable(inventory);
                } else {
                    available.merge(key, (long) delivered, Long::sum);
                }
                deliveredTotal += delivered;
                remainingEmc = remainingEmc.subtract(unitCost.multiply(BigInteger.valueOf(delivered)));
            }
            if (delivered < amount) {
                recordFailure(failures, Failure.NO_SPACE, output);
            } else if (delivered < shortage) {
                recordFailure(failures, Failure.INSUFFICIENT_EMC, output);
            }
        }

        int incompleteMaterials = unlearned.size() + failures.values().stream().mapToInt(FailureSummary::count).sum();
        MutableComponent message;
        if (deliveredTotal > 0) {
            provider.setEmc(remainingEmc);
            provider.syncEmc(player);
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            String emcSpent = startingEmc.subtract(remainingEmc).toString();
            message = incompleteMaterials > 0
                    ? Component.translatable(MESSAGE_PREFIX + "partial", deliveredTotal, emcSpent, incompleteMaterials)
                    : Component.translatable(MESSAGE_PREFIX + "converted", deliveredTotal, emcSpent);
        } else if (incompleteMaterials > 0) {
            message = Component.translatable(MESSAGE_PREFIX + "failed", incompleteMaterials);
        } else {
            message = Component.translatable(MESSAGE_PREFIX + "nothing_missing");
        }
        for (Map.Entry<Failure, FailureSummary> entry : failures.entrySet()) {
            String key = MESSAGE_PREFIX + "failure." + entry.getKey().name().toLowerCase(Locale.ROOT);
            FailureSummary summary = entry.getValue();
            Component detail = summary.count() == 1
                    ? Component.translatable(key, summary.firstMaterial())
                    : Component.translatable(key + ".multiple", summary.count());
            message.append("\n").append(detail);
        }
        if (!unlearned.isEmpty()) {
            message.append("\n").append(Component.translatable(MESSAGE_PREFIX + "unlearned_list", unlearned.size()));
            for (Component material : unlearned) {
                message.append("\n  ").append(material);
            }
        }
        if (easyPlace) {
            PacketHandler.sendTo(new PacketForgematicaTransmuteResult(incompleteMaterials > 0 ? message : null, true, incompleteMaterials == 0), player);
        } else {
            reply(player, message);
        }
    }

    private static void recordFailure(Map<Failure, FailureSummary> failures, Failure reason, ItemStack stack) {
        failures.compute(reason, (key, summary) -> summary == null
                ? new FailureSummary(1, stack.getHoverName())
                : new FailureSummary(summary.count() + 1, summary.firstMaterial()));
    }

    private static Map<MaterialKey, Long> countDirectAvailable(Inventory inventory) {
        Map<MaterialKey, Long> counts = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot >= 36 && slot != 40) continue;
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) counts.merge(new MaterialKey(stack), (long) stack.getCount(), Long::sum);
        }
        return counts;
    }

    // Forgematica counts the inventory slots and the contents of carried shulker boxes.
    private static Map<MaterialKey, Long> countAvailable(Inventory inventory) {
        Map<MaterialKey, Long> counts = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(new MaterialKey(stack), (long) stack.getCount(), Long::sum);
            CompoundTag data = getShulkerData(stack);
            if (data == null) {
                continue;
            }
            ListTag contents = data.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < contents.size(); i++) {
                ItemStack stored = ItemStack.of(contents.getCompound(i));
                if (!stored.isEmpty()) {
                    counts.merge(new MaterialKey(stored), (long) stored.getCount(), Long::sum);
                }
            }
        }
        return counts;
    }

    private static CompoundTag getShulkerData(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock
                ? BlockItem.getBlockEntityData(stack) : null;
    }

    private static void reply(ServerPlayer player, Component message) {
        PacketHandler.sendTo(new PacketForgematicaTransmuteResult(message), player);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(easyPlace);
        buffer.writeVarInt(demands.size());
        for (Demand demand : demands) {
            // The default stack codec can omit tags even for a material whose tag determines its identity.
            buffer.writeId(BuiltInRegistries.ITEM, demand.stack().getItem());
            buffer.writeNbt(demand.stack().getTag());
            buffer.writeVarLong(demand.needed());
            buffer.writeVarLong(demand.shortage());
        }
    }

    public static PacketForgematicaTransmute decode(FriendlyByteBuf buffer) {
        boolean easyPlace = buffer.readBoolean();
        int size = buffer.readVarInt();
        if (size < 1 || size > MAX_MATERIALS) {
            throw new DecoderException("Invalid Forgematica material list size");
        }
        List<Demand> demands = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Item item = buffer.readById(BuiltInRegistries.ITEM);
            CompoundTag tag = buffer.readNbt();
            long needed = buffer.readVarLong();
            long shortage = buffer.readVarLong();
            if (item == null || shortage <= 0 || shortage > needed) {
                throw new DecoderException("Invalid Forgematica material demand");
            }
            ItemStack stack = new ItemStack(item);
            if (stack.isEmpty()) {
                throw new DecoderException("Invalid Forgematica material item");
            }
            stack.setTag(tag);
            demands.add(new Demand(stack, needed, shortage));
        }
        if (easyPlace && (demands.size() != 1 || demands.get(0).needed() != 1 || demands.get(0).shortage() != 1)) {
            throw new DecoderException("Invalid easy place demand");
        }
        return new PacketForgematicaTransmute(List.copyOf(demands), easyPlace);
    }
}
