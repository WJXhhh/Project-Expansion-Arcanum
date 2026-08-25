package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
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
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public class ItemArcaneTransmutationTablet extends ItemPE {
    public ItemArcaneTransmutationTablet() {
        super(new Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant());
        addItemCapability("curios", IntegrationHelper.CURIO_CAP_SUPPLIER);
    }

    public static void openFromInventory(ServerPlayer player) {
        NetworkHooks.openScreen(player, new Provider(null), buffer -> {
            buffer.writeBoolean(false);
            buffer.writeByte(player.getInventory().selected);
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new Provider(hand), buffer -> {
                buffer.writeBoolean(true);
                buffer.writeEnum(hand);
                buffer.writeByte(player.getInventory().selected);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("item.projectexa.arcane_transmutation_tablet.tooltip").withStyle(ChatFormatting.GRAY));
    }

    private record Provider(@Nullable InteractionHand hand) implements MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("gui.projectexa.arcane_transmutation_tablet");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
            return new ContainerArcaneTransmutationTablet(windowId, inventory, hand, inventory.selected);
        }
    }
}
