package com.wjx.forge.projectexa.registries;

import com.wjx.forge.projectexa.capability.IAlchemicalBookLocationsProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class Capabilities {
    public static final Capability<IAlchemicalBookLocationsProvider> ALCHEMICAL_BOOK_LOCATIONS = CapabilityManager.get(new CapabilityToken<>(){});
}
