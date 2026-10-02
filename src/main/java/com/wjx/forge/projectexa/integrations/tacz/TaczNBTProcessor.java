package com.wjx.forge.projectexa.integrations.tacz;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.nbt.INBTProcessor;
import moze_intel.projecte.api.nbt.NBTProcessor;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

/**
 * Reduces a TACZ gun, cartridge or attachment to the identity NBT its EMC value is registered under.
 *
 * <p>ProjectE calls this whenever it needs the "persistent" form of a stack: learning an item, condenser
 * targets and this mod's own transmutation helpers all go through it. Without it a gun would only be
 * recognised while it was pristine, because loading ammo or installing an attachment rewrites its tag.</p>
 *
 * <p>The returned tag is the stack the gun smith table would craft, so a transmuted gun is identical to a
 * crafted one. Toggle it through ProjectE's own {@code config/ProjectE/processing.toml}
 * ({@code processors.TACZItemProcessor}).</p>
 */
@NBTProcessor(requiredMods = "tacz")
public class TaczNBTProcessor implements INBTProcessor {

    @Override
    public String getName() {
        return "TACZItemProcessor";
    }

    @Override
    public String getDescription() {
        return "Keeps the gun/ammo/attachment id of TACZ items so each of them keeps its own EMC value, "
                + "and drops the volatile rest of the tag (ammo count, fire mode, attachments, level, skins).";
    }

    @Override
    public boolean hasPersistentNBT() {
        return true;
    }

    @Override
    public long recalculateEMC(ItemInfo info, long currentEMC) throws ArithmeticException {
        // The value itself comes from the EMC map entry of the canonical form; nothing to add here.
        return currentEMC;
    }

    @Override
    public @Nullable CompoundTag getPersistentNBT(ItemInfo info) {
        if (!info.hasNBT() || !TaczEmcHelper.isIdentityItem(info.getItem())) {
            return null;
        }
        return TaczEmcHelper.canonicalTag(info.createStack());
    }
}
