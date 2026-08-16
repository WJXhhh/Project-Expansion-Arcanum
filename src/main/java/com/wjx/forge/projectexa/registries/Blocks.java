package com.wjx.forge.projectexa.registries;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.block.BlockCompactSun;
import com.wjx.forge.projectexa.block.BlockStoneTable;
import com.wjx.forge.projectexa.block.BlockTransmutationInterface;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class Blocks {
    public static final DeferredRegister<Block> Registry = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MOD_ID);

    public static final RegistryObject<BlockTransmutationInterface> TRANSMUTATION_INTERFACE = Registry.register("transmutation_interface", BlockTransmutationInterface::new);
    public static final RegistryObject<BlockCompactSun> COMPACT_SUN = Registry.register("compact_sun", BlockCompactSun::new);
    public static final RegistryObject<BlockStoneTable> STONE_TABLE = Registry.register("stone_table", BlockStoneTable::new);
}
