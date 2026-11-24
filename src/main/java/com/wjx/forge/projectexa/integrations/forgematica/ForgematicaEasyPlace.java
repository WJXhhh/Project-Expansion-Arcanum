package com.wjx.forge.projectexa.integrations.forgematica;

import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketForgematicaTransmute;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class ForgematicaEasyPlace {
    public static final ConfigBoolean AUTO_TRANSMUTE = new ConfigBoolean(
            "easyPlaceArcaneTransmute", false,
            "Transmute missing blocks using the Arcane Transmutation Tablet during Easy Place.",
            "config.projectexa.easy_place_arcane_transmute.name");
    private static BlockPos pendingPosition;
    private static long lastRequest;

    private ForgematicaEasyPlace() {}

    public static boolean request(ItemStack stack, BlockPos pos, Minecraft mc) {
        if (!AUTO_TRANSMUTE.getBooleanValue() || !Configs.Generic.EASY_PLACE_MODE.getBooleanValue()
                || mc.player == null || mc.player.isCreative() || stack.isEmpty()
                || !PacketOpenArcaneTransmutationTablet.hasTablet(mc.player)
                || EntityUtils.getUsedHandForItem(mc.player, stack) != null
                || mc.player.getInventory().findSlotMatchingItem(stack) != -1) return false;
        long now = System.currentTimeMillis();
        if (pendingPosition != null && now - lastRequest < 5000) return pos.equals(pendingPosition);
        if (now - lastRequest < 1000) return false;

        pendingPosition = pos.immutable();
        lastRequest = now;
        PacketHandler.sendToServer(new PacketForgematicaTransmute(
                List.of(new PacketForgematicaTransmute.Demand(stack.copyWithCount(1), 1, 1)), true));
        return true;
    }

    public static void onResult(boolean success) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos position = pendingPosition;
        pendingPosition = null;
        if (!success) return;
        lastRequest = 0;
        if (position == null || mc.player == null || mc.level == null || mc.screen != null
                || !AUTO_TRANSMUTE.getBooleanValue() || !Configs.Generic.EASY_PLACE_MODE.getBooleanValue()) return;
        double range = Configs.Generic.EASY_PLACE_VANILLA_REACH.getBooleanValue() ? 4.5 : 6.0;
        RayTraceUtils.RayTraceWrapper trace = Configs.Generic.EASY_PLACE_FIRST.getBooleanValue()
                ? RayTraceUtils.getGenericTrace(mc.level, mc.player, range, true,
                        Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue(), false)
                : RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.level, mc.player, range);
        if (trace != null && trace.getHitType() == RayTraceUtils.RayTraceWrapper.HitType.SCHEMATIC_BLOCK
                && position.equals(trace.getBlockHitResult().getBlockPos())) {
            WorldUtils.handleEasyPlace(mc);
        }
    }
}
