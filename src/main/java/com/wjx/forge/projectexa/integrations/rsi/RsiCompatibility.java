package com.wjx.forge.projectexa.integrations.rsi;

import com.wjx.forge.projectexa.Main;
import net.minecraftforge.fml.ModList;

public final class RsiCompatibility {
    private RsiCompatibility() {}

    public static boolean isAvailable() {
        return Availability.SUPPORTED;
    }

    public static void registerAutoEatFoodSourceIfAvailable() {
        try {
            Class.forName("com.huanghuang.rsintegration.api.RSIAutoEatFoodSources", false,
                    RsiCompatibility.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError unavailable) {
            return;
        }
        RsiEmcFoodSource.register();
    }

    private static final class Availability {
        private static final boolean SUPPORTED = detect();

        private static boolean detect() {
            if (!ModList.get().isLoaded("rs_integration")) return false;
            try {
                ClassLoader loader = RsiCompatibility.class.getClassLoader();
                Class.forName("com.huanghuang.rsintegration.api.RSICraftingMaterialSources", false, loader);
                Class.forName("com.huanghuang.rsintegration.api.RSIItemRetrieval", false, loader);
                return true;
            } catch (ClassNotFoundException | LinkageError failure) {
                Main.Logger.warn("ProjectExA RSI integration requires the native extension API 1.3");
                return false;
            }
        }
    }
}
