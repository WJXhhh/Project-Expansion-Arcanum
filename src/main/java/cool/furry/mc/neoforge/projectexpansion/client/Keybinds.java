package cool.furry.mc.neoforge.projectexpansion.client;

import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static boolean REGISTERED = false;
    public static KeyMapping OPEN_TRANSMUTATION_TABLET;

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_TRANSMUTATION_TABLET = new KeyMapping(
                "key.projecte.curios.open_transmutation_tablet",
                GLFW.GLFW_KEY_K,
                PELang.PROJECTE.getTranslationKey()
        );
        event.register(OPEN_TRANSMUTATION_TABLET);
        REGISTERED = true;
    }
}
