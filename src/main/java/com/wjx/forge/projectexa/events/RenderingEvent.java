package com.wjx.forge.projectexa.events;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.block.BlockAdvancedAlchemicalChest;
import com.wjx.forge.projectexa.block.entity.BlockEntityAdvancedAlchemicalChest;
import com.wjx.forge.projectexa.rendering.ChestRenderer;
import com.wjx.forge.projectexa.util.AdvancedAlchemicalChest;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for(DyeColor color: DyeColor.values()) {
            RegistryObject<BlockAdvancedAlchemicalChest> block = AdvancedAlchemicalChest.getRegistryBlock(color);
            BlockEntityType<BlockEntityAdvancedAlchemicalChest> blockEntityType = AdvancedAlchemicalChest.getBlockEntityType(color);
            event.registerBlockEntityRenderer(blockEntityType, context -> new ChestRenderer<>(context, Main.rl(String.format("textures/block/advanced_alchemical_chest/%s.png", color.getName())), () -> block));
        }
    }
}
