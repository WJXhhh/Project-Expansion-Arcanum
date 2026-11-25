package com.wjx.neoforge.projectexa;

import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.mojang.authlib.GameProfile;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.wjx.neoforge.projectexa.integrations.tacz.TaczIntegration;
import com.wjx.neoforge.projectexa.item.ItemPersonalEmcWarehouse;
import com.wjx.neoforge.projectexa.registries.Items;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TaczGameTests {
    private static final String BATCH = "tacz";
    private static final String TEMPLATE = "bastion/mobs/empty";

    @BeforeBatch(batch = BATCH)
    public static void loadEmc(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(level.getServer().getPlayerList(), null));
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = TEMPLATE)
    public static void itemIdentityAndEmc(GameTestHelper helper) {
        ItemStack gun = output(helper, "gun/ump45");
        ItemStack ammo = output(helper, "ammo/45acp");
        ItemStack attachment = output(helper, "attachments/sniper_extended_mag_1");
        ItemStack otherAmmo = output(helper, "ammo/9mm");
        for (ItemStack item : List.of(gun, ammo, attachment, otherAmmo)) {
            long value = IEMCProxy.INSTANCE.getValue(item);
            helper.assertTrue(value > 0, "TaCZ recipe must produce positive EMC for " + item);
            helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(new ItemStack(item.getItem())), 0L, "Bare item EMC");

            ItemStack changed = item.copyWithCount(1);
            if (changed.getItem() instanceof IGun iGun) iGun.setCurrentAmmoCount(changed, 7);
            CustomData.update(DataComponents.CUSTOM_DATA, changed, tag -> tag.putString("TestSkin", "sample"));
            helper.assertValueEqual(IEMCProxy.INSTANCE.getValue(changed), value, "Modified TaCZ item EMC for " + item);
            helper.assertTrue(ItemStack.isSameItemSameComponents(Util.cleanStack(changed), Util.cleanStack(item)),
                    "Modified TaCZ item's learned form must match the crafted item");
        }
        helper.assertFalse(ItemStack.isSameItemSameComponents(Util.cleanStack(ammo), Util.cleanStack(otherAmmo)),
                "Different ammunition IDs must remain distinct");
        helper.succeed();
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = TEMPLATE)
    public static void tabletChecksRecipeAndPaysForDisplayedMaterial(GameTestHelper helper) {
        var player = player(helper);
        var knowledge = Util.getKnowledgeProvider(player);
        var recipeId = id("ammo/45acp");
        var inputs = recipe(helper, "ammo/45acp").getInputs();
        var first = inputs.getFirst();
        ItemStack material = first.getIngredient().getItems()[0];
        long value = IEMCProxy.INSTANCE.getValue(material);
        helper.assertTrue(value > 0, "Selected material must have an EMC value");
        knowledge.addKnowledge(material);
        int count = Math.min(2, first.getCount());
        BigInteger balance = BigInteger.valueOf(value * count);
        knowledge.setEmc(balance);

        var menu = new GunSmithTableMenu(12, player.getInventory(), id("ammo_workbench"));
        helper.assertTrue(menu.canSelectRecipe(recipeId, player.server.getRecipeManager()), "Ammo workbench accepts this recipe");
        player.containerMenu = menu;
        var displayed = new ArrayList<>(Collections.nCopies(inputs.size(), ItemStack.EMPTY));
        displayed.set(0, material);
        TaczIntegration.transmute(player, menu.containerId, recipeId, displayed);
        helper.assertValueEqual(knowledge.getEmc(), balance, "Request without a tablet");

        player.getInventory().setItem(8, new ItemStack(Items.ARCANE_TRANSMUTATION_TABLET.get()));
        TaczIntegration.transmute(player, 11, recipeId, displayed);
        helper.assertValueEqual(knowledge.getEmc(), balance, "Request for a closed menu");
        TaczIntegration.transmute(player, menu.containerId, id("missing_recipe"), displayed);
        helper.assertValueEqual(knowledge.getEmc(), balance, "Request for an unknown recipe");

        TaczIntegration.transmute(player, menu.containerId, recipeId, displayed);
        helper.assertValueEqual(player.getInventory().countItem(material.getItem()), count, "Purchased material count");
        helper.assertValueEqual(knowledge.getEmc(), BigInteger.ZERO, "Purchase cost");
        helper.succeed();
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = TEMPLATE)
    public static void playerAndMaidReloadFromWarehouses(GameTestHelper helper) throws ReflectiveOperationException {
        var owner = player(helper);
        var knowledge = Util.getKnowledgeProvider(owner);
        var maid = InitEntities.MAID.get().create(helper.getLevel());
        ItemStack gun = output(helper, "gun/ump45");
        ItemStack correct = Util.cleanStack(output(helper, "ammo/45acp"));
        ItemStack wrong = Util.cleanStack(output(helper, "ammo/9mm"));
        helper.assertTrue(((IAmmo) correct.getItem()).isAmmoOfGun(gun, correct), "Fixture must use the gun's ammunition");
        knowledge.addKnowledge(correct);
        knowledge.addKnowledge(wrong);
        BigInteger unitCost = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(correct));
        knowledge.setEmc(unitCost.multiply(BigInteger.TEN));

        // Mock players are not indexed for owner lookup. Register this test owner only for the assertions.
        var players = onlinePlayers(helper.getLevel().getServer().getPlayerList());
        players.put(owner.getUUID(), owner);
        try {
            ItemStack warehouse = new ItemStack(Items.PERSONAL_EMC_WAREHOUSE.get());
            ItemPersonalEmcWarehouse.bindTo(warehouse, owner);
            ItemPersonalEmcWarehouse.setFilter(warehouse, 0, ItemPersonalEmcWarehouse.sanitizeFilter(wrong));
            ItemPersonalEmcWarehouse.setFilter(warehouse, 1, ItemPersonalEmcWarehouse.sanitizeFilter(correct));

            owner.getInventory().setItem(0, warehouse);
            owner.getInventory().setItem(1, warehouse.copy());
            helper.assertValueEqual(TaczIntegration.countWarehouseAmmo(owner, gun, 9999), 10, "Shared warehouse balance");
            reload(helper, owner, gun);
            owner.getInventory().clearContent();
            maid.getMaidInv().setStackInSlot(0, warehouse);
            reload(helper, maid, gun);
            maid.getMaidInv().setStackInSlot(0, ItemStack.EMPTY);
            var slot = CuriosApi.getCuriosInventory(maid).orElseThrow().getStacksHandler("curio").orElseThrow().getStacks();
            helper.assertTrue(slot.isItemValid(0, warehouse), "Maid's Curios slot accepts the warehouse");
            slot.setStackInSlot(0, warehouse);
            reload(helper, maid, gun);
            helper.assertValueEqual(knowledge.getEmc(), unitCost, "Only matching ammo is charged to the bound owner");
            helper.succeed();
        } finally {
            players.remove(owner.getUUID());
        }
    }

    private static void reload(GameTestHelper helper, LivingEntity shooter, ItemStack gun) {
        var item = (AbstractGunItem) gun.getItem();
        helper.assertTrue(item.canReload(shooter, gun), "TaCZ must see warehouse ammo for " + shooter);
        helper.assertValueEqual(item.findAndExtractInventoryAmmo(shooter.getCapability(Capabilities.ItemHandler.ENTITY), gun, 3),
                3, "TaCZ reload extraction count");
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> onlinePlayers(PlayerList list) throws ReflectiveOperationException {
        var field = PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        return (Map<UUID, ServerPlayer>) field.get(list);
    }

    private static FakePlayer player(GameTestHelper helper) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tacz-test"));
    }

    private static GunSmithTableRecipe recipe(GameTestHelper helper, String path) {
        return (GunSmithTableRecipe) helper.getLevel().getRecipeManager().byKey(id(path)).orElseThrow().value();
    }

    private static ItemStack output(GameTestHelper helper, String path) {
        return recipe(helper, path).getResultItem(helper.getLevel().registryAccess());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("tacz", path);
    }
}
