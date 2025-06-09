package cool.furry.mc.neoforge.projectexpansion.registries;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.capability.IAlchemicalBookLocationsProvider;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;

public class Capabilities {
    public static final EntityCapability<IAlchemicalBookLocationsProvider, Void> ALCHEMICAL_BOOK_LOCATIONS_ENTITY = EntityCapability.createVoid(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
    public static final ItemCapability<IAlchemicalBookLocationsProvider, Void> ALCHEMICAL_BOOK_LOCATIONS_ITEM = ItemCapability.createVoid(Main.rl("alchemical_book_locations"), IAlchemicalBookLocationsProvider.class);
}
