package com.wjx.neoforge.projectexa.util;

import com.wjx.neoforge.projectexa.integrations.curios.CuriosIntegration;
import com.wjx.neoforge.projectexa.item.ItemArcaneTransmutationTablet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The server always checks possession again; a client-side JEI button grants no access. */
public final class ArcaneTabletAccess {
    private ArcaneTabletAccess() {}

    public static boolean hasTablet(Player player) { return find(player) != null; }

    private static ItemArcaneTransmutationTablet find(Player player) {
        if (player == null) return null;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).getItem() instanceof ItemArcaneTransmutationTablet tablet) return tablet;
        }
        var curios = CuriosIntegration.getAll(player);
        if (curios != null) {
            for (int slot = 0; slot < curios.getSlots(); slot++) {
                ItemStack stack = curios.getStackInSlot(slot);
                if (stack.getItem() instanceof ItemArcaneTransmutationTablet tablet) return tablet;
            }
        }
        return null;
    }

    public static boolean openFromInventory(ServerPlayer player) {
        var tablet = find(player);
        if (tablet == null) return false;
        tablet.openContainer(player);
        return true;
    }
}
