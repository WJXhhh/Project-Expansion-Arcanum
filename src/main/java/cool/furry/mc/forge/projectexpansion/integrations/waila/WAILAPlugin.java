package cool.furry.mc.forge.projectexpansion.integrations.waila;

import cool.furry.mc.forge.projectexpansion.Main;
import mcp.mobius.waila.api.IWailaClientRegistration;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import net.minecraft.world.level.block.Block;

@mcp.mobius.waila.api.WailaPlugin(Main.MOD_ID)
public class WAILAPlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registrar) {
        registrar.registerComponentProvider(WAILADataProvider.INSTANCE, TooltipPosition.BODY, Block.class);
    }
}
