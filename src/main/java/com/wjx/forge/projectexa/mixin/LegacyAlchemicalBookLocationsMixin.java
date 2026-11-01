package com.wjx.forge.projectexa.mixin;

import com.wjx.forge.projectexa.Main;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Migrates both player and item book locations before Forge selects capability NBT by key. */
@Mixin(value = CapabilityDispatcher.class, remap = false)
public abstract class LegacyAlchemicalBookLocationsMixin {
    @Unique
    private static final String projectexa$legacyKey = "projectexpansion:alchemical_book_locations";
    @Unique
    private static final String projectexa$currentKey = Main.MOD_ID + ":alchemical_book_locations";

    @Shadow
    @Final
    private String[] names;

    @ModifyVariable(
            method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private CompoundTag projectexa$migrateBookLocations(CompoundTag nbt) {
        if (!Arrays.asList(names).contains(projectexa$currentKey)
                || !nbt.contains(projectexa$legacyKey, Tag.TAG_COMPOUND)) {
            return nbt;
        }

        // Work on copies: item capability NBT can be shared with another stack.
        CompoundTag migrated = nbt.copy();
        CompoundTag current = migrated.getCompound(projectexa$currentKey);
        CompoundTag legacy = nbt.getCompound(projectexa$legacyKey);
        List<String> additions = legacy.getAllKeys().stream()
                .filter(name -> !current.contains(name))
                .sorted(Comparator.<String>comparingInt(name -> legacy.getCompound(name).getInt("Index"))
                        .thenComparing(Comparator.naturalOrder()))
                .toList();

        // Existing Arcanum destinations win name conflicts and retain their order.
        // Append legacy-only destinations rather than duplicating their old indices.
        int nextIndex = current.getAllKeys().stream()
                .filter(name -> !name.equals("@back"))
                .mapToInt(name -> current.getCompound(name).getInt("Index"))
                .max().orElse(-1) + 1;
        for (String name : additions) {
            CompoundTag location = legacy.getCompound(name).copy();
            location.putInt("Index", name.equals("@back") ? -1 : nextIndex++);
            current.put(name, location);
        }

        migrated.put(projectexa$currentKey, current);
        migrated.remove(projectexa$legacyKey);
        return migrated;
    }
}
