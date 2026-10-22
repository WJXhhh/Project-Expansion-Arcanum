package com.wjx.neoforge.projectexa.registries;

import com.wjx.neoforge.projectexa.Main;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public class DamageTypes {
    public static final ResourceKey<DamageType> WALK_ON_SUN = ResourceKey.create(Registries.DAMAGE_TYPE, Main.rl("walk_on_sun"));
    public static final ResourceKey<DamageType> STARE_AT_SUN = ResourceKey.create(Registries.DAMAGE_TYPE, Main.rl("stare_at_sun"));
}
