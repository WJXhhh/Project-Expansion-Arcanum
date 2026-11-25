package com.wjx.forge.projectexa.integrations.jei;

import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.common.Internal;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IClickableIngredientInternal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

final class JeiEmcInputCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private JeiEmcInputCompatibility() { }

    static Optional<InputMethods> getInputMethods(Class<? extends IJeiUserInput> inputClass) {
        return InputMethodCache.METHODS.get(inputClass);
    }

    static @Nullable IClientConfig getClientConfig() {
        ConfigMethods methods = ConfigMethodCache.METHODS;
        if (methods == null) {
            return null;
        }
        Object configs = invoke(methods.getConfigs(), null);
        return (IClientConfig) invoke(methods.getClientConfig(), configs);
    }

    // The virtual method and the wrapper constructor use whichever input package JEI is running.
    private static final class InputMethodCache {
        private static final ClassValue<Optional<InputMethods>> METHODS = new ClassValue<>() {
            @Override
            protected Optional<InputMethods> computeValue(Class<?> inputClass) {
                try {
                    Method inputType = inputClass.getMethod("getInputType");
                    Class<?> userInput = inputType.getDeclaringClass();
                    String inputPackage = userInput.getPackageName();
                    ClassLoader loader = userInput.getClassLoader();
                    Class<?> handler = Class.forName(inputPackage + ".IUserInputHandler", false, loader);
                    Class<?> mouseOverable = Class.forName(inputPackage + ".IMouseOverable", false, loader);
                    Class<?> sameElement = Class.forName(
                            inputPackage + ".handlers.SameElementInputHandler", false, loader);
                    Method ingredientLookup = CombinedRecipeFocusSource.class.getMethod(
                            "getIngredientUnderMouse", userInput, IInternalKeyMappings.class);
                    MethodHandle mouseOver = MethodHandles.lookup().unreflect(
                            IClickableIngredientInternal.class.getMethod("isMouseOver", double.class, double.class));
                    return Optional.of(new InputMethods(inputType, ingredientLookup,
                            sameElement.getConstructor(handler, mouseOverable), mouseOverable, mouseOver));
                } catch (ReflectiveOperationException | LinkageError exception) {
                    LOGGER.warn("Could not bind JEI EMC input for {}", inputClass.getName(), exception);
                    return Optional.empty();
                }
            }
        };
    }

    record InputMethods(Method inputType, Method ingredientLookup, Constructor<?> sameElement,
                        Class<?> mouseOverable, MethodHandle mouseOver) {
        boolean isExecute(IJeiUserInput input) {
            return ((Enum<?>) invoke(inputType, input)).name().equals("EXECUTE");
        }

        @SuppressWarnings("unchecked")
        Stream<IClickableIngredientInternal<?>> getIngredients(Object source, IJeiUserInput input,
                                                                IInternalKeyMappings keys) {
            return (Stream<IClickableIngredientInternal<?>>) invoke(ingredientLookup, source, input, keys);
        }

        Object createSameElementHandler(Object handler, IClickableIngredientInternal<?> clicked) {
            Object hitbox = MethodHandleProxies.asInterfaceInstance(mouseOverable, mouseOver.bindTo(clicked));
            try {
                return sameElement.newInstance(handler, hitbox);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not create JEI's same-element input handler", exception);
            }
        }
    }

    private static final class ConfigMethodCache {
        private static final @Nullable ConfigMethods METHODS = findConfigMethods();

        private static @Nullable ConfigMethods findConfigMethods() {
            try {
                Method getConfigs;
                try {
                    getConfigs = Internal.class.getMethod("getClientConfigs");
                } catch (NoSuchMethodException ignored) {
                    getConfigs = Internal.class.getMethod("getJeiClientConfigs");
                }
                return new ConfigMethods(getConfigs, getConfigs.getReturnType().getMethod("getClientConfig"));
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.warn("Could not bind JEI client configuration", exception);
                return null;
            }
        }
    }

    private record ConfigMethods(Method getConfigs, Method getClientConfig) { }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not invoke " + method, exception);
        }
    }
}
