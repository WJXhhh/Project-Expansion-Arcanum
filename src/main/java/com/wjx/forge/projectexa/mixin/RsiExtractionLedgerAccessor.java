package com.wjx.forge.projectexa.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.ExtractionLedger", remap = false)
public interface RsiExtractionLedgerAccessor {
    @Invoker("findAllBackpackInventories")
    static List<IItemHandler> projectexa$backpacks(ServerPlayer player) {
        throw new AssertionError();
    }
}
