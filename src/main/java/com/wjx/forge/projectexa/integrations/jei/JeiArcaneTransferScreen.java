package com.wjx.forge.projectexa.integrations.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only transfer context for recipes opened from non-container screens.
 * Never displayed or installed as the player's menu. Its distinct menu class
 * selects the arcane fallback instead of vanilla's 2x2 inventory transfer.
 */
public final class JeiArcaneTransferScreen extends AbstractContainerScreen<AbstractContainerMenu> {
    private final Player player;

    public JeiArcaneTransferScreen(Player player) {
        super(new TransferMenu(), player.getInventory(), Component.empty());
        this.player = player;
    }

    public boolean isFor(Player player) {
        return this.player == player;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // This screen supplies a JEI transfer context only; it is never rendered.
    }

    private static final class TransferMenu extends AbstractContainerMenu {
        private TransferMenu() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    }
}
