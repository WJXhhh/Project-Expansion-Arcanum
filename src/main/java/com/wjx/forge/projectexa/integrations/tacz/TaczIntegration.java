package com.wjx.forge.projectexa.integrations.tacz;

import com.wjx.forge.projectexa.mixin.TaczGunSmithTableMenuAccessor;
import com.wjx.forge.projectexa.util.Util;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.List;

/** Server-side TACZ integration. All validation happens again on the server. */
public final class TaczIntegration {
    private static final int MAX_INPUTS = 64;

    private TaczIntegration() {
    }

    public static void transmute(ServerPlayer player, ResourceLocation recipeId,
                                 List<ItemStack> displayedIngredients) {
        if (recipeId == null || displayedIngredients == null || displayedIngredients.size() > MAX_INPUTS
                || !(player.containerMenu instanceof GunSmithTableMenu menu) || !menu.stillValid(player)
                || player.level().getServer() == null) {
            return;
        }

        GunSmithTableRecipe recipe = ((TaczGunSmithTableMenuAccessor) (Object) menu)
                .projectexa$getRecipe(recipeId, player.level().getServer().getRecipeManager());
        if (recipe == null || recipe.getInputs().size() != displayedIngredients.size()) {
            return;
        }

        IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
        if (provider == null) {
            return;
        }

        TransmutationInventory transmutation = new TransmutationInventory(player);
        for (int index = 0; index < recipe.getInputs().size(); index++) {
            GunSmithTableIngredient input = recipe.getInputs().get(index);
            ItemStack displayed = displayedIngredients.get(index);
            if (displayed == null || displayed.isEmpty() || !input.getIngredient().test(displayed)) {
                continue;
            }

            ItemStack item = Util.cleanStack(displayed);
            if (item.isEmpty() || !input.getIngredient().test(item) || !provider.hasKnowledge(item)) {
                continue;
            }

            long value = IEMCProxy.INSTANCE.getValue(item);
            if (value <= 0 || input.getCount() <= 0) {
                continue;
            }

            BigInteger unitCost = BigInteger.valueOf(value);
            int affordable = Util.safeIntValue(transmutation.getAvailableEmc().divide(unitCost));
            int count = Math.min(input.getCount(), affordable);
            if (count <= 0) {
                continue;
            }

            transmutation.removeEmc(unitCost.multiply(BigInteger.valueOf(count)));
            Util.returnToInventory(player.getInventory(), player, item.copyWithCount(count), true);
        }
    }
}
