package com.wjx.neoforge.projectexa.config;

import com.wjx.neoforge.projectexa.Main;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Client input remains untouched until the server has supplied its setting. */
@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class JeiEmcClientState {
    private static volatile boolean enabled;
    private static volatile boolean worldPickEnabled;
    private static volatile boolean largeRecipeTransferCandidatesAllowed;

    private JeiEmcClientState() { }
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isWorldPickEnabled() { return worldPickEnabled; }
    public static void setWorldPickEnabled(boolean value) { worldPickEnabled = value; }
    public static boolean allowsLargeRecipeTransferCandidates() { return largeRecipeTransferCandidatesAllowed; }
    public static void setAllowLargeRecipeTransferCandidates(boolean value) { largeRecipeTransferCandidatesAllowed = value; }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        enabled = false;
        worldPickEnabled = false;
        largeRecipeTransferCandidatesAllowed = false;
    }
}
