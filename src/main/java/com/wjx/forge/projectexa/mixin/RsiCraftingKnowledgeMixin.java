package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.AsyncCraftChain;
import com.huanghuang.rsintegration.crafting.graph.MaterialBroker;
import com.huanghuang.rsintegration.crafting.graph.MaterialKey;
import com.huanghuang.rsintegration.crafting.graph.MaterialSource;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.wjx.forge.projectexa.integrations.rsi.RsiCraftingKnowledge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.AsyncCraftChain", remap = false)
public abstract class RsiCraftingKnowledgeMixin {
    @Shadow @Final private UUID playerId;

    @Shadow
    public abstract AsyncCraftChain.State state();

    @Shadow
    private ServerPlayer resolvePlayer() {
        throw new AssertionError();
    }

    @Unique
    private RsiCraftingKnowledge projectexa$knowledge;

    @WrapMethod(method = "tick(Lcom/huanghuang/rsintegration/crafting/VanillaCraftingTickBudget$ChainAllowance;"
            + "Ljava/util/function/BooleanSupplier;)Z", remap = false)
    private boolean projectexa$captureCraftedProducts(@Coerce Object allowance, BooleanSupplier dispatchAllowed,
            Operation<Boolean> original) {
        if (projectexa$knowledge == null) projectexa$knowledge = new RsiCraftingKnowledge(playerId);
        try {
            return projectexa$knowledge.capture(() -> original.call(allowance, dispatchAllowed));
        } finally {
            if (state() == AsyncCraftChain.State.ABORTED) projectexa$knowledge.discard();
        }
    }

    // Graph execution publishes actual outputs before later nodes can consume them.
    @WrapOperation(method = {"publishIncrementalGraphOutputs", "publishDeclaredNodeOutputs"},
            at = @At(value = "INVOKE", target = "Lcom/huanghuang/rsintegration/crafting/graph/MaterialBroker;"
                    + "publishActual(Lcom/huanghuang/rsintegration/crafting/graph/MaterialSource;"
                    + "Lcom/huanghuang/rsintegration/crafting/graph/MaterialKey;"
                    + "Lnet/minecraft/world/item/ItemStack;)J"), remap = false)
    private long projectexa$rememberGraphProducts(MaterialBroker broker, MaterialSource source,
            MaterialKey material, ItemStack stack, Operation<Long> original) {
        RsiCraftingKnowledge.recordCrafted(resolvePlayer(), stack);
        return original.call(broker, source, material, stack);
    }

    // Only this tick overload contains the legacy machine-step output collection.
    @WrapOperation(method = "tick(Lcom/huanghuang/rsintegration/crafting/VanillaCraftingTickBudget$ChainAllowance;"
            + "Ljava/util/function/BooleanSupplier;)Z", at = @At(value = "INVOKE",
            target = "Lcom/huanghuang/rsintegration/crafting/AsyncCraftChain$OutputCollection;stacks()Ljava/util/List;"),
            remap = false)
    private List<ItemStack> projectexa$rememberCollectedProducts(@Coerce Object collection,
            Operation<List<ItemStack>> original) {
        List<ItemStack> stacks = original.call(collection);
        ServerPlayer player = resolvePlayer();
        for (ItemStack stack : stacks) RsiCraftingKnowledge.recordCrafted(player, stack);
        return stacks;
    }

    // This call occurs only after the ledger commit and final output settlement succeed.
    // Learn before onDone listeners can start a repeated craft.
    @Inject(method = "finish", at = @At(value = "INVOKE",
            target = "Lcom/huanghuang/rsintegration/crafting/AsyncCraftChain;fireOnDone()V"))
    private void projectexa$learnSuccessfulCraft(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (projectexa$knowledge != null) projectexa$knowledge.complete(player);
    }
}
