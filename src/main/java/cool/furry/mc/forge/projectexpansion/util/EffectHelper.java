package cool.furry.mc.forge.projectexpansion.util;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import javax.annotation.Nullable;
import java.util.Optional;

// this only exists currently because all the parameters in MobEffectInstance are unnamed
public class EffectHelper {
    public static MobEffectInstance create(MobEffect effect) {
        return new MobEffectInstance(effect);
    }

    public static MobEffectInstance create(MobEffect effect, int duration) {
        return new MobEffectInstance(effect, duration, 0);
    }

    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible) {
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible);
    }

    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon);
    }

    public static MobEffectInstance create(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, @Nullable MobEffectInstance hiddenEffect) {
        return new MobEffectInstance(effect, duration, amplifier, ambient, visible, showIcon, hiddenEffect);
    }
}
