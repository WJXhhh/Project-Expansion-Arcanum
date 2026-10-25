package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.plan.PlanResponse;
import com.huanghuang.rsintegration.crafting.tree.IngredientKey;
import com.huanghuang.rsintegration.storage.StorageReferenceCodec;
import com.wjx.forge.projectexa.integrations.rsi.RsiEmcPreview;
import com.wjx.forge.projectexa.integrations.rsi.RsiEmcPlanInputs;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_client.PacketRsiEmcEstimateResult;
import com.wjx.forge.projectexa.net.packets.to_server.PacketRsiEmcEstimate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.plan.CraftingPlanScreen", remap = false)
public abstract class RsiCraftingPlanScreenMixin extends Screen implements RsiEmcPreview {
    @Shadow private PlanResponse plan;
    @Shadow private int contentLayoutWidth;
    @Shadow private int missingAreaHeight;
    @Unique private static final AtomicLong projectexa$quoteIds = new AtomicLong();
    @Unique private long projectexa$quoteId;
    @Unique private int projectexa$quoteTick;
    @Unique private PacketRsiEmcEstimateResult projectexa$quote;
    @Unique private Map<IngredientKey, String> projectexa$suffixes = Map.of();

    protected RsiCraftingPlanScreenMixin(Component title) { super(title); }

    @Inject(method = {"init", "m_7856_", "updatePlan"}, at = @At("TAIL"))
    private void projectexa$requestNewPlanQuote(CallbackInfo ci) {
        projectexa$quote = null;
        projectexa$suffixes = Map.of();
        contentLayoutWidth = -1;
        // updatePlan may also call init. Coalesce those notifications into one request next tick.
        projectexa$quoteId = projectexa$quoteIds.incrementAndGet();
        projectexa$quoteTick = 20;
    }

    @Inject(method = {"tick", "m_86600_"}, at = @At("TAIL"))
    private void projectexa$refreshQuote(CallbackInfo ci) {
        if (++projectexa$quoteTick >= 20) projectexa$requestQuote();
    }

    @Unique
    private void projectexa$requestQuote() {
        if (plan == null || minecraft == null || minecraft.getConnection() == null) return;
        projectexa$quoteTick = 0;
        projectexa$quoteId = projectexa$quoteIds.incrementAndGet();
        Map<IngredientKey, Integer> initial = RsiEmcPlanInputs.collect(plan);
        boolean supported = initial != null;
        List<PacketRsiEmcEstimate.Demand> demands = supported ? initial.entrySet().stream()
                .map(entry -> new PacketRsiEmcEstimate.Demand(entry.getKey().stack(1), entry.getValue())).toList()
                : List.of();
        PacketHandler.sendToServer(new PacketRsiEmcEstimate(projectexa$quoteId,
                plan.storageReference() == null ? null : StorageReferenceCodec.encode(plan.storageReference()),
                supported, demands));
    }

    @Override
    public void projectexa$acceptQuote(PacketRsiEmcEstimateResult quote) {
        if (quote.requestId() != projectexa$quoteId) return; // Old plan, repeat count, network or screen.
        projectexa$quote = quote;
        Map<IngredientKey, String> suffixes = new HashMap<>();
        if (quote.state() == PacketRsiEmcEstimateResult.READY) {
            for (var cost : quote.costs()) suffixes.put(IngredientKey.of(cost.stack()),
                    Component.translatable("gui.projectexa.rsi.emc_material", projectexa$number(cost.count()),
                            projectexa$number(cost.emc())).getString());
        }
        projectexa$suffixes = Map.copyOf(suffixes);
        contentLayoutWidth = -1;
    }

    @Override
    public String projectexa$emcSuffix(ItemStack stack) {
        return projectexa$suffixes.getOrDefault(IngredientKey.of(stack), "");
    }

    @Override
    public String projectexa$emcTotal() {
        if (projectexa$quote == null || projectexa$quote.state() == PacketRsiEmcEstimateResult.HIDDEN) return "";
        if (projectexa$quote.state() >= PacketRsiEmcEstimateResult.UNAVAILABLE) {
            String reason = switch (projectexa$quote.state()) {
                case PacketRsiEmcEstimateResult.INPUTS_UNAVAILABLE -> ".plan";
                case PacketRsiEmcEstimateResult.NETWORK_UNAVAILABLE -> ".network";
                case PacketRsiEmcEstimateResult.PERMISSION_DENIED -> ".permission";
                case PacketRsiEmcEstimateResult.INVENTORY_UNAVAILABLE -> ".inventory";
                default -> "";
            };
            return Component.translatable("gui.projectexa.rsi.emc_unavailable" + reason).getString();
        }
        BigInteger total = projectexa$totalCost();
        String key = total.compareTo(projectexa$quote.balance()) > 0
                ? "gui.projectexa.rsi.emc_total_insufficient" : "gui.projectexa.rsi.emc_total";
        return Component.translatable(key, projectexa$number(total)).getString();
    }

    @Unique
    private BigInteger projectexa$totalCost() {
        return projectexa$quote.costs().stream().map(PacketRsiEmcEstimateResult.Cost::emc)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    @Unique
    private static String projectexa$number(Object value) { return String.format(Locale.ROOT, "%,d", value); }

    @Override
    public int projectexa$emcColor() {
        return projectexa$quote != null && projectexa$quote.state() == PacketRsiEmcEstimateResult.READY
                && projectexa$totalCost().compareTo(projectexa$quote.balance()) <= 0 ? 0xFF99D9B0 : 0xFFFFBB66;
    }

    @Redirect(method = "layoutContentSections", at = @At(value = "FIELD",
            target = "Lcom/huanghuang/rsintegration/crafting/plan/CraftingPlanScreen;missingAreaHeight:I",
            opcode = Opcodes.PUTFIELD))
    private void projectexa$reserveQuoteHeader(@Coerce Object screen, int height) {
        // Pad the bottom of the preceding section, leaving native material scrolling/hitboxes unchanged.
        missingAreaHeight = height + (projectexa$emcTotal().isEmpty() ? 0 : font.lineHeight + 4);
    }

    @Inject(method = "stripCountText", at = @At("RETURN"), cancellable = true)
    private static void projectexa$annotateTreeMaterial(@Coerce Object entry, boolean leftover,
                                                       CallbackInfoReturnable<String> cir) {
        if (!leftover && net.minecraft.client.Minecraft.getInstance().screen instanceof RsiEmcPreview preview) {
            cir.setReturnValue(cir.getReturnValue() + preview.projectexa$emcSuffix(
                    ((RsiStripEntryAccessor) entry).projectexa$key().stack(1)));
        }
    }

    @Inject(method = "stripRowCount", at = @At("RETURN"), cancellable = true)
    private void projectexa$reserveTreeHeader(CallbackInfoReturnable<Integer> cir) {
        if (!plan.materials().isEmpty() && !projectexa$emcTotal().isEmpty()) cir.setReturnValue(cir.getReturnValue() + 1);
    }

    @Inject(method = "renderStripRow", at = @At("HEAD"))
    private void projectexa$drawTreeQuote(GuiGraphics graphics, Font font, int centerX, int y, String label,
                                        int color, List<?> entries, boolean leftover, int mouseX, int mouseY,
                                        CallbackInfo ci) {
        if (leftover || projectexa$emcTotal().isEmpty()) return;
        int width = font.width(label) + 8;
        for (Object entry : entries) {
            RsiStripEntryAccessor material = (RsiStripEntryAccessor) entry;
            String count = material.projectexa$available() + "/" + material.projectexa$count()
                    + projectexa$emcSuffix(material.projectexa$key().stack(1));
            width += 18 + font.width(count) + 6;
        }
        graphics.drawString(font, projectexa$emcTotal(), centerX - width / 2,
                y - font.lineHeight - 4, projectexa$emcColor());
    }
}
