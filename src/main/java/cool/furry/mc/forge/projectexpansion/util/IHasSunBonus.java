package cool.furry.mc.forge.projectexpansion.util;

import cool.furry.mc.forge.projectexpansion.block.BlockCompactSun;
import cool.furry.mc.forge.projectexpansion.config.Config;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface IHasSunBonus {
    boolean hasSunBonus();

    default @Nullable Integer getSunBonus() {
        int bonus = Config.compactSunBonus.get();
        return bonus == 0 || !hasSunBonus() ? null : bonus;
    }
}
