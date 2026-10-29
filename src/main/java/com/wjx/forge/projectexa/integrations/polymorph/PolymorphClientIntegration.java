package com.wjx.forge.projectexa.integrations.polymorph;

import com.illusivesoulworks.polymorph.api.PolymorphApi;
import com.illusivesoulworks.polymorph.client.recipe.widget.PlayerRecipesWidget;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.gui.GUIArcaneTransmutationTablet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PolymorphClientIntegration {
    private PolymorphClientIntegration() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("polymorph")) event.enqueueWork(PolymorphClientIntegration::register);
    }

    private static void register() {
        PolymorphApi.client().registerWidget(screen -> {
            if (!(screen instanceof GUIArcaneTransmutationTablet tablet)) return null;
            return PolymorphApi.client().findCraftingResultSlot(screen).map(slot ->
                    new PlayerRecipesWidget(screen, slot) {
                        @Override public int getXPos() { return -73; }
                        @Override public int getYPos() { return 81; }
                        @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) {
                            if (tablet.isCraftingPanelVisible()) super.render(graphics, x, y, partialTick);
                        }
                        @Override public boolean mouseClicked(double x, double y, int button) {
                            return tablet.isCraftingPanelVisible() && super.mouseClicked(x, y, button);
                        }
                    }).orElse(null);
        });
    }
}
