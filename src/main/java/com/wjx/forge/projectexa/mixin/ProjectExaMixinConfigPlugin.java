package com.wjx.forge.projectexa.mixin;

import net.minecraftforge.fml.loading.FMLEnvironment;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/** Selects JEI transfer Mixins and limits RSI lifecycle workarounds to development runs. */
public final class ProjectExaMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String RECIPE_TRANSFER_BUTTON = "mezz.jei.gui.recipes.RecipeTransferButton";
    private static final String MODERN_MIXIN =
            "com.wjx.forge.projectexa.mixin.JeiRecipeTransferButtonMixin";
    private static final String LEGACY_MIXIN =
            "com.wjx.forge.projectexa.mixin.JeiRecipeTransferButtonLegacyMixin";
    private static final String LEGACY_CREATE_DESCRIPTOR =
            "(Lmezz/jei/api/gui/IRecipeLayoutDrawable;Ljava/lang/Runnable;)"
                    + "Lmezz/jei/gui/recipes/RecipeTransferButton;";
    private static final String MODERN_CREATE_DESCRIPTOR =
            "(Lmezz/jei/api/gui/IRecipeLayoutDrawable;"
                    + "Lmezz/jei/common/transfer/RecipeTransferService;"
                    + "Ljava/util/function/Supplier;Ljava/lang/Runnable;)"
                    + "Lmezz/jei/gui/recipes/RecipeTransferButton;";

    private static final Set<String> DEVELOPMENT_ONLY_RSI_MIXINS = Set.of(
            "com.wjx.forge.projectexa.mixin.RsiKeyMappingsMixin",
            "com.wjx.forge.projectexa.mixin.RsiSidePanelModuleMixin",
            "com.wjx.forge.projectexa.mixin.RsiSidePanelClientMixin");
    private static final Set<String> LEGACY_RSI_INVENTORY_MIXINS = Set.of(
            "com.wjx.forge.projectexa.mixin.RsiExtractionLedgerAccessor",
            "com.wjx.forge.projectexa.mixin.RsiExtractionLedgerMixin",
            "com.wjx.forge.projectexa.mixin.RsiMaterialSourcesMixin");
    private static final String BD_SESSION_MIXIN =
            "com.wjx.forge.projectexa.mixin.RsiBeyondDimensionsSessionMixin";
    private static final String RS_STORAGE_CACHE =
            "com/refinedmods/refinedstorage/api/storage/cache/IStorageCache.class";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Published RSI mods tolerate early config reads; only userdev runs need these workarounds.
        if (DEVELOPMENT_ONLY_RSI_MIXINS.contains(mixinClassName)) {
            return !FMLEnvironment.production;
        }
        // RSI supports Beyond Dimensions without RS. Its ledger still contains
        // legacy RS bytecode, so transforming it without this API forces
        // ModLauncher to resolve a class that is intentionally not installed.
        if (LEGACY_RSI_INVENTORY_MIXINS.contains(mixinClassName)) {
            return hasResource(RS_STORAGE_CACHE);
        }
        if (BD_SESSION_MIXIN.equals(mixinClassName)) {
            return !hasResource(RS_STORAGE_CACHE);
        }
        if (!RECIPE_TRANSFER_BUTTON.equals(targetClassName)) {
            return true;
        }
        if (MODERN_MIXIN.equals(mixinClassName)) {
            return hasCreateMethod(targetClassName, MODERN_CREATE_DESCRIPTOR);
        }
        if (LEGACY_MIXIN.equals(mixinClassName)) {
            return hasCreateMethod(targetClassName, LEGACY_CREATE_DESCRIPTOR);
        }
        return true;
    }

    private static boolean hasCreateMethod(String targetClassName, String descriptor) {
        String resourceName = targetClassName.replace('.', '/') + ".class";
        ClassLoader[] classLoaders = {
                ProjectExaMixinConfigPlugin.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };

        for (ClassLoader classLoader : classLoaders) {
            if (classLoader == null) {
                continue;
            }
            try (InputStream inputStream = classLoader.getResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    continue;
                }
                ClassReader classReader = new ClassReader(inputStream);
                boolean[] found = {false};
                classReader.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(
                            int access,
                            String name,
                            String methodDescriptor,
                            String signature,
                            String[] exceptions) {
                        if (name.equals("create") && methodDescriptor.equals(descriptor)) {
                            found[0] = true;
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return found[0];
            } catch (IOException | RuntimeException ignored) {
                // Try the next class loader. JEI is optional during early startup.
            }
        }
        return false;
    }

    private static boolean hasResource(String resourceName) {
        ClassLoader[] classLoaders = {
                ProjectExaMixinConfigPlugin.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null && classLoader.getResource(resourceName) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
