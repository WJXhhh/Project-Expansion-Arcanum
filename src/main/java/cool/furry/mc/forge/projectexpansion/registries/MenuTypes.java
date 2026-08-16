package cool.furry.mc.forge.projectexpansion.registries;

import cool.furry.mc.forge.projectexpansion.Main;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerCollector;
import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeDeferredRegister;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> Registry = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Main.MOD_ID);

    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_1 = register("collector_tier_1", BlockEntityCollector.class, ContainerCollector.Tier1::new);
    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_2 = register("collector_tier_2", BlockEntityCollector.class, ContainerCollector.Tier2::new);
    public static final RegistryObject<MenuType<ContainerCollector>> COLLECTOR_TIER_3 = register("collector_tier_3", BlockEntityCollector.class, ContainerCollector.Tier3::new);
    public static final RegistryObject<MenuType<ContainerArcaneTransmutationTablet>> ARCANE_TRANSMUTATION_TABLET = Registry.register("arcane_transmutation_tablet", () -> IForgeMenuType.create(ContainerArcaneTransmutationTablet::fromNetwork));

    public static <CONTAINER extends AbstractContainerMenu, BE extends BlockEntity> RegistryObject<MenuType<CONTAINER>> register(String name,
                                                                                                                          Class<BE> blockEntityClass, ContainerTypeDeferredRegister.IBlockEntityContainerFactory<CONTAINER, BE> factory) {
        return Registry.register(name, () -> IForgeMenuType.create((id, inv, buf) -> factory.create(id, inv, getBlockEntityFromBuf(buf, blockEntityClass))));
    }

    private static <BE extends BlockEntity> BE getBlockEntityFromBuf(FriendlyByteBuf buf, Class<BE> type) {
        return DistExecutor.unsafeRunForDist(() -> () -> {
            BlockPos pos = buf.readBlockPos();
            BE blockEntity = WorldHelper.getBlockEntity(type, Minecraft.getInstance().level, pos);
            if (blockEntity == null) {
                throw new IllegalStateException("Client could not locate block entity at " + pos + " for block entity container. "
                        + "This is likely caused by a mod breaking client side block entity lookup");
            }
            return blockEntity;
        }, () -> () -> {
            throw new RuntimeException("Shouldn't be called on server!");
        });
    }
}
