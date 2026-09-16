package com.wjx.forge.projectexa.integrations.minecraft;

import moze_intel.projecte.api.imc.CustomEMCRegistration;
import moze_intel.projecte.api.imc.IMCMethods;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

public final class MinecraftIntegration {
    public static final String MOD_ID = "minecraft";
    public static final long WITHER_SKELETON_SKULL_EMC = 256L;
    public static final long TOTEM_OF_UNDYING_EMC = 8192L;
    public static final long REGULAR_GOAT_HORN_EMC = 96L;
    public static final long SCREAMING_GOAT_HORN_EMC = 192L;
    public static final long ANCIENT_DEBRIS_EMC = 12288L;
    public static final long FIREWORK_STAR_EMC = 200L;
    public static final long DRAGON_HEAD_EMC = 2048L;
    public static final long ELYTRA_EMC = 8192L;

    private MinecraftIntegration() {
    }

    public static void sendIMC(InterModEnqueueEvent event) {
        InterModComms.sendTo(
                "projecte",
                IMCMethods.REGISTER_CUSTOM_EMC,
                () -> new CustomEMCRegistration(
                        NSSItem.createItem(new ResourceLocation(MOD_ID, "wither_skeleton_skull")),
                        WITHER_SKELETON_SKULL_EMC
                )
        );
        InterModComms.sendTo(
                "projecte",
                IMCMethods.REGISTER_CUSTOM_EMC,
                () -> new CustomEMCRegistration(
                        NSSItem.createItem(new ResourceLocation(MOD_ID, "totem_of_undying")),
                        TOTEM_OF_UNDYING_EMC
                )
        );
        registerEMC("ancient_debris", ANCIENT_DEBRIS_EMC);
        registerEMC("firework_star", FIREWORK_STAR_EMC);
        registerEMC("dragon_head", DRAGON_HEAD_EMC);
        registerEMC("elytra", ELYTRA_EMC);
        registerEMC("goat_horn", REGULAR_GOAT_HORN_EMC);
        registerGoatHornEMC("ponder_goat_horn", REGULAR_GOAT_HORN_EMC);
        registerGoatHornEMC("sing_goat_horn", REGULAR_GOAT_HORN_EMC);
        registerGoatHornEMC("seek_goat_horn", REGULAR_GOAT_HORN_EMC);
        registerGoatHornEMC("feel_goat_horn", REGULAR_GOAT_HORN_EMC);
        registerGoatHornEMC("admire_goat_horn", SCREAMING_GOAT_HORN_EMC);
        registerGoatHornEMC("call_goat_horn", SCREAMING_GOAT_HORN_EMC);
        registerGoatHornEMC("yearn_goat_horn", SCREAMING_GOAT_HORN_EMC);
        registerGoatHornEMC("dream_goat_horn", SCREAMING_GOAT_HORN_EMC);
    }

    private static void registerGoatHornEMC(String instrument, long emc) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("instrument", MOD_ID + ":" + instrument);
        registerEMC(NSSItem.createItem(new ResourceLocation(MOD_ID, "goat_horn"), nbt), emc);
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
