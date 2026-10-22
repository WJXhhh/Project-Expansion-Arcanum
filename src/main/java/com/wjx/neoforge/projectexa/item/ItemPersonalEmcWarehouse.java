package com.wjx.neoforge.projectexa.item;

import com.wjx.neoforge.projectexa.gui.container.ContainerPersonalEmcWarehouse;
import com.wjx.neoforge.projectexa.registries.DataComponentTypes;
import com.wjx.neoforge.projectexa.util.IHasCapability;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A bearer-bound virtual inventory. Only the server may mutate its EMC account. */
public class ItemPersonalEmcWarehouse extends ItemPE implements IHasCapability {
    public static final boolean ENABLED = true;
    public static final int FILTER_SLOTS = 27;
    private static final int MAX_CLIENT_AVAILABLE_COUNT = 9_999;

    public ItemPersonalEmcWarehouse() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());
    }

    @Override
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        IntegrationHelper.registerCuriosCapability(event, this);
        event.registerItem(Capabilities.ItemHandler.ITEM,
                (stack, context) -> new PersonalEmcWarehouseHandler(stack, null), this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer) {
            if (!isBound(stack) || player.isShiftKeyDown()) {
                bindTo(stack, player);
                player.displayClientMessage(Component.translatable(
                        "item.projectexa.personal_emc_warehouse.bound", player.getName()), true);
                if (player.isShiftKeyDown()) return InteractionResultHolder.success(stack);
            }
            player.openMenu(new Provider(hand), buffer -> buffer.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(isBound(stack)
                ? Component.translatable("item.projectexa.personal_emc_warehouse.account", getOwnerName(stack)).withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.projectexa.personal_emc_warehouse.unbound").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.filters", getFilterCount(stack)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return isBound(stack); }

    public static void bindTo(ItemStack stack, Player player) {
        stack.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()));
        if (!stack.has(DataComponentTypes.WAREHOUSE_ID.get())) stack.set(DataComponentTypes.WAREHOUSE_ID.get(), UUID.randomUUID());
        stack.remove(DataComponentTypes.WAREHOUSE_AVAILABILITY.get());
        refreshAvailability(stack);
    }

    public static Optional<UUID> getOwner(ItemStack stack) {
        var owner = stack.get(DataComponentTypes.OWNER.get());
        return owner == null ? Optional.empty() : Optional.of(owner.uuid());
    }

    public static String getOwnerName(ItemStack stack) {
        var owner = stack.get(DataComponentTypes.OWNER.get());
        return owner == null ? "?" : owner.name();
    }

    public static boolean isBound(ItemStack stack) { return getOwner(stack).isPresent(); }

    public static ItemStack getFilter(ItemStack warehouse, int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return ItemStack.EMPTY;
        var contents = warehouse.get(DataComponentTypes.WAREHOUSE_FILTERS.get());
        return contents == null || slot >= contents.getSlots() ? ItemStack.EMPTY : contents.getStackInSlot(slot).copy();
    }

    public static void setFilter(ItemStack warehouse, int slot, ItemStack filter) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        List<ItemStack> filters = new ArrayList<>(FILTER_SLOTS);
        for (int i = 0; i < FILTER_SLOTS; i++) filters.add(i == slot ? filter.copyWithCount(1) : getFilter(warehouse, i));
        warehouse.set(DataComponentTypes.WAREHOUSE_FILTERS.get(), ItemContainerContents.fromItems(filters));
        refreshAvailability(warehouse);
    }

    public static boolean refreshAvailability(ItemStack warehouse) {
        ServerPlayer owner = getOwner(warehouse).map(Util::getPlayer).orElse(null);
        IKnowledgeProvider provider = owner == null ? null : Util.getKnowledgeProvider(owner);
        List<Integer> counts = new ArrayList<>(FILTER_SLOTS);
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            ItemStack filter = getFilter(warehouse, slot);
            long value = filter.isEmpty() ? 0 : IEMCProxy.INSTANCE.getValue(filter);
            int count = provider != null && value > 0 && provider.hasKnowledge(filter)
                    ? owner.isCreative() ? MAX_CLIENT_AVAILABLE_COUNT
                    : Math.min(MAX_CLIENT_AVAILABLE_COUNT, Util.safeIntValue(provider.getEmc().max(BigInteger.ZERO).divide(BigInteger.valueOf(value)))) : 0;
            counts.add(count);
        }
        if (counts.equals(warehouse.get(DataComponentTypes.WAREHOUSE_AVAILABILITY.get()))) return false;
        warehouse.set(DataComponentTypes.WAREHOUSE_AVAILABILITY.get(), List.copyOf(counts));
        return true;
    }

    public static boolean hasAvailabilitySnapshot(ItemStack stack) { return hasAvailabilityCountSnapshot(stack); }
    public static boolean hasAvailabilityCountSnapshot(ItemStack stack) { return stack.has(DataComponentTypes.WAREHOUSE_AVAILABILITY.get()); }
    public static boolean isFilterAvailableSnapshot(ItemStack stack, int slot) { return getFilterAvailableCountSnapshot(stack, slot) > 0; }
    public static int getFilterAvailableCountSnapshot(ItemStack stack, int slot) {
        List<Integer> counts = stack.getOrDefault(DataComponentTypes.WAREHOUSE_AVAILABILITY.get(), List.of());
        return slot >= 0 && slot < counts.size() ? Math.max(0, counts.get(slot)) : 0;
    }

    public static int getFilterCount(ItemStack stack) {
        int count = 0;
        for (int slot = 0; slot < FILTER_SLOTS; slot++) if (!getFilter(stack, slot).isEmpty()) count++;
        return count;
    }

    public static ItemStack sanitizeFilter(ItemStack input) {
        if (input.isEmpty() || input.getItem() instanceof ItemPersonalEmcWarehouse) return ItemStack.EMPTY;
        ItemStack filter = Util.cleanStack(input);
        return IEMCProxy.INSTANCE.getValue(filter) > 0 ? filter : ItemStack.EMPTY;
    }

    public static boolean sameWarehouse(ItemStack first, ItemStack second) {
        UUID id = first.get(DataComponentTypes.WAREHOUSE_ID.get());
        return first.getItem() instanceof ItemPersonalEmcWarehouse && second.getItem() instanceof ItemPersonalEmcWarehouse
                && id != null && id.equals(second.get(DataComponentTypes.WAREHOUSE_ID.get()));
    }

    private record Provider(InteractionHand hand) implements MenuProvider {
        @Override
        public Component getDisplayName() { return Component.translatable("gui.projectexa.personal_emc_warehouse"); }
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new ContainerPersonalEmcWarehouse(id, inventory, hand);
        }
    }
}
