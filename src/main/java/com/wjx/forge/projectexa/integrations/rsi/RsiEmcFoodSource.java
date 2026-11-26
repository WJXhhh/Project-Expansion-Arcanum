package com.wjx.forge.projectexa.integrations.rsi;

import com.huanghuang.rsintegration.api.RSIAutoEatFoodSources;
import com.huanghuang.rsintegration.api.RSIAutoEatFoodSources.Context;
import com.huanghuang.rsintegration.api.RSIAutoEatFoodSources.TakeResult;
import com.wjx.forge.projectexa.Main;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.List;

final class RsiEmcFoodSource implements RSIAutoEatFoodSources.Source {
    static void register() {
        if (!RSIAutoEatFoodSources.register(RsiEmcMaterialSource.ID, new RsiEmcFoodSource())) {
            Main.Logger.warn("RSI auto-eat food source {} is already registered", RsiEmcMaterialSource.ID);
        }
    }

    @Override
    public List<ItemStack> candidates(Context context) {
        IKnowledgeProvider provider = RsiEmcMaterialSource.provider(context.player());
        if (provider == null) return List.of();
        return RsiEmcMaterialSource.affordableOffers(provider, provider.getEmc(),
                item -> item.isEdible() && (context.wantedItems().isEmpty() || context.wantedItems().contains(item)),
                (template, price) -> template.copyWithCount(1));
    }

    @Override
    public TakeResult takeOne(Context context, ItemStack exactTemplate) {
        if (exactTemplate.getCount() != 1 || !exactTemplate.getItem().isEdible()) return new TakeResult.Unavailable();
        IKnowledgeProvider provider = RsiEmcMaterialSource.provider(context.player());
        if (provider == null) return new TakeResult.Unavailable();
        BigInteger price = BigInteger.valueOf(RsiEmcMaterialSource.unitValue(provider, exactTemplate));
        if (price.signum() <= 0 || provider.getEmc().compareTo(price) < 0) return new TakeResult.Unavailable();

        TakeResult.Taken serving = new TakeResult.Taken(exactTemplate.copy());
        return RsiEmcMaterialSource.debitEmc(context.player(), provider, price)
                ? serving : new TakeResult.Indeterminate();
    }
}
