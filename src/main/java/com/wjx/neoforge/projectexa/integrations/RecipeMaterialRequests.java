package com.wjx.neoforge.projectexa.integrations;

import com.wjx.neoforge.projectexa.integrations.arsnouveau.ArsNouveauRecipeIntegration;
import com.wjx.neoforge.projectexa.integrations.create.CreateRecipeIntegration;
import com.wjx.neoforge.projectexa.integrations.goety.GoetyRecipeIntegration;
import com.wjx.neoforge.projectexa.integrations.jei.ArsNouveauJeiRecipeTypes;
import com.wjx.neoforge.projectexa.integrations.jei.CreateJeiRecipeTypes;
import com.wjx.neoforge.projectexa.integrations.jei.GoetyJeiRecipeTypes;
import com.wjx.neoforge.projectexa.util.ArcaneTabletAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.ModList;
import java.util.List;

/** Optional adapters have common-only method signatures and are linked only after their mod guard. */
public final class RecipeMaterialRequests {
    private RecipeMaterialRequests() {}
    public static boolean supported(ResourceLocation category) {
        if (category == null || !ModList.get().isLoaded(category.getNamespace())) return false;
        return switch (category.getNamespace()) {
            case "ars_nouveau" -> ArsNouveauJeiRecipeTypes.isSupported(category);
            case "create" -> CreateJeiRecipeTypes.isSupported(category);
            case "goety" -> GoetyJeiRecipeTypes.isSupported(category);
            default -> false;
        };
    }

    public static void handle(ServerPlayer player, ResourceLocation category, ResourceLocation id, ItemStack contextItem) {
        if (!player.server.isSameThread() || !supported(category) || !ArcaneTabletAccess.hasTablet(player)) return;
        var manager = player.server.getRecipeManager();
        var holder = manager.byKey(id).orElse(null);
        List<Ingredient> ingredients;
        if ("goety".equals(category.getNamespace()) && "brewing".equals(category.getPath())) {
            ingredients = GoetyRecipeIntegration.brewingIngredients(manager, contextItem);
        } else if ("create".equals(category.getNamespace())) {
            ingredients = CreateRecipeIntegration.resolve(player, holder, category, id, contextItem);
        } else if (holder == null) return;
        else if ("ars_nouveau".equals(category.getNamespace())) ingredients = ArsNouveauRecipeIntegration.ingredients(holder.value(), category);
        else if ("goety".equals(category.getNamespace())) ingredients = GoetyRecipeIntegration.ingredients(holder.value(), category);
        else return;
        RecipeMaterialTransmutation.supply(player, ingredients);
    }
}
