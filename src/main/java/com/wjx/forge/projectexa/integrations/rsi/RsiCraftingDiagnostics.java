package com.wjx.forge.projectexa.integrations.rsi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Temporary, bounded, read-only diagnostics. Never probe damage, serialize capabilities or create NBT. */
public final class RsiCraftingDiagnostics {
    private static final Logger LOGGER = LogManager.getLogger("ProjectExA/RSI-Diag");
    private static final boolean ENABLED = !"false".equalsIgnoreCase(
            System.getProperty("projectexa.rsiDiagnostics", "true"));
    private static final Map<String, Sample> SAMPLES = new LinkedHashMap<>(64, .75F, true);
    private static boolean environmentLogged;
    private static boolean errorLogged;

    private RsiCraftingDiagnostics() {}

    public static boolean enabled() {
        return ENABLED;
    }

    /** Planning validation has no player argument and can run before a craft is launched. */
    public static void planning(String phase, String subject, Supplier<String> detail) {
        if (!ENABLED) return;
        try {
            long suppressed = sample("planning/" + phase + "/" + subject);
            if (suppressed < 0) return;
            long dropped = rate("planning");
            if (dropped < 0) return;
            LOGGER.info("[ProjectExA-RSI-Diag] phase={} subject={} thread={} suppressed={} rateDropped={} {}",
                    phase, subject, Thread.currentThread().getName(), suppressed, dropped, detail.get());
        } catch (RuntimeException | LinkageError error) {
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.warn("[ProjectExA-RSI-Diag] planning diagnostic read failed (further errors suppressed)", error);
            }
        }
    }

    public static void log(ServerPlayer player, String phase, String subject, Supplier<String> detail) {
        if (!ENABLED || player == null) return;
        try {
            long suppressed = sample(player.getUUID() + "/" + phase + "/" + subject);
            if (suppressed < 0) return;
            long rateDropped = rate(player.getUUID().toString());
            if (rateDropped < 0) return;
            if (!environmentLogged) {
                environmentLogged = true;
                String versions = ModList.get().getMods().stream()
                        .filter(mod -> switch (mod.getModId()) {
                            case "projectexa", "projecte", "rs_integration", "refinedstorage",
                                    "beyonddimensions", "sophisticatedbackpacks", "lhmiracleroad" -> true;
                            default -> false;
                        }).map(mod -> mod.getModId() + "=" + mod.getVersion()).toList().toString();
                LOGGER.info("[ProjectExA-RSI-Diag] environment versions={} legacyLedger={} interval=5s keyLimit=512 nbt=shape-only",
                        versions, RsiStorageCompat.hasLegacyInventory());
            }
            LOGGER.info("[ProjectExA-RSI-Diag] player={} tick={} phase={} subject={} suppressed={} rateDropped={} {}",
                    player.getUUID(), player.server.getTickCount(), phase, subject, suppressed, rateDropped, detail.get());
        } catch (RuntimeException | LinkageError error) {
            // Diagnostics must not turn a previously working recipe into a failure.
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.warn("[ProjectExA-RSI-Diag] diagnostic read failed (further errors suppressed)", error);
            }
        }
    }

    private static synchronized long sample(String key) {
        long now = System.nanoTime();
        Sample previous = SAMPLES.get(key);
        if (previous != null && now - previous.time < 5_000_000_000L) {
            previous.suppressed++;
            return -1;
        }
        long suppressed = previous == null ? 0 : previous.suppressed;
        if (previous == null && SAMPLES.size() >= 512) {
            SAMPLES.remove(SAMPLES.keySet().iterator().next());
        }
        SAMPLES.put(key, new Sample(now));
        return suppressed;
    }

    private static synchronized long rate(String player) {
        String key = "rate/" + player;
        long now = System.nanoTime();
        Sample window = SAMPLES.get(key);
        if (window == null || now - window.time >= 5_000_000_000L) {
            long dropped = window == null ? 0 : window.suppressed;
            if (window == null && SAMPLES.size() >= 512) SAMPLES.remove(SAMPLES.keySet().iterator().next());
            SAMPLES.put(key, new Sample(now));
            return dropped;
        }
        if (++window.emitted >= 64) { window.suppressed++; return -1; }
        return 0;
    }

    public static String item(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static String stack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        var tag = stack.getTag();
        String shape = tag == null ? "null" : tag.isEmpty() ? "{}"
                : "keys=" + tag.getAllKeys().stream().sorted().limit(12).toList();
        return item(stack) + " x" + stack.getCount() + " tag=" + shape;
    }

    public static String pending(Map<?, ?> pending) {
        // Values/counts only: key.toString() could dump arbitrary player NBT.
        return "keys=" + pending.size() + " counts=" + pending.values().stream().limit(12).toList();
    }

    public static String physical(ServerPlayer player, ItemStack template) {
        StringBuilder result = new StringBuilder();
        int matches = 0;
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack actual = player.getInventory().items.get(slot);
            if (actual.isEmpty() || actual.getItem() != template.getItem()) continue;
            if (++matches > 12) { result.append(" ..."); break; }
            result.append(" slot=").append(slot).append('[').append(stack(actual)).append(']');
        }
        return matches == 0 ? "none-in-main-inventory" : result.toString();
    }

    private static final class Sample {
        private final long time;
        private long suppressed;
        private int emitted;
        private Sample(long time) { this.time = time; }
    }
}
