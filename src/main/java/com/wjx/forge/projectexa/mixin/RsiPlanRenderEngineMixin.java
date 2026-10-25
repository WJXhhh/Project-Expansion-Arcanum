package com.wjx.forge.projectexa.mixin;

import com.huanghuang.rsintegration.crafting.plan.PlanRenderEngine;
import com.huanghuang.rsintegration.crafting.plan.PlanResponse;
import com.huanghuang.rsintegration.crafting.tree.IngredientKey;
import com.wjx.forge.projectexa.integrations.rsi.RsiEmcPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Map;

@Pseudo
@Mixin(targets = "com.huanghuang.rsintegration.crafting.plan.PlanRenderEngine", remap = false)
public abstract class RsiPlanRenderEngineMixin {
    @Shadow @Final private Font font;
    @Unique private Iterator<IngredientKey> projectexa$materialKeys;

    @Inject(method = "renderMaterialArea", at = @At("HEAD"))
    private void projectexa$drawQuote(GuiGraphics graphics, int x, int y, int width, int height,
                                    Map<IngredientKey, PlanResponse.Availability> materials,
                                    int mouseX, int mouseY, int scroll,
                                    PlanRenderEngine.MaterialTooltipSink tooltip,
                                    PlanRenderEngine.MaterialBookmarkSink bookmarks,
                                    CallbackInfoReturnable<Integer> cir) {
        projectexa$materialKeys = materials.keySet().iterator();
        if (Minecraft.getInstance().screen instanceof RsiEmcPreview preview && !preview.projectexa$emcTotal().isEmpty()) {
            graphics.drawString(font, preview.projectexa$emcTotal(), x + 10, y - font.lineHeight,
                    preview.projectexa$emcColor());
        }
    }

    @Redirect(method = "renderMaterialArea", at = @At(value = "INVOKE",
            target = "Lcom/huanghuang/rsintegration/crafting/plan/PlanRenderEngine;formatCompact(I)Ljava/lang/String;",
            ordinal = 1))
    private String projectexa$appendTransmutation(int needed) {
        String count = PlanRenderEngine.formatCompact(needed);
        if (projectexa$materialKeys != null && projectexa$materialKeys.hasNext()) {
            IngredientKey key = projectexa$materialKeys.next();
            if (Minecraft.getInstance().screen instanceof RsiEmcPreview preview) {
                count += preview.projectexa$emcSuffix(key.stack(1));
            }
        }
        // RSI measures this same string before rendering: brackets cannot overlap the next material.
        return count;
    }
}
