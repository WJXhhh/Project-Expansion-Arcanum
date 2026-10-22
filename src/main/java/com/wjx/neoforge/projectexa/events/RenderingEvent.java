package com.wjx.neoforge.projectexa.events;

import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.registries.BlockEntityTypes;
import com.wjx.neoforge.projectexa.rendering.ChestRenderer;
import com.wjx.neoforge.projectexa.util.IChestLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Main.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderingEvent {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerChest(event, BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get());
        registerChest(event, BlockEntityTypes.CONDENSER_MK3.get());
    }

    private static <BE extends BlockEntity & IChestLike> void registerChest(EntityRenderersEvent.RegisterRenderers event, BlockEntityType<BE> type) {
        event.registerBlockEntityRenderer(type, context -> new ChestRenderer<>(context, type));
    }
}
