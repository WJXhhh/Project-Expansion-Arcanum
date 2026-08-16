package com.wjx.forge.projectexa.events;

import com.wjx.forge.projectexa.Main;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Maps registry entries from the original runtime namespace to Arcanum's new
 * namespace. The block-entity NBT ID is handled separately by
 * {@code LegacyBlockEntityNbtMixin}, before vanilla resolves the entity type.
 */
@Mod.EventBusSubscriber(modid = Main.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyIdMigration {
    private static final String LEGACY_MOD_ID = "projectexpansion";

    private LegacyIdMigration() {
    }

    @SubscribeEvent
    public static void remapLegacyIds(MissingMappingsEvent event) {
        remap(event, ForgeRegistries.BLOCKS.getRegistryKey(), ForgeRegistries.BLOCKS);
        remap(event, ForgeRegistries.ITEMS.getRegistryKey(), ForgeRegistries.ITEMS);
        remap(event, ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), ForgeRegistries.BLOCK_ENTITY_TYPES);
        remap(event, ForgeRegistries.MENU_TYPES.getRegistryKey(), ForgeRegistries.MENU_TYPES);
        remap(event, ForgeRegistries.ENCHANTMENTS.getRegistryKey(), ForgeRegistries.ENCHANTMENTS);
        remap(event, ForgeRegistries.SOUND_EVENTS.getRegistryKey(), ForgeRegistries.SOUND_EVENTS);
    }

    private static <T> void remap(
            MissingMappingsEvent event,
            ResourceKey<? extends Registry<T>> registryKey,
            IForgeRegistry<T> registry
    ) {
        for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registryKey, LEGACY_MOD_ID)) {
            ResourceLocation legacyId = mapping.getKey();
            T replacement = registry.getValue(Main.rl(legacyId.getPath()));
            if (replacement != null) {
                mapping.remap(replacement);
            }
        }
    }
}
