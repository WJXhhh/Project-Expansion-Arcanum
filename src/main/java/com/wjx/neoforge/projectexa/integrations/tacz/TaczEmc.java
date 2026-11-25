package com.wjx.neoforge.projectexa.integrations.tacz;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.util.DefaultGunAttachments;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class TaczEmc {
    private TaczEmc() {}

    static ItemStack canonicalStack(ItemStack stack) {
        if (stack.getItem() instanceof IGun gun) {
            ResourceLocation id = gun.getGunId(stack);
            if (DefaultAssets.EMPTY_GUN_ID.equals(id)) return ItemStack.EMPTY;
            return TimelessAPI.getCommonGunIndex(id).map(index -> {
                var modes = index.getGunData().getFireModeSet();
                return GunItemBuilder.create().setId(id)
                        .setFireMode(modes.isEmpty() ? FireMode.UNKNOWN : modes.getFirst())
                        .putAllVirtualAttachment(DefaultGunAttachments.defaults(id))
                        .build();
            }).orElse(ItemStack.EMPTY);
        }
        if (stack.getItem() instanceof IAmmo ammo) {
            ResourceLocation id = ammo.getAmmoId(stack);
            if (!DefaultAssets.EMPTY_AMMO_ID.equals(id)) {
                return AmmoItemBuilder.create().setId(id).build();
            }
        }
        if (stack.getItem() instanceof IAttachment attachment) {
            ResourceLocation id = attachment.getAttachmentId(stack);
            if (!DefaultAssets.isEmptyAttachmentId(id)) {
                return AttachmentItemBuilder.create().setId(id).build();
            }
        }
        return ItemStack.EMPTY;
    }

    static NSSItem normalized(ItemStack stack) {
        ItemStack canonical = canonicalStack(stack);
        return NSSItem.createItem(canonical.isEmpty() ? stack : canonical);
    }
}
