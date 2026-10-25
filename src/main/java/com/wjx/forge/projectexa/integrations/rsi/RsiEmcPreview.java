package com.wjx.forge.projectexa.integrations.rsi;

import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult;
import net.minecraft.world.item.ItemStack;

/** Implemented only by RSI's plan screen; safe to reference when RSI is absent. */
public interface RsiEmcPreview {
    void projectexa$acceptQuote(PacketRsiEmcEstimateResult quote);

    String projectexa$emcSuffix(ItemStack stack);

    String projectexa$emcTotal();

    int projectexa$emcColor();
}
