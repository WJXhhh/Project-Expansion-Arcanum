package com.wjx.neoforge.projectexa;

import com.wjx.neoforge.projectexa.config.Config;
import com.wjx.neoforge.projectexa.net.PacketHandler;
import com.wjx.neoforge.projectexa.registries.*;
import com.wjx.neoforge.projectexa.util.*;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;

@Mod(Main.MOD_ID)
public class Main {
    public static final String MOD_ID = "projectexa";
    @SuppressWarnings("unused")
    public static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();
    @SuppressWarnings("unused")
    public static ModContainer MOD_CONTAINER;
    private static Main INSTANCE;

    private final PacketHandler packetHandler;

    public Main(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
        MOD_CONTAINER = modContainer;

        AttachmentTypes.Registry.register(modEventBus);
        Attributes.Registry.register(modEventBus);
        BlockEntityTypes.Registry.register(modEventBus);
        Blocks.Registry.register(modEventBus);
        BlockTypes.Registry.register(modEventBus);
        DataComponentTypes.Registry.register(modEventBus);
        Items.Registry.register(modEventBus);
        SoundEvents.Registry.register(modEventBus);
        MenuTypes.Registry.register(modEventBus);
        CreativeTabs.Registry.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        Config.register(modContainer);

        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
        AdvancedAlchemicalChest.register();

        this.packetHandler = new PacketHandler(modEventBus, modContainer.getModInfo().getVersion());
    }

    @SuppressWarnings("unused")
    public static PacketHandler packetHandler() {
        return INSTANCE.packetHandler;
    }

    private void serverTick(ServerTickEvent.Post event) {
        AlchemicalCollectionCollector.process();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (event.getServer().getTickCount() % 20 == 0) {
                boolean changed = false;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    ItemStack warehouse = player.getInventory().getItem(slot);
                    if (warehouse.getItem() instanceof com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse
                            && com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse.refreshAvailability(warehouse)) {
                        changed = true;
                        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(-2, 0, slot, warehouse));
                    }
                }
                var curios = com.wjx.neoforge.projectexa.integrations.curios.CuriosIntegration.getAll(player);
                if (curios != null) {
                    for (int slot = 0; slot < curios.getSlots(); slot++) {
                        ItemStack warehouse = curios.getStackInSlot(slot);
                        if (warehouse.getItem() instanceof com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse) {
                            com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse.refreshAvailability(warehouse);
                        }
                    }
                }
                if (changed) player.getInventory().setChanged();
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem().equals(Items.INFINITE_FUEL.get()) && !stack.has(DataComponentTypes.OWNER.get())) {
                    stack.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()));
                    continue;
                }
                boolean hasEnch = EnchantmentHelper.getTagEnchantmentLevel(event.getServer().registryAccess().holderOrThrow(Enchantments.ALCHEMICAL_COLLECTION), stack) > 0;
                if(hasEnch && !stack.has(PEDataComponentTypes.ACTIVE)) {
                    stack.set(PEDataComponentTypes.ACTIVE, true);
                }
            }
        }
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
