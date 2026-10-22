package com.wjx.neoforge.projectexa.integrations.goety;

import moze_intel.projecte.api.mapper.EMCMapper;
import moze_intel.projecte.api.mapper.IEMCMapper;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.nss.NSSItem;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** ProjectE 1.21 removed custom-EMC IMC; mapper discovery also runs during reloademc. */
@EMCMapper(requiredMods = "goety")
public final class GoetyEMCMapper implements IEMCMapper<NormalizedSimpleStack, Long> {
    private static final java.util.Map<String, Long> SEEDS = java.util.Map.ofEntries(
            java.util.Map.entry("ectoplasm", 512L),
            java.util.Map.entry("cursed_ingot", 2292L),
            java.util.Map.entry("shade_stone", 283L),
            java.util.Map.entry("soul_jar", 1698L),
            java.util.Map.entry("howling_soul", 8192L),
            java.util.Map.entry("blazing_helm", 16384L),
            java.util.Map.entry("crystal_ball", 8192L),
            java.util.Map.entry("lofty_chest", 64L),
            java.util.Map.entry("spider_sac", 2048L),
            java.util.Map.entry("end_lamp", 1920L),
            java.util.Map.entry("tall_skull", 256L),
            java.util.Map.entry("redstone_golem_skull", 2048L),
            java.util.Map.entry("grave_golem_skull", 2048L),
            java.util.Map.entry("redstone_monstrosity_head", 4096L),
            java.util.Map.entry("crypt_bookshelf", 736L),
            java.util.Map.entry("pithos", 64L),
            java.util.Map.entry("crypt_chest", 64L),
            java.util.Map.entry("crypt_urn", 64L),
            java.util.Map.entry("chipped_dark_anvil", 198524L),
            java.util.Map.entry("damaged_dark_anvil", 198524L),
            java.util.Map.entry("night_beacon", 32768L),
            java.util.Map.entry("stormlander", 65536L),
            java.util.Map.entry("fell_blade", 4096L),
            java.util.Map.entry("frozen_blade", 8192L),
            java.util.Map.entry("totem_of_roots", 2256L),
            java.util.Map.entry("spent_totem", 42688L),
            java.util.Map.entry("grave_dust", 512L),
            java.util.Map.entry("shadow_essence", 4096L),
            java.util.Map.entry("spider_egg", 256L),
            java.util.Map.entry("jade", 512L),
            java.util.Map.entry("venomous_fang", 512L),
            java.util.Map.entry("raging_matter", 512L),
            java.util.Map.entry("ice_cube", 1L),
            java.util.Map.entry("void_echo", 4096L),
            java.util.Map.entry("soul_ruby", 16384L),
            java.util.Map.entry("empty_focus", 256L),
            java.util.Map.entry("savage_tooth", 1296L),
            java.util.Map.entry("magic_emerald", 16384L),
            java.util.Map.entry("void_shard", 2048L),
            java.util.Map.entry("ominous_shard", 4096L),
            java.util.Map.entry("withered_manuscript", 4096L),
            java.util.Map.entry("shrouded_blueprint", 8192L),
            java.util.Map.entry("ravaging_scroll", 4096L),
            java.util.Map.entry("warred_scroll", 8192L),
            java.util.Map.entry("buried_scroll", 2048L),
            java.util.Map.entry("haunting_scroll", 8192L),
            java.util.Map.entry("front_scroll", 4096L),
            java.util.Map.entry("mistral_scroll", 8192L),
            java.util.Map.entry("floral_scroll", 2048L),
            java.util.Map.entry("cursed_knight_helmet", 8192L),
            java.util.Map.entry("cursed_knight_chestplate", 16384L),
            java.util.Map.entry("cursed_knight_leggings", 12288L),
            java.util.Map.entry("cursed_knight_boots", 8192L),
            java.util.Map.entry("cursed_paladin_helmet", 16384L),
            java.util.Map.entry("cursed_paladin_chestplate", 32768L),
            java.util.Map.entry("cursed_paladin_leggings", 24576L),
            java.util.Map.entry("cursed_paladin_boots", 16384L),
            java.util.Map.entry("feet_of_frog", 64L),
            java.util.Map.entry("refuse_bottle", 1L),
            java.util.Map.entry("void_bottle", 1025L),
            java.util.Map.entry("void_bucket", 3840L),
            java.util.Map.entry("end_mud_bottle", 65L),
            java.util.Map.entry("end_mud_bucket", 960L),
            java.util.Map.entry("unholy_blood", 32768L),
            java.util.Map.entry("snap_fungus", 1120L),
            java.util.Map.entry("henbane_flower", 64L),
            java.util.Map.entry("nightshade_blossom", 64L),
            java.util.Map.entry("corpse_blossom", 64L),
            java.util.Map.entry("sienna_grass", 1L),
            java.util.Map.entry("tall_sienna_grass", 1L),
            java.util.Map.entry("sienna_fern", 1L),
            java.util.Map.entry("large_sienna_fern", 1L),
            java.util.Map.entry("windswept_dead_bush", 1L),
            java.util.Map.entry("end_grass_sprout", 1L),
            java.util.Map.entry("end_grass", 1L),
            java.util.Map.entry("tall_end_grass", 1L),
            java.util.Map.entry("chorus_tall_grass", 1L),
            java.util.Map.entry("chorus_fern_sprout", 1L),
            java.util.Map.entry("chorus_fern", 1L),
            java.util.Map.entry("large_chorus_fern", 1L),
            java.util.Map.entry("chorus_sprout", 1L),
            java.util.Map.entry("chorus_stalk", 1L),
            java.util.Map.entry("large_chorus_stalk", 1L),
            java.util.Map.entry("chorus_vine", 8L),
            java.util.Map.entry("chorus_blossom_vines", 8L),
            java.util.Map.entry("chorus_blossom_vines_pruned", 8L),
            java.util.Map.entry("end_growth_vines", 8L),
            java.util.Map.entry("end_rock", 1L),
            java.util.Map.entry("end_basalt", 4L),
            java.util.Map.entry("end_soil", 1L),
            java.util.Map.entry("end_mud", 1L),
            java.util.Map.entry("jade_ore", 512L));
    @Override public String getTranslationKey() { return "projectexa.integration.goety.seeds"; }
    @Override public String getName() { return "GoetyCustomEMC"; }
    @Override public String getDescription() { return "Goety base values ported from Arcanum 1.20.1, with component-aware pure blood."; }
    @Override
    public void addMappings(IMappingCollector<NormalizedSimpleStack, Long> mapper, ReloadableServerResources resources,
                            RegistryAccess registryAccess, ResourceManager resourceManager) {
        SEEDS.forEach((name, value) -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("goety", name);
            if (BuiltInRegistries.ITEM.containsKey(id)) mapper.setValueBefore(NSSItem.createItem(id), value);
        });
        ResourceLocation bloodId = ResourceLocation.fromNamespaceAndPath("goety", "unholy_blood");
        if (BuiltInRegistries.ITEM.containsKey(bloodId)) {
            ItemStack blood = new ItemStack(BuiltInRegistries.ITEM.get(bloodId));
            CompoundTag data = new CompoundTag(); data.putBoolean("Pure", true);
            blood.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            mapper.setValueBefore(NSSItem.createItem(blood), 65536L);
        }
    }
}
