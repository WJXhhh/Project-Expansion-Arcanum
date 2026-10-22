package com.wjx.neoforge.projectexa.integrations.goety;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;

/** Preserve only the verified Pure marker, not arbitrary custom data, as blood's learned identity. */
@DataComponentProcessor(requiredMods = "goety")
public final class GoetyPureBloodProcessor implements IDataComponentProcessor {
    private static final ResourceLocation BLOOD = ResourceLocation.fromNamespaceAndPath("goety", "unholy_blood");
    @Override public String getTranslationKey() { return "projectexa.integration.goety.pure_blood"; }
    @Override public String getName() { return "GoetyPureUnholyBlood"; }
    @Override public String getDescription() { return "Keep Goety's pure unholy blood distinct in learned knowledge and EMC values."; }
    @Override public boolean hasPersistentComponents() { return true; }
    @Override public long recalculateEMC(ItemInfo info, long current) { return current; }
    @Override
    public void collectPersistentComponents(ItemInfo info, DataComponentPatch.Builder builder) {
        if (!BLOOD.equals(BuiltInRegistries.ITEM.getKey(info.getItem().value()))) return;
        CustomData data = info.getOrNull(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains("Pure")) {
            CompoundTag marker = new CompoundTag(); marker.putBoolean("Pure", true);
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        }
    }
}
