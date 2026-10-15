package com.wjx.forge.projectexa.api.ammo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Opt-in bridge for a weapon's projectile lookup. Register once per weapon family.
 * Registration does NOT intercept an arbitrary weapon's consumption: that weapon must
 * call AmmoSources.consumeProjectile with the original preview, or use Selection.take.
 * Weapons inheriting the unchanged BowItem/CrossbowItem consumption paths are built in.
 */
public interface ProjectileAmmoAdapter {
    boolean supports(ItemStack weapon);

    boolean accepts(Player player, ItemStack weapon, ItemStack ammunition);

    /** Exempts payment only. The weapon remains responsible for its native pickup/usage rules. */
    default boolean isInfinite(Player player, ItemStack weapon, ItemStack ammunition) {
        return false;
    }
}
