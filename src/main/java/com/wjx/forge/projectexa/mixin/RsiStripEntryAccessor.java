package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.tree.IngredientKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.plan.CraftingPlanScreen$StripEntry", remap = false)
public interface RsiStripEntryAccessor {
    @Accessor("key") IngredientKey projectexa$key();
    @Accessor("available") long projectexa$available();
    @Accessor("count") long projectexa$count();
}
