package cool.furry.mc.forge.projectexpansion.mixin;

import cool.furry.mc.forge.projectexpansion.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewrites legacy block entity IDs before vanilla tries to resolve their type.
 * Registry missing mappings do not cover the string ID stored inside chunk NBT.
 */
@Mixin(BlockEntity.class)
public abstract class LegacyBlockEntityNbtMixin {
    private static final String LEGACY_PREFIX = "projectexpansion:";

    @Inject(method = "loadStatic", at = @At("HEAD"))
    private static void remapLegacyId(
            BlockPos pos,
            BlockState state,
            CompoundTag tag,
            CallbackInfoReturnable<BlockEntity> cir
    ) {
        if (!tag.contains("id", Tag.TAG_STRING)) {
            return;
        }

        String id = tag.getString("id");
        if (id.startsWith(LEGACY_PREFIX)) {
            tag.putString("id", Main.MOD_ID + ":" + id.substring(LEGACY_PREFIX.length()));
        }
    }
}
