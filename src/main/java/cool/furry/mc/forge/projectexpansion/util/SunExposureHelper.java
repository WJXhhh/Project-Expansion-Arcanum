package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.Main;
import moze_intel.projecte.utils.LazyTagLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraftforge.registries.ForgeRegistries;

public class SunExposureHelper {
    public static final TagKey<Item> PROTECTIVE_ITEMS = ItemTags.create(Main.rl("sun_exposure_protection"));
    public static final LazyTagLookup<Item> PROTECTIVE_ITEMS_LOOKUP = LazyTagLookup.create(ForgeRegistries.ITEMS, PROTECTIVE_ITEMS);

    private static boolean automaticProtection(ServerPlayer player) {
        return player.gameMode.isCreative() || player.gameMode.getGameModeForPlayer().equals(GameType.SPECTATOR);
    }

    public static boolean wearingProtectiveBoots(ServerPlayer player) {
        return automaticProtection(player) || PROTECTIVE_ITEMS_LOOKUP.contains(player.getInventory().getArmor(EquipmentSlot.FEET.getIndex()).getItem());
    }

    public static boolean wearingProtectiveLeggings(ServerPlayer player) {
        return automaticProtection(player) || PROTECTIVE_ITEMS_LOOKUP.contains(player.getInventory().getArmor(EquipmentSlot.LEGS.getIndex()).getItem());
    }

    public static boolean wearingProtectiveChestplate(ServerPlayer player) {
        return automaticProtection(player) || PROTECTIVE_ITEMS_LOOKUP.contains(player.getInventory().getArmor(EquipmentSlot.CHEST.getIndex()).getItem());
    }

    public static boolean wearingProtectiveHelmet(ServerPlayer player) {
        return automaticProtection(player) || PROTECTIVE_ITEMS_LOOKUP.contains(player.getInventory().getArmor(EquipmentSlot.HEAD.getIndex()).getItem());
    }

    public static int getProtectionAmount(ServerPlayer player) {
        int protection = 0;
        if (wearingProtectiveBoots(player)) protection++;
        if (wearingProtectiveLeggings(player)) protection++;
        if (wearingProtectiveChestplate(player)) protection++;
        if (wearingProtectiveHelmet(player)) protection++;
        return protection;
    }

    public static boolean wearingAllProtectiveArmor(ServerPlayer player) {
        return getProtectionAmount(player) == 4;
    }

    public static boolean wearingAnyProtectiveArmor(ServerPlayer player) {
        return getProtectionAmount(player) > 0;
    }
}
