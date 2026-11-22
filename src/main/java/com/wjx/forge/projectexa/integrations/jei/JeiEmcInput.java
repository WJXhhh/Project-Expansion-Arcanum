package com.wjx.forge.projectexa.integrations.jei;

import com.wjx.forge.projectexa.mixin.JeiCommandUtilAccessor;
import com.wjx.forge.projectexa.config.JeiEmcClientState;
import com.wjx.forge.projectexa.util.Util;
import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.common.Internal;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import mezz.jei.common.config.GiveMode;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.gui.util.CommandUtil;
import com.wjx.forge.projectexa.net.PacketHandler;
import com.wjx.forge.projectexa.net.packets.to_server.PacketJeiTransmute;
import com.wjx.forge.projectexa.net.packets.to_server.PacketOpenArcaneTransmutationTablet;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.InputType;
import mezz.jei.gui.input.handlers.SameElementInputHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.math.BigInteger;
import java.util.Optional;

public final class JeiEmcInput {
    private JeiEmcInput() { }

    public static Optional<IUserInputHandler> handle(IUserInputHandler handler, Screen screen, UserInput input,
            IInternalKeyMappings keys, CombinedRecipeFocusSource source, IIngredientManager ingredients,
            IClientToggleState toggleState, CommandUtil commandUtil) {
        if (!JeiEmcClientState.isEnabled() || !(screen instanceof AbstractContainerScreen<?>)) return Optional.empty();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !PacketOpenArcaneTransmutationTablet.hasTablet(minecraft.player)) return Optional.empty();
        IClientConfig config = ((JeiCommandUtilAccessor) (Object) commandUtil).projectexa$getClientConfig();
        boolean stackKey;
        boolean oneKey;
        // JEI's configured cheat key mappings require the cheat-mode context.
        // Enable that context only for matching, then immediately restore it.
        toggleState.setCheatItemsEnabled(true);
        try {
            stackKey = input.is(keys.getCheatItemStack());
            oneKey = !stackKey && input.is(keys.getCheatOneItem());
        } finally {
            toggleState.setCheatItemsEnabled(false);
        }
        int destination = config.getGiveMode() == GiveMode.MOUSE_PICKUP ? -1 : -2;
        boolean single = oneKey;
        if (!stackKey && !oneKey) {
            if (!config.isCheatToHotbarUsingHotkeysEnabled()) return Optional.empty();
            destination = -3;
            for (int slot = 0; slot < 9; slot++) {
                if (input.is(minecraft.options.keyHotbarSlots[slot])) { destination = slot; break; }
            }
            if (destination == -3) return Optional.empty();
        }
        final int target = destination;
        final boolean one = single;
        boolean recipeClick = input.getKey().getType() == InputConstants.Type.MOUSE
                && (input.getKey().getValue() == 0 || input.getKey().getValue() == 1);
        return source.getIngredientUnderMouse(input, keys)
                .filter(clicked -> !clicked.getCheatItemStack(ingredients).isEmpty())
                .findFirst().filter(clicked -> {
                    ItemStack stack = clicked.getCheatItemStack(ingredients);
                    if (!recipeClick) return true;
                    ItemStack clean = Util.cleanStack(stack);
                    IKnowledgeProvider provider = Util.getKnowledgeProvider(minecraft.player);
                    if (clean.isEmpty() || provider == null || !provider.hasKnowledge(clean)) return false;
                    long value = IEMCProxy.INSTANCE.getValue(clean);
                    if (value <= 0) return false;
                    if (provider.getEmc().compareTo(BigInteger.valueOf(value)) < 0) {
                        // JEI selects its recipe handler during SIMULATE and later executes that handler.
                        // Report on the initial mouse press without consuming the input.
                        if (input.getInputType() != InputType.EXECUTE) {
                            minecraft.player.sendSystemMessage(Component.translatable(
                                    "text.projectexa.jei_transmute.insufficient_emc", Long.toString(value))
                                    .withStyle(ChatFormatting.RED));
                        }
                        return false;
                    }
                    return true;
                })
                .map(clicked -> {
                    if (!input.isSimulate()) {
                        ItemStack stack = clicked.getCheatItemStack(ingredients);
                        sendTransmutation(screen, stack, target, one, false);
                    }
                    return new SameElementInputHandler(handler, clicked::isMouseOver);
                });
    }

    public static boolean tryRsiMiddleClick(Screen screen, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!JeiEmcClientState.isEnabled() || !(screen instanceof AbstractContainerScreen<?>)
                || minecraft.player == null || !PacketOpenArcaneTransmutationTablet.hasTablet(minecraft.player)) {
            return false;
        }
        IClientToggleState toggleState = Internal.getClientToggleState();
        if (toggleState.isCheatItemsEnabled()) return false;
        var keys = Internal.getKeyMappings();
        var middle = InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        boolean stackKey;
        boolean oneKey;
        toggleState.setCheatItemsEnabled(true);
        try {
            stackKey = keys.getCheatItemStack().isActiveAndMatches(middle);
            oneKey = !stackKey && keys.getCheatOneItem().isActiveAndMatches(middle);
        } finally {
            toggleState.setCheatItemsEnabled(false);
        }
        if (!stackKey && !oneKey) return false;
        GiveMode giveMode = Internal.getJeiClientConfigs().getClientConfig().getGiveMode();
        sendTransmutation(screen, stack, giveMode == GiveMode.MOUSE_PICKUP ? -1 : -2, oneKey, true);
        return true;
    }

    private static void sendTransmutation(Screen screen, ItemStack stack, int destination,
                                           boolean single, boolean pullFromRsi) {
        var menu = Minecraft.getInstance().player.containerMenu;
        boolean creativeCursor = destination == -1 && screen instanceof CreativeModeInventoryScreen;
        ItemStack carried = creativeCursor ? menu.getCarried().copy() : ItemStack.EMPTY;
        PacketHandler.sendToServer(new PacketJeiTransmute(stack.copyWithCount(1), menu.containerId,
                destination, single, creativeCursor, carried, pullFromRsi));
    }
}
