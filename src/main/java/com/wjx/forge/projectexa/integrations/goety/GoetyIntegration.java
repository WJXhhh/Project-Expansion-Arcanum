package com.wjx.forge.projectexa.integrations.goety;

import moze_intel.projecte.api.imc.CustomEMCRegistration;
import moze_intel.projecte.api.imc.IMCMethods;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

public final class GoetyIntegration {
    public static final String MOD_ID = "goety";
    public static final long ECTOPLASM_EMC = 512L;
    public static final long CURSED_METAL_INGOT_EMC = 2292L;
    public static final long SHADE_STONE_EMC = 283L;
    public static final long SOUL_JAR_EMC = 1698L;
    public static final long HOWLING_SOUL_EMC = 8192L;
    public static final long BLAZING_HELM_EMC = 16384L;
    public static final long CRYSTAL_BALL_EMC = 8192L;
    public static final long LOFTY_CHEST_EMC = 64L;
    public static final long SPIDER_SAC_EMC = 2048L;
    public static final long END_LAMP_EMC = 1920L;
    public static final long TALL_SKULL_EMC = 256L;
    public static final long REDSTONE_GOLEM_SKULL_EMC = 2048L;
    public static final long GRAVE_GOLEM_SKULL_EMC = 2048L;
    public static final long REDSTONE_MONSTROSITY_HEAD_EMC = 4096L;
    public static final long CRYPT_BOOKSHELF_EMC = 736L;
    public static final long PITHOS_EMC = 64L;
    public static final long CRYPT_CHEST_EMC = 64L;
    public static final long CRYPT_URN_EMC = 64L;
    public static final long CHIPPED_DARK_ANVIL_EMC = 198524L;
    public static final long DAMAGED_DARK_ANVIL_EMC = 198524L;
    public static final long NIGHT_BEACON_EMC = 32768L;
    public static final long STORMLANDER_EMC = 65536L;
    public static final long FELL_BLADE_EMC = 4096L;
    public static final long FROZEN_BLADE_EMC = 8192L;
    public static final long TOTEM_OF_ROOTS_EMC = 2256L;
    public static final long SPENT_TOTEM_EMC = 42688L;
    public static final long GRAVE_DUST_EMC = 512L;
    public static final long SHADOW_ESSENCE_EMC = 4096L;
    public static final long SPIDER_EGG_EMC = 256L;
    public static final long JADE_EMC = 512L;
    public static final long VENOMOUS_FANG_EMC = 512L;
    public static final long RAGING_MATTER_EMC = 512L;
    public static final long ICE_CUBE_EMC = 1L;
    public static final long VOID_ECHO_EMC = 4096L;
    public static final long SOUL_RUBY_EMC = 16384L;
    public static final long EMPTY_FOCUS_EMC = 256L;
    public static final long SAVAGE_TOOTH_EMC = 1296L;
    public static final long MAGIC_EMERALD_EMC = 16384L;
    public static final long VOID_SHARD_EMC = 2048L;
    public static final long OMINOUS_SHARD_EMC = 4096L;
    public static final long WITHERED_MANUSCRIPT_EMC = 4096L;
    public static final long SHROUDED_BLUEPRINT_EMC = 8192L;
    public static final long RAVAGING_SCROLL_EMC = 4096L;
    public static final long WARRED_SCROLL_EMC = 8192L;
    public static final long BURIED_SCROLL_EMC = 2048L;
    public static final long HAUNTING_SCROLL_EMC = 8192L;
    public static final long FRONT_SCROLL_EMC = 4096L;
    public static final long MISTRAL_SCROLL_EMC = 8192L;
    public static final long FLORAL_SCROLL_EMC = 2048L;
    public static final long CURSED_KNIGHT_HELMET_EMC = 8192L;
    public static final long CURSED_KNIGHT_CHESTPLATE_EMC = 16384L;
    public static final long CURSED_KNIGHT_LEGGINGS_EMC = 12288L;
    public static final long CURSED_KNIGHT_BOOTS_EMC = 8192L;
    public static final long CURSED_PALADIN_HELMET_EMC = 16384L;
    public static final long CURSED_PALADIN_CHESTPLATE_EMC = 32768L;
    public static final long CURSED_PALADIN_LEGGINGS_EMC = 24576L;
    public static final long CURSED_PALADIN_BOOTS_EMC = 16384L;
    public static final long FEET_OF_FROG_EMC = 64L;
    public static final long REFUSE_BOTTLE_EMC = 1L;
    public static final long VOID_BOTTLE_EMC = 1025L;
    public static final long VOID_BUCKET_EMC = 3840L;
    public static final long END_MUD_BOTTLE_EMC = 65L;
    public static final long END_MUD_BUCKET_EMC = 960L;
    public static final long UNHOLY_BLOOD_EMC = 32768L;
    public static final long PURE_UNHOLY_BLOOD_EMC = 65536L;
    public static final long SNAP_FUNGUS_EMC = 1120L;
    public static final long HENBANE_FLOWER_EMC = 64L;
    public static final long NIGHTSHADE_BLOSSOM_EMC = 64L;
    public static final long CORPSE_BLOSSOM_EMC = 64L;
    public static final long SIENNA_GRASS_EMC = 1L;
    public static final long TALL_SIENNA_GRASS_EMC = 1L;
    public static final long SIENNA_FERN_EMC = 1L;
    public static final long LARGE_SIENNA_FERN_EMC = 1L;
    public static final long WINDSWEPT_DEAD_BUSH_EMC = 1L;
    public static final long END_GRASS_SPROUT_EMC = 1L;
    public static final long END_GRASS_EMC = 1L;
    public static final long TALL_END_GRASS_EMC = 1L;
    public static final long CHORUS_TALL_GRASS_EMC = 1L;
    public static final long CHORUS_FERN_SPROUT_EMC = 1L;
    public static final long CHORUS_FERN_EMC = 1L;
    public static final long LARGE_CHORUS_FERN_EMC = 1L;
    public static final long CHORUS_SPROUT_EMC = 1L;
    public static final long CHORUS_STALK_EMC = 1L;
    public static final long LARGE_CHORUS_STALK_EMC = 1L;
    public static final long CHORUS_VINE_EMC = 8L;
    public static final long CHORUS_BLOSSOM_VINES_EMC = 8L;
    public static final long CHORUS_BLOSSOM_VINES_PRUNED_EMC = 8L;
    public static final long END_GROWTH_VINES_EMC = 8L;
    public static final long END_ROCK_EMC = 1L;
    public static final long END_BASALT_EMC = 4L;
    public static final long END_SOIL_EMC = 1L;
    public static final long END_MUD_EMC = 1L;
    public static final long JADE_ORE_EMC = 512L;

    private GoetyIntegration() {
    }

    public static void sendIMC(InterModEnqueueEvent event) {
        registerEMC("ectoplasm", ECTOPLASM_EMC);
        registerEMC("cursed_ingot", CURSED_METAL_INGOT_EMC);
        registerEMC("shade_stone", SHADE_STONE_EMC);
        registerEMC("soul_jar", SOUL_JAR_EMC);
        registerEMC("howling_soul", HOWLING_SOUL_EMC);
        registerEMC("blazing_helm", BLAZING_HELM_EMC);
        registerEMC("crystal_ball", CRYSTAL_BALL_EMC);
        registerEMC("lofty_chest", LOFTY_CHEST_EMC);
        registerEMC("spider_sac", SPIDER_SAC_EMC);
        registerEMC("end_lamp", END_LAMP_EMC);
        registerEMC("tall_skull", TALL_SKULL_EMC);
        registerEMC("redstone_golem_skull", REDSTONE_GOLEM_SKULL_EMC);
        registerEMC("grave_golem_skull", GRAVE_GOLEM_SKULL_EMC);
        registerEMC("redstone_monstrosity_head", REDSTONE_MONSTROSITY_HEAD_EMC);
        registerEMC("crypt_bookshelf", CRYPT_BOOKSHELF_EMC);
        registerEMC("pithos", PITHOS_EMC);
        registerEMC("crypt_chest", CRYPT_CHEST_EMC);
        registerEMC("crypt_urn", CRYPT_URN_EMC);
        registerEMC("chipped_dark_anvil", CHIPPED_DARK_ANVIL_EMC);
        registerEMC("damaged_dark_anvil", DAMAGED_DARK_ANVIL_EMC);
        registerEMC("night_beacon", NIGHT_BEACON_EMC);
        registerEMC("stormlander", STORMLANDER_EMC);
        registerEMC("fell_blade", FELL_BLADE_EMC);
        registerEMC("frozen_blade", FROZEN_BLADE_EMC);
        registerEMC("totem_of_roots", TOTEM_OF_ROOTS_EMC);
        registerEMC("spent_totem", SPENT_TOTEM_EMC);
        registerEMC("grave_dust", GRAVE_DUST_EMC);
        registerEMC("shadow_essence", SHADOW_ESSENCE_EMC);
        registerEMC("spider_egg", SPIDER_EGG_EMC);
        registerEMC("jade", JADE_EMC);
        registerEMC("venomous_fang", VENOMOUS_FANG_EMC);
        registerEMC("raging_matter", RAGING_MATTER_EMC);
        registerEMC("ice_cube", ICE_CUBE_EMC);
        registerEMC("void_echo", VOID_ECHO_EMC);
        registerEMC("soul_ruby", SOUL_RUBY_EMC);
        registerEMC("empty_focus", EMPTY_FOCUS_EMC);
        registerEMC("savage_tooth", SAVAGE_TOOTH_EMC);
        registerEMC("magic_emerald", MAGIC_EMERALD_EMC);
        registerEMC("void_shard", VOID_SHARD_EMC);
        registerEMC("ominous_shard", OMINOUS_SHARD_EMC);
        registerEMC("withered_manuscript", WITHERED_MANUSCRIPT_EMC);
        registerEMC("shrouded_blueprint", SHROUDED_BLUEPRINT_EMC);
        registerEMC("ravaging_scroll", RAVAGING_SCROLL_EMC);
        registerEMC("warred_scroll", WARRED_SCROLL_EMC);
        registerEMC("buried_scroll", BURIED_SCROLL_EMC);
        registerEMC("haunting_scroll", HAUNTING_SCROLL_EMC);
        registerEMC("front_scroll", FRONT_SCROLL_EMC);
        registerEMC("mistral_scroll", MISTRAL_SCROLL_EMC);
        registerEMC("floral_scroll", FLORAL_SCROLL_EMC);
        registerEMC("cursed_knight_helmet", CURSED_KNIGHT_HELMET_EMC);
        registerEMC("cursed_knight_chestplate", CURSED_KNIGHT_CHESTPLATE_EMC);
        registerEMC("cursed_knight_leggings", CURSED_KNIGHT_LEGGINGS_EMC);
        registerEMC("cursed_knight_boots", CURSED_KNIGHT_BOOTS_EMC);
        registerEMC("cursed_paladin_helmet", CURSED_PALADIN_HELMET_EMC);
        registerEMC("cursed_paladin_chestplate", CURSED_PALADIN_CHESTPLATE_EMC);
        registerEMC("cursed_paladin_leggings", CURSED_PALADIN_LEGGINGS_EMC);
        registerEMC("cursed_paladin_boots", CURSED_PALADIN_BOOTS_EMC);
        registerEMC("feet_of_frog", FEET_OF_FROG_EMC);
        registerEMC("refuse_bottle", REFUSE_BOTTLE_EMC);
        registerEMC("void_bottle", VOID_BOTTLE_EMC);
        registerEMC("void_bucket", VOID_BUCKET_EMC);
        registerEMC("end_mud_bottle", END_MUD_BOTTLE_EMC);
        registerEMC("end_mud_bucket", END_MUD_BUCKET_EMC);
        registerEMC("unholy_blood", UNHOLY_BLOOD_EMC);
        registerPureUnholyBloodEMC();
        registerEMC("snap_fungus", SNAP_FUNGUS_EMC);
        registerEMC("henbane_flower", HENBANE_FLOWER_EMC);
        registerEMC("nightshade_blossom", NIGHTSHADE_BLOSSOM_EMC);
        registerEMC("corpse_blossom", CORPSE_BLOSSOM_EMC);
        registerEMC("sienna_grass", SIENNA_GRASS_EMC);
        registerEMC("tall_sienna_grass", TALL_SIENNA_GRASS_EMC);
        registerEMC("sienna_fern", SIENNA_FERN_EMC);
        registerEMC("large_sienna_fern", LARGE_SIENNA_FERN_EMC);
        registerEMC("windswept_dead_bush", WINDSWEPT_DEAD_BUSH_EMC);
        registerEMC("end_grass_sprout", END_GRASS_SPROUT_EMC);
        registerEMC("end_grass", END_GRASS_EMC);
        registerEMC("tall_end_grass", TALL_END_GRASS_EMC);
        registerEMC("chorus_tall_grass", CHORUS_TALL_GRASS_EMC);
        registerEMC("chorus_fern_sprout", CHORUS_FERN_SPROUT_EMC);
        registerEMC("chorus_fern", CHORUS_FERN_EMC);
        registerEMC("large_chorus_fern", LARGE_CHORUS_FERN_EMC);
        registerEMC("chorus_sprout", CHORUS_SPROUT_EMC);
        registerEMC("chorus_stalk", CHORUS_STALK_EMC);
        registerEMC("large_chorus_stalk", LARGE_CHORUS_STALK_EMC);
        registerEMC("chorus_vine", CHORUS_VINE_EMC);
        registerEMC("chorus_blossom_vines", CHORUS_BLOSSOM_VINES_EMC);
        registerEMC("chorus_blossom_vines_pruned", CHORUS_BLOSSOM_VINES_PRUNED_EMC);
        registerEMC("end_growth_vines", END_GROWTH_VINES_EMC);
        registerEMC("end_rock", END_ROCK_EMC);
        registerEMC("end_basalt", END_BASALT_EMC);
        registerEMC("end_soil", END_SOIL_EMC);
        registerEMC("end_mud", END_MUD_EMC);
        registerEMC("jade_ore", JADE_ORE_EMC);
    }

    private static void registerPureUnholyBloodEMC() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("Pure", true);
        registerEMC(
                NSSItem.createItem(new ResourceLocation(MOD_ID, "unholy_blood"), nbt),
                PURE_UNHOLY_BLOOD_EMC
        );
    }

    private static void registerEMC(NSSItem item, long emc) {
        InterModComms.sendTo(
                "projecte",
                IMCMethods.REGISTER_CUSTOM_EMC,
                () -> new CustomEMCRegistration(item, emc)
        );
    }

    private static void registerEMC(String itemPath, long emc) {
        registerEMC(NSSItem.createItem(new ResourceLocation(MOD_ID, itemPath)), emc);
    }
}
