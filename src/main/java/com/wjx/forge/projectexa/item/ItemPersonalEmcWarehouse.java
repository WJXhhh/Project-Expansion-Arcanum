package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.gui.container.ContainerPersonalEmcWarehouse;
import com.wjx.forge.projectexa.integrations.tacz.TaczIntegration;
import com.wjx.forge.projectexa.util.EMCFormat;
import com.wjx.forge.projectexa.util.TagNames;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.integration.IntegrationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ItemPersonalEmcWarehouse extends ItemPE {
    public static final int FILTER_SLOTS = 27;
    private static final String FILTERS = "Filters";
    private static final String WAREHOUSE_ID = "WarehouseId";

    public ItemPersonalEmcWarehouse() {
        super(new Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());
        addItemCapability(EmcWarehouseItemCapability::new);
        addItemCapability("curios", IntegrationHelper.CURIO_CAP_SUPPLIER);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean wasUnbound = getOwner(stack).isEmpty();
            if (wasUnbound || player.isShiftKeyDown()) {
                bindTo(stack, serverPlayer);
                player.displayClientMessage(Component.translatable(
                        "item.projectexa.personal_emc_warehouse.bound", player.getGameProfile().getName()), true);
                if (player.isShiftKeyDown()) {
                    return InteractionResultHolder.success(stack);
                }
            }
            NetworkHooks.openScreen(serverPlayer, new Provider(hand), buffer -> buffer.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.tooltip.bind").withStyle(ChatFormatting.DARK_GRAY));
        if (ModList.get().isLoaded("touhou_little_maid")) {
            tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.tooltip.maid")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        getOwner(stack).ifPresentOrElse(owner -> {
            tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.account", getOwnerName(stack))
                    .withStyle(ChatFormatting.GRAY));
            ServerPlayer player = Util.getPlayer(owner);
            if (player != null && Util.getKnowledgeProvider(player) != null) {
                tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.emc",
                        EMCFormat.format(Util.getKnowledgeProvider(player).getEmc())).withStyle(ChatFormatting.GRAY));
            }
        }, () -> tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.unbound")
                .withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.translatable("item.projectexa.personal_emc_warehouse.filters", getFilterCount(stack))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getOwner(stack).isPresent();
    }

    public static void bindTo(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TagNames.OWNER, player.getUUID());
        tag.putString(TagNames.OWNER_NAME, player.getGameProfile().getName());
        if (!tag.hasUUID(WAREHOUSE_ID)) {
            tag.putUUID(WAREHOUSE_ID, UUID.randomUUID());
        }
    }

    public static Optional<UUID> getOwner(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(TagNames.OWNER)
                ? Optional.of(tag.getUUID(TagNames.OWNER)) : Optional.empty();
    }

    public static String getOwnerName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TagNames.OWNER_NAME)
                ? tag.getString(TagNames.OWNER_NAME) : getOwner(stack).map(UUID::toString).orElse("?");
    }

    public static boolean isBound(ItemStack stack) {
        return getOwner(stack).isPresent();
    }

    public static ItemStack getFilter(ItemStack warehouse, int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS || warehouse.getTag() == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag filters = warehouse.getTag().getCompound(FILTERS);
        String key = Integer.toString(slot);
        return filters.contains(key, Tag.TAG_COMPOUND)
                ? ItemStack.of(filters.getCompound(key)) : ItemStack.EMPTY;
    }

    public static void setFilter(ItemStack warehouse, int slot, ItemStack filter) {
        if (slot < 0 || slot >= FILTER_SLOTS) {
            return;
        }
        CompoundTag root = warehouse.getOrCreateTag();
        CompoundTag filters = root.getCompound(FILTERS);
        String key = Integer.toString(slot);
        if (filter.isEmpty()) {
            filters.remove(key);
        } else {
            filters.put(key, filter.copyWithCount(1).save(new CompoundTag()));
        }
        if (filters.isEmpty()) {
            root.remove(FILTERS);
        } else {
            root.put(FILTERS, filters);
        }
    }

    public static int getFilterCount(ItemStack warehouse) {
        int count = 0;
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            if (!getFilter(warehouse, slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static ItemStack sanitizeFilter(ItemStack input) {
        if (input.isEmpty() || input.getItem() instanceof ItemPersonalEmcWarehouse) {
            return ItemStack.EMPTY;
        }
        ItemStack filter = ModList.get().isLoaded("tacz") && TaczIntegration.isAmmo(input)
                ? input.copyWithCount(1) : Util.cleanStack(input);
        return IEMCProxy.INSTANCE.getValue(filter) > 0 ? filter : ItemStack.EMPTY;
    }

    public static boolean sameWarehouse(ItemStack first, ItemStack second) {
        if (!(first.getItem() instanceof ItemPersonalEmcWarehouse)
                || !(second.getItem() instanceof ItemPersonalEmcWarehouse)) {
            return false;
        }
        CompoundTag firstTag = first.getTag();
        CompoundTag secondTag = second.getTag();
        return firstTag != null && secondTag != null && firstTag.hasUUID(WAREHOUSE_ID)
                && secondTag.hasUUID(WAREHOUSE_ID)
                && firstTag.getUUID(WAREHOUSE_ID).equals(secondTag.getUUID(WAREHOUSE_ID));
    }

    private record Provider(InteractionHand hand) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.projectexa.personal_emc_warehouse");
        }

        @Override
        public @Nullable AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
            return new ContainerPersonalEmcWarehouse(windowId, inventory, hand);
        }
    }
}
