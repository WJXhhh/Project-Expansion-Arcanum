package com.wjx.forge.projectexa.integrations.top;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.integrations.Common;
import mcjty.theoneprobe.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

@SuppressWarnings("unused")
public class ProbeInfoProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {
    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level, BlockState blockState, IProbeHitData data) {
        Common.registerCommonTooltips(probeInfo::mcText, blockState.getBlock(), level.getBlockEntity(data.getPos()));
    }

    @Override
    public ResourceLocation getID() {
        return Main.rl("plugin");
    }

    @Override
    public Void apply(ITheOneProbe iTheOneProbe) {
        iTheOneProbe.registerProvider(this);
        return null;
    }
}