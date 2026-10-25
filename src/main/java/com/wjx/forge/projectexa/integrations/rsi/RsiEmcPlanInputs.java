package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.crafting.graph.DemandRole;
import com.huanghuang.rsintegration.crafting.plan.PlanResponse;
import com.huanghuang.rsintegration.crafting.plan.PlanStep;
import com.huanghuang.rsintegration.crafting.tree.IngredientKey;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** Collects initial stock demands from either of RSI's graph or legacy-step preview formats. */
public final class RsiEmcPlanInputs {
    private RsiEmcPlanInputs() {}

    /** A null result means the response cannot describe its initial stock unambiguously. */
    public static Map<IngredientKey, Integer> collect(PlanResponse plan) {
        Map<IngredientKey, Long> initial = new LinkedHashMap<>();
        if (plan.graph() != null) {
            for (var edge : plan.graph().edges()) {
                if (edge.source().initial()) add(initial, edge.material(), edge.quantity());
            }
            for (var root : plan.graph().roots()) {
                for (var edge : root.allocations()) {
                    if (edge.source().initial()) add(initial, edge.material(), edge.quantity());
                }
            }
        } else if (!fromLegacySteps(plan, initial)) {
            return null;
        }
        if (initial.size() > PacketRsiEmcEstimate.MAX_MATERIALS) return null;
        Map<IngredientKey, Integer> result = new LinkedHashMap<>();
        for (var entry : initial.entrySet()) {
            if (entry.getValue() > Integer.MAX_VALUE) return null;
            if (entry.getValue() > 0) result.put(entry.getKey(), entry.getValue().intValue());
        }
        return result;
    }

    private static boolean fromLegacySteps(PlanResponse plan, Map<IngredientKey, Long> initial) {
        if (!plan.success() || plan.steps().size() > PacketRsiEmcEstimate.MAX_MATERIALS) return false;
        Map<IngredientKey, Long> produced = new LinkedHashMap<>();
        Map<IngredientKey, Long> future = new LinkedHashMap<>();
        Map<IngredientKey, Long> grossInputs = new LinkedHashMap<>();
        for (PlanStep step : plan.steps()) add(future, step.output(), outputCount(step));

        // RSI sends legacy steps in execution order. Consume earlier outputs BEFORE charging
        // initial stock. Subtracting all outputs globally would incorrectly erase a cycle's seed.
        for (PlanStep step : plan.steps()) {
            if (step.output().isEmpty() || step.batches() <= 0) return false;
            IngredientKey output = IngredientKey.of(step.output());
            future.merge(output, -outputCount(step), Long::sum);
            Map<IngredientKey, Long> consumed = new LinkedHashMap<>();
            Map<IngredientKey, Long> catalysts = new LinkedHashMap<>();
            for (int slot = 0; slot < step.inputs().size(); slot++) {
                ItemStack input = step.inputs().get(slot);
                if (input.isEmpty()) continue;
                IngredientKey key = IngredientKey.of(input);
                // Self-amplifying and container-returning recipes need graph source/return edges.
                if (key.equals(output) || step.inputRole(slot) == DemandRole.CONTAINER_RETURNING) return false;
                int count = step.totalInputCount(slot, step.batches());
                add(grossInputs, input, count);
                add(step.inputRole(slot) == DemandRole.CATALYST ? catalysts : consumed, input, count);
            }
            for (var input : consumed.entrySet()) {
                if (!allocate(input.getKey(), input.getValue(), false, produced, future, initial)) return false;
            }
            for (var catalyst : catalysts.entrySet()) {
                if (!allocate(catalyst.getKey(), catalyst.getValue(), true, produced, future, initial)) return false;
            }
            add(produced, step.output(), outputCount(step));
        }
        // Some delegates append direct materials (e.g. fuel) outside their displayed step inputs.
        // Account for those too, but do not guess an unrepresented producer/consumer relationship.
        for (var entry : plan.materials().entrySet()) {
            long extra = (long) entry.getValue().needed() - grossInputs.getOrDefault(entry.getKey(), 0L);
            if (extra <= 0) continue;
            if (future.containsKey(entry.getKey())) return false;
            initial.merge(entry.getKey(), extra, Long::sum);
        }
        return true;
    }

    private static boolean allocate(IngredientKey key, long count, boolean reusable,
                                    Map<IngredientKey, Long> produced, Map<IngredientKey, Long> future,
                                    Map<IngredientKey, Long> initial) {
        long stock = produced.getOrDefault(key, 0L);
        long used = Math.min(stock, count);
        long missing = count - used;
        if (missing > 0) {
            // An output of a later step cannot fund an earlier input. Do not overcharge a reordered plan.
            if (future.getOrDefault(key, 0L) > 0) return false;
            initial.merge(key, missing, Long::sum);
        }
        produced.put(key, reusable ? Math.max(stock, count) : stock - used);
        return true;
    }

    private static long outputCount(PlanStep step) {
        return (long) step.output().getCount() * Math.max(0, step.batches());
    }

    private static void add(Map<IngredientKey, Long> counts, ItemStack stack, long count) {
        if (!stack.isEmpty() && count > 0) counts.merge(IngredientKey.of(stack), count, Long::sum);
    }
}
