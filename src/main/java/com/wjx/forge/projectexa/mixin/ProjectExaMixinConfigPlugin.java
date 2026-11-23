package com.wjx.forge.projectexa.mixin;

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

/** Selects optional integrations without loading target classes during startup. */
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

    private static final Set<String> RSI_INVENTORY_MIXINS = Set.of(
            "com.wjx.forge.projectexa.mixin.RsiExtractionLedgerAccessor",
            "com.wjx.forge.projectexa.mixin.RsiExtractionLedgerMixin",
            "com.wjx.forge.projectexa.mixin.RsiTypedMaterialSourcesMixin");
    private static final String RSI_EXTENSION_API =
            "com.huanghuang.rsintegration.api.RSIItemRetrievalFallback";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".ForgematicaMaterialListMixin")) {
            return hasMethod(targetClassName, "getElementTotalWidth", "()I")
                    && hasClass("fi.dy.masa.malilib.gui.button.ButtonGeneric");
        }
        if (mixinClassName.endsWith(".JeiEmcCheatInputMixin")) {
            return hasClass("mezz.jei.gui.input.handlers.CheatInputHandler");
        }
        if (mixinClassName.endsWith(".JeiEmcLegacyInputMixin")) {
            return !hasClass("mezz.jei.gui.input.handlers.CheatInputHandler");
        }
        if (RSI_INVENTORY_MIXINS.contains(mixinClassName)) {
            return hasClass(RSI_EXTENSION_API);
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

    private static boolean hasClass(String name) {
        String resource = name.replace('.', '/') + ".class";
        ClassLoader loader = ProjectExaMixinConfigPlugin.class.getClassLoader();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return (loader != null && loader.getResource(resource) != null)
                || (context != null && context.getResource(resource) != null);
    }

    private static boolean hasCreateMethod(String targetClassName, String descriptor) {
        return hasMethod(targetClassName, "create", descriptor);
    }

    private static boolean hasMethod(String targetClassName, String methodName, String descriptor) {
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
                        if (name.equals(methodName) && methodDescriptor.equals(descriptor)) {
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
