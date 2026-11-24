package com.wjx.forge.projectexa.util;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.config.JeiEmcClientState;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketWorldEmcPick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class WorldEmcPickClient {
    private WorldEmcPickClient() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isCanceled() || !event.isPickBlock() || !canPick(mc)
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ItemStack stack = mc.level.getBlockState(hit.getBlockPos()).getCloneItemStack(hit, mc.level, hit.getBlockPos(), mc.player);
        if (requestPick(hit, stack, Screen.hasShiftDown())) event.setCanceled(true);
    }

    private static boolean canPick(Minecraft mc) {
        return JeiEmcClientState.isWorldPickEnabled() && mc.screen == null && mc.player != null && mc.level != null
                && !mc.player.isCreative() && !mc.player.isSpectator()
                && PacketOpenArcaneTransmutationTablet.hasTablet(mc.player);
    }

    public static boolean requestPick(BlockHitResult hit, ItemStack stack, boolean shift) {
        Minecraft mc = Minecraft.getInstance();
        if (stack.isEmpty() || !canPick(mc)) return false;
        ItemStack held = mc.player.getMainHandItem();
        boolean topUp = shift && !held.isEmpty() && held.getCount() < held.getMaxStackSize()
                && ItemStack.isSameItemSameTags(held, stack)
                && ItemStack.isSameItemSameTags(held, Util.cleanStack(stack));
        if (!topUp && mc.player.getInventory().findSlotMatchingItem(stack) >= 0) return false;
        PacketHandler.sendToServer(new PacketWorldEmcPick(hit.getBlockPos(), shift));
        return true;
    }
}
