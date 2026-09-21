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

/** Selects the JEI transfer button mixin matching the installed JEI internals. */
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

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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
