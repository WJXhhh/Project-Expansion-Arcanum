package com.wjx.forge.projectexa.item;

import com.wjx.forge.projectexa.api.ammo.AmmoSources;
import com.wjx.forge.projectexa.Main;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-event lookup bridge; consumption is handled by the common API and narrow adapters. */
@Mod.EventBusSubscriber(modid = Main.MOD_ID)
public final class WarehouseProjectiles {
    private WarehouseProjectiles() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onGetProjectile(LivingGetProjectileEvent event) {
        // Preserve physical ammunition and any projectile supplied by another mod first.
        if (event.getEntity() instanceof Player player && event.getProjectileItemStack().isEmpty()) {
            ItemStack projectile = find(player, event.getProjectileWeaponItemStack());
            if (!projectile.isEmpty()) event.setProjectileItemStack(projectile);
        }
    }

    public static ItemStack find(Player player, ItemStack weapon) {
        return AmmoSources.previewProjectile(player, weapon);
    }

    public static boolean commit(Player player, ItemStack weapon, ItemStack projectile) {
        return AmmoSources.consumeProjectile(player, weapon, projectile);
    }

    /** Compatibility with the initial vanilla-only helper; policy now comes from its adapter. */
    @Deprecated
    public static boolean commit(Player player, ItemStack weapon, ItemStack projectile, boolean bow) {
        return commit(player, weapon, projectile);
    }
}
