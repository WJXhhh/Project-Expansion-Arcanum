package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.CraftingResolver.ResolutionStep;
import com.huanghuang.rsintegration.crafting.graph.DagScheduler;
import com.huanghuang.rsintegration.crafting.graph.NodeAdmissionCoordinator;
import com.huanghuang.rsintegration.crafting.graph.NodeId;
import com.huanghuang.rsintegration.util.CraftLogContext;
import com.wjx.forge.projectexa.integrations.rsi.RsiCraftingDiagnostics;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.UUID;

/** Observation only: no cancelled injections, redirects, claims, retries or scheduler mutations. */
@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.AsyncCraftChain", remap = false)
public abstract class RsiAsyncCraftDiagnosticsMixin {
    @Shadow @Final private UUID craftId;
    @Shadow @Final private CraftLogContext ctx;
    @Shadow @Final private DagScheduler graphScheduler;
    @Shadow @Final private Map<?, ?> graphVanillaJobs;
    @Shadow @Final private Map<?, ?> graphFailureDetails;
    @Shadow @Final private Map<?, ?> graphRetryUntilTick;
    @Shadow private boolean waitingForVanillaBudget;

    @Inject(method = "tickGraph", at = @At("RETURN"), require = 0)
    private void projectexa$diagnoseGraph(ServerPlayer online, @Coerce Object allowance, CallbackInfoReturnable<Boolean> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(online)) return;
        RsiCraftingDiagnostics.log(online, "graph-tick-" + cir.getReturnValue(), craftId.toString(),
                () -> "craftId=" + craftId + " op=" + ctx.opId() + " recipe=" + ctx.recipeId()
                        + " scheduler=" + (graphScheduler == null ? "null" : graphScheduler.stateSnapshot()
                                .entrySet().stream().limit(12).toList())
                        + " activeVanillaJobs=" + graphVanillaJobs.size() + " failures=" + graphFailureDetails.size()
                        + " retryUntil=" + graphRetryUntilTick.entrySet().stream().limit(12).toList()
                        + " waitingForBudget=" + waitingForVanillaBudget);
    }

    @Inject(method = "beginGraphVanillaJob", at = @At("RETURN"), require = 0)
    private void projectexa$diagnoseVanillaStart(NodeId node, ResolutionStep step,
            NodeAdmissionCoordinator.Admission admission, int executions, ServerPlayer online,
            CallbackInfoReturnable<Object> cir) {
        if (!PacketOpenArcaneTransmutationTablet.hasTablet(online)) return;
        RsiCraftingDiagnostics.log(online, cir.getReturnValue() == null ? "graph-job-not-started" : "graph-job-started",
                craftId + "/" + node, () -> "craftId=" + craftId + " op=" + ctx.opId()
                        + " recipe=" + ctx.recipeId() + " node=" + node + " executions=" + executions);
    }
}
