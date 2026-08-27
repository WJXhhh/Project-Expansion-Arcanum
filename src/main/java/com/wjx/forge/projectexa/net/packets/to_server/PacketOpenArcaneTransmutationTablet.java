package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.item.ItemArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.packets.IPacket;
import com.wjx.forge.projectexa.registries.Items;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkEvent;

public record PacketOpenArcaneTransmutationTablet() implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && hasTablet(player)) {
            ItemArcaneTransmutationTablet.openFromInventory(player);
        }
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
    }

    public static PacketOpenArcaneTransmutationTablet decode(FriendlyByteBuf buffer) {
        return new PacketOpenArcaneTransmutationTablet();
    }

    public static boolean hasTablet(Player player) {
        if (player == null) return false;
        Item tablet = Items.ARCANE_TRANSMUTATION_TABLET.get();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isTablet(player.getInventory().getItem(slot), tablet)) {
                return true;
            }
        }

        if (ModList.get().isLoaded(IntegrationHelper.CURIO_MODID)) {
            IItemHandler curios = CuriosIntegration.getAll(player);
            if (curios != null) {
                for (int slot = 0; slot < curios.getSlots(); slot++) {
                    if (isTablet(curios.getStackInSlot(slot), tablet)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isTablet(ItemStack stack, Item tablet) {
        return !stack.isEmpty() && stack.getItem() == tablet;
    }
}
