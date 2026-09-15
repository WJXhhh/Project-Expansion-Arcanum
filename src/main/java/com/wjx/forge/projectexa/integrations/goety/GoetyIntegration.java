package com.wjx.forge.projectexa.integrations.goety;

import moze_intel.projecte.api.imc.CustomEMCRegistration;
import moze_intel.projecte.api.imc.IMCMethods;
import moze_intel.projecte.api.nss.NSSItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

public final class GoetyIntegration {
    public static final String MOD_ID = "goety";
    public static final long ECTOPLASM_EMC = 512L;
    public static final long CURSED_METAL_INGOT_EMC = 2292L;

    private GoetyIntegration() {
    }

    public static void sendIMC(InterModEnqueueEvent event) {
        registerEMC("ectoplasm", ECTOPLASM_EMC);
        registerEMC("cursed_ingot", CURSED_METAL_INGOT_EMC);
    }

    private static void registerEMC(String itemPath, long emc) {
        InterModComms.sendTo(
                "projecte",
                IMCMethods.REGISTER_CUSTOM_EMC,
                () -> new CustomEMCRegistration(
                        NSSItem.createItem(new ResourceLocation(MOD_ID, itemPath)),
                        emc
                )
        );
    }
}
