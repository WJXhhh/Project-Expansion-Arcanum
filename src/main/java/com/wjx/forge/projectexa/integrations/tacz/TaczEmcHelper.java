package com.wjx.forge.projectexa.integrations.tacz;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Canonical ("freshly crafted") forms of TACZ guns, ammunition and attachments.
 *
 * <p>TACZ stores every gun, cartridge and attachment on a handful of shared items and keeps the real
 * identity in NBT. ProjectE keys its EMC map on item + NBT, so a value can only be looked up again when
 * the NBT is stable. A gun in a player inventory is not stable: ammo count, fire mode, installed
 * attachments, level exp, heat and skins all live in the same tag.
 *
 * <p>Everything here therefore rebuilds the exact stack the gun smith table would produce for an id,
 * mirroring {@code com.tacz.guns.crafting.result.RawGunTableResult}. That form is used both as the EMC
 * mapping key and as the persistent NBT reported to ProjectE, so a transmuted gun is byte for byte the
 * same item as a crafted one.</p>
 */
public final class TaczEmcHelper {

    private TaczEmcHelper() {
    }

    /** Items whose EMC identity lives in NBT. Ammo boxes are deliberately excluded: they carry a count. */
    public static boolean isIdentityItem(Item item) {
        return item instanceof IGun || item instanceof IAmmo || item instanceof IAttachment;
    }

    /**
     * Rebuilds the "as crafted" stack for a TACZ item, dropping every mutable per-stack value.
     * Returns {@link ItemStack#EMPTY} for anything that is not a TACZ gun, ammo or attachment, and for
     * content whose gun pack is not loaded.
     */
    public static ItemStack canonicalStack(ItemStack stack) {
        if (stack.isEmpty() || !isIdentityItem(stack.getItem())) {
            return ItemStack.EMPTY;
        }

        // TACZ's accessors go through ItemStack#getOrCreateTag, so never read from the caller's stack.
        ItemStack copy = stack.copyWithCount(1);
        Item item = copy.getItem();
        if (item instanceof IGun gun) {
            return canonicalGun(gun.getGunId(copy));
        }
        if (item instanceof IAmmo ammo) {
            return canonicalAmmo(ammo.getAmmoId(copy));
        }
        if (item instanceof IAttachment attachment) {
            return canonicalAttachment(attachment.getAttachmentId(copy));
        }
        return ItemStack.EMPTY;
    }

    /** Mirrors {@code RawGunTableResult#getGunStack} for a recipe without extra data. */
    public static ItemStack canonicalGun(@Nullable ResourceLocation gunId) {
        if (gunId == null || DefaultAssets.EMPTY_GUN_ID.equals(gunId)) {
            return ItemStack.EMPTY;
        }

        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        if (index == null) {
            // The gun pack is not loaded (or not synced yet); there is no canonical form to speak of.
            return ItemStack.EMPTY;
        }

        FireMode fireMode = FireMode.UNKNOWN;
        GunData data = index.getGunData();
        if (data != null) {
            List<FireMode> fireModes = data.getFireModeSet();
            if (fireModes != null && !fireModes.isEmpty()) {
                fireMode = fireModes.get(0);
            }
        }

        return GunItemBuilder.create()
                .setId(gunId)
                .setFireMode(fireMode)
                .setAmmoCount(0)
                .setAmmoInBarrel(false)
                .build();
    }

    public static ItemStack canonicalAmmo(@Nullable ResourceLocation ammoId) {
        if (ammoId == null || DefaultAssets.EMPTY_AMMO_ID.equals(ammoId)) {
            return ItemStack.EMPTY;
        }
        return AmmoItemBuilder.create().setId(ammoId).build();
    }

    public static ItemStack canonicalAttachment(@Nullable ResourceLocation attachmentId) {
        if (attachmentId == null || DefaultAssets.isEmptyAttachmentId(attachmentId)) {
            return ItemStack.EMPTY;
        }
        return AttachmentItemBuilder.create().setId(attachmentId).build();
    }

    /** The canonical NBT of a TACZ stack, or null when the stack has no canonical form. */
    public static @Nullable CompoundTag canonicalTag(ItemStack stack) {
        ItemStack canonical = canonicalStack(stack);
        if (canonical.isEmpty()) {
            return null;
        }
        CompoundTag tag = canonical.getTag();
        return tag == null || tag.isEmpty() ? null : tag;
    }

    /**
     * The normalized stack ProjectE should use for a recipe input or output: the canonical TACZ form when
     * there is one, otherwise plain ProjectE behaviour.
     */
    public static NormalizedSimpleStack toNormalizedStack(ItemStack stack) {
        CompoundTag canonical = canonicalTag(stack);
        return canonical == null ? NSSItem.createItem(stack) : NSSItem.createItem(stack.getItem(), canonical);
    }
}
