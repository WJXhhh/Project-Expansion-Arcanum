package com.wjx.forge.projectexa.integrations.rsi;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

import java.util.Arrays;
import java.util.List;

/** Scoped to the validator's existing comparisons; never repeats a match or queries item damage. */
public final class RsiPlanningDiagnostics {
    private static final ThreadLocal<Probe> ACTIVE = new ThreadLocal<>();

    private RsiPlanningDiagnostics() {}

    public static Probe begin(Object graph, Ingredient ingredient, Item item, String tag) {
        if (!RsiCraftingDiagnostics.enabled()) return null;
        try {
            String material = BuiltInRegistries.ITEM.getKey(item).toString();
            Probe probe = new Probe(ACTIVE.get(), graph, ingredient, material,
                    tag == null ? "null" : tag.equals("{}") ? "{}" : "present(length=" + tag.length() + ")");
            ACTIVE.set(probe);
            return probe;
        } catch (RuntimeException | LinkageError error) {
            readError(error);
            return null;
        }
    }

    public static void beforeMatch(Ingredient ingredient, ItemStack actual) {
        Probe probe = ACTIVE.get();
        if (probe == null || probe.ingredient != ingredient || probe.captured) return;
        try {
            probe.captured = true;
            probe.actual = actual;
            probe.actualBefore = RsiCraftingDiagnostics.stack(actual);
            // The planner creates these built-in exact ingredients. Do not run getItems()
            // on arbitrary custom ingredients purely for a diagnostic read.
            if (ingredient instanceof StrictNBTIngredient || ingredient instanceof CompoundIngredient) {
                ItemStack[] templates = ingredient.getItems();
                probe.templateCount = templates.length;
                probe.templates = Arrays.copyOf(templates, Math.min(4, templates.length));
                probe.expectedBefore = describe(probe.templates);
            }
        } catch (RuntimeException | LinkageError error) {
            readError(error);
        }
    }

    public static void afterMatch(Ingredient ingredient, ItemStack actual, boolean matched) {
        Probe probe = ACTIVE.get();
        if (probe != null && probe.ingredient == ingredient && probe.actual == actual) {
            probe.innerResult = matched;
        }
    }

    public static void beforeIngredientTest(Ingredient ingredient, ItemStack actual) {
        Probe probe = ACTIVE.get();
        if (probe == null || probe.ingredient != ingredient || probe.actual != actual) return;
        try {
            // Snapshot at the actual strict comparison, before any later fallback can
            // query actual.getMaxDamage() and hide a transient null/{} difference.
            probe.expectedAtTest = describe(probe.templates);
            probe.actualAtTest = RsiCraftingDiagnostics.stack(actual);
        } catch (RuntimeException | LinkageError error) {
            readError(error);
        }
    }

    public static void complete(Probe probe, boolean matched, Throwable failure) {
        if (probe == null) return;
        try {
            String expectedAfter = describe(probe.templates);
            String actualAfter = probe.captured ? RsiCraftingDiagnostics.stack(probe.actual) : "not-captured";
            boolean expectedChanged = !probe.expectedBefore.equals(expectedAfter);
            boolean actualChanged = !probe.actualBefore.equals(actualAfter);
            String phase = failure != null ? "planning-match-threw" : !matched ? "planning-match-failed"
                    : expectedChanged || actualChanged ? "planning-match-mutated" : "planning-match-ok";
            RsiCraftingDiagnostics.planning(phase, probe.material + "/" + probe.ingredient.getClass().getSimpleName(),
                    () -> "graph=" + probe.graph + " ingredient=" + probe.ingredient.getClass().getName()
                            + " material=" + probe.material + " materialTag=" + probe.materialTag
                            + " captured=" + probe.captured + " templateCount=" + probe.templateCount
                            + " expectedBefore=" + probe.expectedBefore + " expectedAfter=" + expectedAfter
                            + " expectedAtTest=" + probe.expectedAtTest + " actualAtTest=[" + probe.actualAtTest + "]"
                            + " actualBefore=[" + probe.actualBefore + "] actualAfter=[" + actualAfter + "]"
                            + " expectedChanged=" + expectedChanged + " actualChanged=" + actualChanged
                            + " innerResult=" + probe.innerResult + " finalResult=" + matched
                            + " exception=" + (failure == null ? "none" : failure.getClass().getName()));
        } catch (RuntimeException | LinkageError error) {
            readError(error);
        }
    }

    public static void close(Probe probe) {
        if (probe == null) return;
        if (probe.previous == null) ACTIVE.remove();
        else ACTIVE.set(probe.previous);
    }

    public static void validation(Object graph, List<String> errors) {
        RsiCraftingDiagnostics.planning(errors.isEmpty() ? "planning-validation-ok" : "planning-validation-failed",
                "validation", () -> "graph=" + Integer.toHexString(System.identityHashCode(graph))
                        + " errorCount=" + errors.size() + " errors=" + errors.stream().limit(12)
                                .map(error -> error.length() > 256 ? error.substring(0, 256) + "..." : error).toList());
    }

    private static String describe(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(RsiCraftingDiagnostics::stack).toList().toString();
    }

    private static void readError(Throwable error) {
        RsiCraftingDiagnostics.planning("planning-probe-read-error", error.getClass().getName(),
                () -> "error=" + error.getClass().getName());
    }

    public static final class Probe {
        private final Probe previous;
        private final String graph;
        private final Ingredient ingredient;
        private final String material;
        private final String materialTag;
        private boolean captured;
        private ItemStack actual;
        private ItemStack[] templates = new ItemStack[0];
        private int templateCount;
        private String expectedBefore = "[]";
        private String actualBefore = "not-captured";
        private String expectedAtTest = "not-captured";
        private String actualAtTest = "not-captured";
        private Boolean innerResult;

        private Probe(Probe previous, Object graph, Ingredient ingredient, String material, String materialTag) {
            this.previous = previous;
            this.graph = Integer.toHexString(System.identityHashCode(graph));
            this.ingredient = ingredient;
            this.material = material;
            this.materialTag = materialTag;
        }
    }
}
