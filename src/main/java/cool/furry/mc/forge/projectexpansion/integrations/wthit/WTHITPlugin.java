package cool.furry.mc.forge.projectexpansion.integrations.wthit;

import cool.furry.mc.forge.projectexpansion.Main;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.world.level.block.Block;

public class WTHITPlugin implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        registrar.addComponent(WTHITDataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
