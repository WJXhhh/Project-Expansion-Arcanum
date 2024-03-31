package cool.furry.mc.forge.projectexpansion.integrations.jade;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.integrations.Common;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class JadeDataProvider implements IBlockComponentProvider {
    static final JadeDataProvider INSTANCE = new JadeDataProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        Common.registerCommonTooltips(tooltip::add, accessor.getBlock(), accessor.getBlockEntity());
    }

    @Override
    public ResourceLocation getUid() {
        return Main.rl("provider");
    }
}
