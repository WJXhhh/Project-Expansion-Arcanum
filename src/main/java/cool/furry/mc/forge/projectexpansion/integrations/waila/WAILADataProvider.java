package cool.furry.mc.forge.projectexpansion.integrations.waila;

import cool.furry.mc.forge.projectexpansion.integrations.Common;
import mcp.mobius.waila.api.BlockAccessor;
import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.config.IPluginConfig;

public class WAILADataProvider implements IComponentProvider {

    static final WAILADataProvider INSTANCE = new WAILADataProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, accessor.getBlock(), accessor.getBlockEntity());
    }
}
