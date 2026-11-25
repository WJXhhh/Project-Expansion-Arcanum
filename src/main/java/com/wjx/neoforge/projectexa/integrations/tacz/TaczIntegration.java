package com.wjx.neoforge.projectexa.integrations.tacz;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.wjx.neoforge.projectexa.item.EmcWarehouseInventoryHandler;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public final class TaczIntegration {
    private TaczIntegration() {}

    public static boolean isAmmo(ItemStack stack) {
        return stack.getItem() instanceof IAmmo;
    }

    public static int countWarehouseAmmo(Player player, ItemStack gun, int limit) {
        if (limit <= 0 || !(player.getCapability(Capabilities.ItemHandler.ENTITY)
                instanceof EmcWarehouseInventoryHandler warehouse)) return 0;
        return warehouse.countMatchingWarehouseItems(stack -> stack.getItem() instanceof IAmmo ammo
                && ammo.isAmmoOfGun(gun, stack), limit);
    }

    public static void transmute(ServerPlayer player, int menuId, ResourceLocation recipeId,
                                 List<ItemStack> displayedIngredients) {
        if (!player.server.isSameThread() || player.isSpectator() || !ArcaneTabletAccess.hasTablet(player)
                || !(player.containerMenu instanceof GunSmithTableMenu menu) || menu.containerId != menuId
                || !menu.stillValid(player)) return;

        var recipes = player.server.getRecipeManager();
        if (!menu.canSelectRecipe(recipeId, recipes)) return;
        var holder = recipes.byKey(recipeId).orElse(null);
        if (holder == null || !(holder.value() instanceof GunSmithTableRecipe recipe)
                || recipe.getInputs().size() != displayedIngredients.size()) return;

        var knowledge = Util.getKnowledgeProvider(player);
        if (knowledge == null) return;
        var transmutation = new TransmutationInventory(player);
        for (int i = 0; i < recipe.getInputs().size(); i++) {
            GunSmithTableIngredient input = recipe.getInputs().get(i);
            ItemStack displayed = displayedIngredients.get(i);
            if (displayed.isEmpty() || input.getCount() <= 0) continue;
            ItemStack selected = Arrays.stream(input.getIngredient().getItems())
                    .filter(option -> ItemStack.isSameItemSameComponents(option, displayed))
                    .findFirst().orElse(ItemStack.EMPTY);
            ItemStack item = Util.cleanStack(selected);
            if (item.isEmpty() || !input.getIngredient().test(item) || !knowledge.hasKnowledge(item)) continue;

            long value = IEMCProxy.INSTANCE.getValue(item);
            if (value <= 0) continue;
            BigInteger unitCost = BigInteger.valueOf(value);
            int affordable = Util.safeIntValue(transmutation.getAvailableEmc().divide(unitCost));
            int count = Math.min(input.getCount(), affordable);
            if (count <= 0) continue;
            transmutation.removeEmc(unitCost.multiply(BigInteger.valueOf(count)));
            Util.returnToInventory(player.getInventory(), player, item.copyWithCount(count), true);
        }
        knowledge.syncEmc(player);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
