package com.wjx.neoforge.projectexa;

import com.mojang.authlib.GameProfile;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneSmithingTransfer;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import com.wjx.neoforge.projectexa.util.Util;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(Main.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeTransferGameTests {
    private static final String BATCH = "recipe_transfer";
    private static final String EMPTY_TEMPLATE = "bastion/mobs/empty";
    private static final ResourceLocation LARGE_RECIPE = Main.rl("recipe_transfer_test");
    private static final ResourceLocation TORCH = ResourceLocation.withDefaultNamespace("torch");
    private static final ResourceLocation NETHERITE_PICKAXE = ResourceLocation.withDefaultNamespace("netherite_pickaxe_smithing");

    @BeforeBatch(batch = BATCH)
    public static void loadEmcValues(ServerLevel level) {
        // ProjectE initializes its EMC map on datapack sync, which normally follows player login.
        NeoForge.EVENT_BUS.post(new OnDatapackSyncEvent(level.getServer().getPlayerList(), null));
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void largeCraftingListKeepsCheapestEmc(GameTestHelper helper) {
        var menu = tablet(helper);
        var provider = menu.getProvider();
        var coal = new ItemStack(Items.COAL);
        long coalCost = IEMCProxy.INSTANCE.getValue(coal);
        helper.assertTrue(coalCost > 0, "Coal must have an EMC value");

        var distinct = ItemStackLinkedSet.createTypeAndComponentsSet();
        var candidates = new ArrayList<ItemStack>();
        EMCMappingHandler.getMappedItems().stream().map(ItemInfo::createStack).map(Util::cleanStack)
                .filter(stack -> IEMCProxy.INSTANCE.getValue(stack) > coalCost)
                .filter(distinct::add).limit(280).forEachOrdered(candidates::add);
        helper.assertValueEqual(candidates.size(), 280, "Distinct higher-cost candidates");
        candidates.add(coal);
        candidates.forEach(provider::addKnowledge);
        provider.setEmc(BigInteger.valueOf(Long.MAX_VALUE));

        var filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu);
        helper.assertValueEqual(filtered.size(), 256, "Crafting candidate limit");
        helper.assertTrue(filtered.getFirst().is(Items.COAL), "The cheapest learned option must survive truncation");

        var inventory = menu.getPlayer().getInventory();
        inventory.setItem(0, new ItemStack(Items.STICK, 4));
        provider.setEmc(BigInteger.valueOf(coalCost * 4));
        var packet = new PacketArcaneTransmutationTabletRecipeTransfer(LARGE_RECIPE,
                List.of(filtered, List.of(new ItemStack(Items.STICK))), true);
        var recipeManager = helper.getLevel().getRecipeManager();
        var originalRecipes = List.copyOf(recipeManager.getRecipes());
        var recipes = new ArrayList<>(originalRecipes);
        var ingredients = NonNullList.of(Ingredient.EMPTY, Ingredient.of(candidates.stream()), Ingredient.of(Items.STICK));
        recipes.add(new RecipeHolder<>(LARGE_RECIPE,
                new ShapelessRecipe("", CraftingBookCategory.MISC, new ItemStack(Items.TORCH), ingredients)));
        recipeManager.replaceRecipes(recipes);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var codec = PacketArcaneTransmutationTabletRecipeTransfer.STREAM_CODEC;
            codec.encode(buffer, packet);
            var decoded = codec.decode(buffer);
            helper.assertTrue(menu.onRecipeTransfer(decoded.recipeId(), decoded.recipe(), decoded.transferAll()),
                    "A large recipe must transfer through its packet and produce a result");
        } finally {
            buffer.release();
            recipeManager.replaceRecipes(originalRecipes);
        }
        helper.assertValueEqual(menu.getCraftingInputs().getFirst().getCount(), 4, "Bulk coal count");
        helper.assertValueEqual(provider.getEmc(), BigInteger.ZERO, "Only the cheapest EMC is charged");
        helper.succeed();
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void largeCraftingListKeepsOwnedItems(GameTestHelper helper) {
        var menu = tablet(helper);
        var inventory = menu.getPlayer().getInventory();
        var coal = new ItemStack(Items.COAL);
        var stick = new ItemStack(Items.STICK);
        inventory.setItem(0, coal);
        inventory.setItem(1, stick.copy());
        var candidates = new ArrayList<>(Collections.nCopies(270, new ItemStack(Items.DIAMOND)));
        candidates.add(coal.copy());
        var filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu);
        helper.assertTrue(menu.onRecipeTransfer(TORCH, List.of(filtered, List.of(), List.of(), List.of(stick.copy())), false),
                "Inventory material after the cutoff must transfer with no EMC");
        helper.assertTrue(inventory.getItem(0).isEmpty(), "The coal must now be in the grid");
        filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu);
        helper.assertTrue(filtered.stream().anyMatch(stack -> stack.is(Items.COAL)),
                "Material already in the grid must remain a candidate when transferring again");
        helper.assertTrue(menu.onRecipeTransfer(TORCH, List.of(filtered, List.of(), List.of(), List.of(stick.copy())), false),
                "A second transfer must reuse the material from the grid");
        helper.succeed();
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void smithingLimitsCandidates(GameTestHelper helper) {
        var menu = tablet(helper);
        menu.getProvider().addKnowledge(new ItemStack(Items.SMITHING_TABLE));
        var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.setDamageValue(12);
        var ingot = new ItemStack(Items.NETHERITE_INGOT);
        var inventory = menu.getPlayer().getInventory();
        inventory.setItem(0, template.copy());
        inventory.setItem(1, pickaxe.copy());
        inventory.setItem(2, ingot.copy());
        var recipe = (SmithingRecipe) helper.getLevel().getRecipeManager().byKey(NETHERITE_PICKAXE).orElseThrow().value();
        var oversized = List.of(Collections.nCopies(257, template), List.of(pickaxe), List.of(ingot));
        helper.assertFalse(menu.transferSmithingRecipe(recipe, oversized, false), "The menu must reject 257 candidates");
        helper.assertValueEqual(menu.getSmithingTransferSequence(), 0, "A rejected transfer must leave the menu sequence intact");

        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var codec = PacketArcaneSmithingTransfer.STREAM_CODEC;
            try {
                codec.encode(buffer, new PacketArcaneSmithingTransfer(menu.containerId, NETHERITE_PICKAXE, oversized, false, -1));
                helper.fail("The smithing packet must reject 257 candidates");
            } catch (EncoderException expected) {
                buffer.clear();
            }

            var valid = List.of(Collections.nCopies(256, template), List.of(pickaxe), List.of(ingot));
            codec.encode(buffer, new PacketArcaneSmithingTransfer(menu.containerId, NETHERITE_PICKAXE, valid, false, -1));
            var decoded = codec.decode(buffer);
            helper.assertValueEqual(decoded.choices().getFirst().size(), 256, "Smithing candidate limit");
            helper.assertTrue(menu.transferSmithingRecipe(recipe, decoded.choices(), decoded.transferAll()),
                    "Smithing must accept 256 candidates and create a result");
        } finally {
            buffer.release();
        }
        var result = menu.getSlot(ContainerArcaneTransmutationTablet.SMITHING + 3).getItem();
        helper.assertTrue(result.is(Items.NETHERITE_PICKAXE), "Smithing result");
        helper.assertValueEqual(result.getDamageValue(), 12, "Existing tool damage");
        helper.succeed();
    }

    private static ContainerArcaneTransmutationTablet tablet(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "recipe-transfer"));
        var provider = Objects.requireNonNull(Util.getKnowledgeProvider(player));
        var menu = new ContainerArcaneTransmutationTablet(1, player.getInventory(), provider);
        player.containerMenu = menu;
        return menu;
    }
}
