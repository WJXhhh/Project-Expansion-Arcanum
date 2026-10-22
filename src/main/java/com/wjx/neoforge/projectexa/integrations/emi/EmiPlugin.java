package com.wjx.neoforge.projectexa.integrations.emi;

import com.wjx.neoforge.projectexa.registries.MenuTypes;
import com.wjx.neoforge.projectexa.util.SearchSync;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class EmiPlugin implements dev.emi.emi.api.EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        SearchSync.register(new SearchSync("emi", EmiApi::setSearchText));
        registry.addRecipeHandler(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), new ArcaneCraftingTransferHandler());
    }
}
