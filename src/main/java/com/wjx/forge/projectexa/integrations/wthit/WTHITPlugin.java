package com.wjx.forge.projectexa.integrations.wthit;

import com.wjx.forge.projectexa.Main;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public class WTHITPlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(WTHITDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
