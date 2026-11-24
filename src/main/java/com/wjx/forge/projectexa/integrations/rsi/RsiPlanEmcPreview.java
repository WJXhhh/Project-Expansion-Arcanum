package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.IntegrationResourceKey;
import com.huanghuang.rsintegration.api.RSIPlanExtensions;
import com.huanghuang.rsintegration.api.RSIPlanExtensions.Context;
import com.huanghuang.rsintegration.api.RSIPlanExtensions.Decoration;
import com.huanghuang.rsintegration.api.RSIPlanExtensions.MaterialNote;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate.Allocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class RsiPlanEmcPreview {
    private static Preview active;
    private static long requestId;
    private static int refreshTicks;

    private RsiPlanEmcPreview() {}

    public static void register() {
        RSIPlanExtensions.register(Main.rl("emc_preview"), RsiPlanEmcPreview::onPlan);
        MinecraftForge.EVENT_BUS.addListener(RsiPlanEmcPreview::onClientTick);
    }

    private static void onPlan(Context context, Consumer<Decoration> publish) {
        active = new Preview(Minecraft.getInstance().screen, collectAllocations(context), publish);
        requestQuote();
    }

    private static List<Allocation> collectAllocations(Context context) {
        if (!context.complete()) return null;
        List<Allocation> allocations = new ArrayList<>();
        for (var source : context.sourceAllocations()) {
            if (!source.sourceId().equals(RsiEmcMaterialSource.ID)) continue;
            var stack = RsiItemIdentity.toStack(source.key());
            if (stack.isEmpty() || source.count() <= 0
                    || allocations.size() >= PacketRsiEmcEstimate.MAX_MATERIALS) return null;
            allocations.add(new Allocation(stack, source.count()));
        }
        return List.copyOf(allocations);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != active.screen() || minecraft.getConnection() == null) {
            active = null;
        } else if (++refreshTicks >= 20) {
            requestQuote();
        }
    }

    private static void requestQuote() {
        refreshTicks = 0;
        long id = ++requestId;
        if (Minecraft.getInstance().getConnection() == null) return;
        PacketHandler.sendToServer(new PacketRsiEmcEstimate(id, active.complete(),
                active.complete() ? active.allocations() : List.of()));
    }

    public static void acceptQuote(PacketRsiEmcEstimateResult quote) {
        if (active == null || active.screen() != Minecraft.getInstance().screen || quote.requestId() != requestId) return;
        if (quote.state() == PacketRsiEmcEstimateResult.HIDDEN) {
            active.publish().accept(new Decoration(List.of(), Map.of()));
            return;
        }
        if (quote.state() != PacketRsiEmcEstimateResult.READY) {
            String reason = quote.state() == PacketRsiEmcEstimateResult.INPUTS_UNAVAILABLE ? ".plan" : "";
            String summary = Component.translatable("gui.projectexa.rsi.emc_unavailable" + reason).getString();
            active.publish().accept(new Decoration(List.of(summary), Map.of()));
            return;
        }

        Map<IntegrationResourceKey, MaterialNote> notes = new HashMap<>();
        BigInteger total = BigInteger.ZERO;
        for (var cost : quote.costs()) {
            String note = Component.translatable("gui.projectexa.rsi.emc_material",
                    number(cost.count()), number(cost.emc())).getString();
            notes.put(IntegrationResourceKey.item(cost.stack()), new MaterialNote(note, List.of()));
            total = total.add(cost.emc());
        }
        String key = total.compareTo(quote.balance()) > 0
                ? "gui.projectexa.rsi.emc_total_insufficient" : "gui.projectexa.rsi.emc_total";
        String summary = Component.translatable(key, number(total)).getString();
        active.publish().accept(new Decoration(List.of(summary), notes));
    }

    private static String number(Object value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private record Preview(Screen screen, List<Allocation> allocations, Consumer<Decoration> publish) {
        private boolean complete() {
            return allocations != null;
        }
    }
}
