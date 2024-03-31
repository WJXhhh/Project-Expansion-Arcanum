package cool.furry.mc.forge.projectexpansion.integrations.wthit;

import cool.furry.mc.forge.projectexpansion.integrations.Common;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;

public class WTHITDataProvider implements IBlockComponentProvider {

    static final WTHITDataProvider INSTANCE = new WTHITDataProvider();

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::addLine, accessor.getBlock(), accessor.getBlockEntity());
    }
}
