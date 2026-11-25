package com.wjx.neoforge.projectexa.integrations.tacz;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.components.DataComponentProcessor;
import moze_intel.projecte.api.components.IDataComponentProcessor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

@DataComponentProcessor(requiredMods = "tacz")
public final class TaczComponentProcessor implements IDataComponentProcessor {
    @Override public String getTranslationKey() { return "projectexa.integration.tacz.items"; }
    @Override public String getName() { return "TACZItemProcessor"; }
    @Override public String getDescription() { return "Preserves the freshly crafted data of each TaCZ gun, ammunition type and attachment."; }
    @Override public boolean hasPersistentComponents() { return true; }
    @Override public long recalculateEMC(ItemInfo info, long emc) { return emc; }

    @Override
    public void collectPersistentComponents(ItemInfo info, DataComponentPatch.Builder builder) {
        if (!info.hasModifiedComponents()) return;
        ItemStack canonical = TaczEmc.canonicalStack(info.createStack());
        if (canonical.isEmpty()) return;
        // TaCZ also sets the ammunition type's stack limit as a component.
        var components = canonical.getComponentsPatch().split();
        components.added().forEach(builder::set);
        components.removed().forEach(builder::remove);
    }
}
