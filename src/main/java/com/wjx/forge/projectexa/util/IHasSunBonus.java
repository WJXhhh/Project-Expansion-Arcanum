package com.wjx.forge.projectexa.util;

import com.wjx.forge.projectexa.block.BlockCompactSun;
import com.wjx.forge.projectexa.config.Config;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

public interface IHasSunBonus {
    boolean hasSunBonus();

    default @Nullable Integer getSunBonus() {
        if (!hasSunBonus()) return null;
        int bonus = Util.getSunBonus();
        return bonus == 0 ? null : bonus;
    }
}
