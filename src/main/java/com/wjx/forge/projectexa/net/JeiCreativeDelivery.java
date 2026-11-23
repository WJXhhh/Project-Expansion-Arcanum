package com.wjx.forge.projectexa.net;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.packets.to_client.PacketJeiCreativeCursor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Holds items until the creative client confirms that it received them. */
@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public final class JeiCreativeDelivery {
    private record Pending(UUID id, ItemStack purchased) { }
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private JeiCreativeDelivery() { }

    public static boolean hasPending(ServerPlayer player) {
        return PENDING.containsKey(player.getUUID());
    }

    public static void begin(ServerPlayer player, ItemStack purchased, ItemStack expected, ItemStack result) {
        UUID id = UUID.randomUUID();
        PENDING.put(player.getUUID(), new Pending(id, purchased.copy()));
        PacketHandler.sendTo(new PacketJeiCreativeCursor(id, expected.copy(), result.copy()), player);
    }

    public static void finish(ServerPlayer player, UUID id, boolean accepted) {
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null || !pending.id().equals(id)) return;
        PENDING.remove(player.getUUID());
        if (!accepted) returnToInventory(player, pending.purchased());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Pending pending = PENDING.remove(player.getUUID());
            if (pending != null) returnToInventory(player, pending.purchased());
        }
    }

    private static void returnToInventory(ServerPlayer player, ItemStack stack) {
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(
                new PlayerMainInvWrapper(player.getInventory()), stack.copy(), false);
        if (!remaining.isEmpty()) player.drop(remaining, false);
        player.containerMenu.broadcastChanges();
    }
}
