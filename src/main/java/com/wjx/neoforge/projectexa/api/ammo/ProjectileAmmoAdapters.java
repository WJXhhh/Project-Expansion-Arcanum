package com.wjx.neoforge.projectexa.api.ammo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared, opt-in registry. More recently registered custom adapters take precedence. */
public final class ProjectileAmmoAdapters {
    private static final CopyOnWriteArrayList<Entry> CUSTOM = new CopyOnWriteArrayList<>();
    private static final ProjectileAmmoAdapter BOW = new StandardAdapter(true);
    private static final ProjectileAmmoAdapter CROSSBOW = new StandardAdapter(false);

    private ProjectileAmmoAdapters() {
    }

    public static synchronized void register(ResourceLocation id, ProjectileAmmoAdapter adapter) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(adapter, "adapter");
        if (CUSTOM.stream().anyMatch(entry -> entry.id().equals(id))) {
            throw new IllegalArgumentException("Duplicate projectile ammo adapter: " + id);
        }
        CUSTOM.add(0, new Entry(id, adapter));
    }

    public static synchronized boolean unregister(ResourceLocation id) {
        return CUSTOM.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<ProjectileAmmoAdapter> find(ItemStack weapon) {
        if (weapon.isEmpty()) return Optional.empty();
        for (Entry entry : CUSTOM) {
            if (entry.adapter().supports(weapon)) return Optional.of(entry.adapter());
        }
        if (BOW.supports(weapon)) return Optional.of(BOW);
        if (CROSSBOW.supports(weapon)) return Optional.of(CROSSBOW);
        // Never hand an unadapted weapon a virtual stack that it could shrink for free.
        return Optional.empty();
    }

    private record Entry(ResourceLocation id, ProjectileAmmoAdapter adapter) {
    }

    private record StandardAdapter(boolean bow) implements ProjectileAmmoAdapter {
        @Override
        public boolean supports(ItemStack weapon) {
            return bow ? weapon.getItem() instanceof BowItem : weapon.getItem() instanceof CrossbowItem;
        }

        @Override
        public boolean accepts(Player player, ItemStack weapon, ItemStack ammunition) {
            return ((ProjectileWeaponItem) weapon.getItem()).getAllSupportedProjectiles(weapon).test(ammunition);
        }

        @Override
        public boolean isInfinite(Player player, ItemStack weapon, ItemStack ammunition) {
            return bow && ammunition.getItem() instanceof ArrowItem arrow
                    && arrow.isInfinite(ammunition, weapon, player);
        }
    }
}
