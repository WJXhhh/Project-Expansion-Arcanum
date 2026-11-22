package com.wjx.neoforge.projectexa;

import com.mojang.authlib.GameProfile;
import com.wjx.neoforge.projectexa.config.Config;
import com.wjx.neoforge.projectexa.gui.container.ContainerArcaneTransmutationTablet;
import com.wjx.neoforge.projectexa.net.packets.to_client.PacketJeiEmcConfig;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneSmithingTransfer;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletRecipeTransfer;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import com.wjx.neoforge.projectexa.util.Util;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.emc.EMCMappingHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
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
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.neoforge.common.ModConfigSpec;
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
    private static final ResourceLocation LARGE_SMITHING_RECIPE = Main.rl("smithing_transfer_test");
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

        var filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu, false);
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
            assertRejectsCandidateList(helper, buffer, codec, new PacketArcaneTransmutationTabletRecipeTransfer(
                    LARGE_RECIPE, List.of(candidates), true));
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
        var filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu, false);
        helper.assertTrue(menu.onRecipeTransfer(TORCH, List.of(filtered, List.of(), List.of(), List.of(stick.copy())), false),
                "Inventory material after the cutoff must transfer with no EMC");
        helper.assertTrue(inventory.getItem(0).isEmpty(), "The coal must now be in the grid");
        filtered = RecipeTransferUtil.filterAvailableCandidates(candidates, menu, false);
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
            assertRejectsCandidateList(helper, buffer, codec,
                    new PacketArcaneSmithingTransfer(menu.containerId, NETHERITE_PICKAXE, oversized, false, -1));

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

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void shiftSmithingPrioritizesOwnedAndCheapCandidates(GameTestHelper helper) {
        var coal = new ItemStack(Items.COAL);
        var iron = new ItemStack(Items.IRON_INGOT);
        var emerald = new ItemStack(Items.EMERALD, 4);
        long coalCost = IEMCProxy.INSTANCE.getValue(coal);
        var distinct = ItemStackLinkedSet.createTypeAndComponentsSet();
        var alternatives = new ArrayList<ItemStack>();
        var otherInputs = List.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.DIAMOND_PICKAXE);
        EMCMappingHandler.getMappedItems().stream().map(ItemInfo::createStack).map(Util::cleanStack)
                .filter(stack -> !otherInputs.contains(stack.getItem()))
                .filter(stack -> !stack.is(Items.IRON_INGOT) && !stack.is(Items.EMERALD))
                .filter(stack -> IEMCProxy.INSTANCE.getValue(stack) > coalCost)
                .filter(distinct::add).limit(280).forEachOrdered(alternatives::add);
        helper.assertValueEqual(alternatives.size(), 280, "Distinct higher-cost smithing candidates");
        alternatives.addAll(List.of(iron, emerald.copyWithCount(1), coal));

        var recipe = new SmithingTransformRecipe(Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(Items.DIAMOND_PICKAXE), Ingredient.of(alternatives.stream()),
                new ItemStack(Items.NETHERITE_PICKAXE));
        var recipeManager = helper.getLevel().getRecipeManager();
        var originalRecipes = List.copyOf(recipeManager.getRecipes());
        var recipes = new ArrayList<>(originalRecipes);
        recipes.add(new RecipeHolder<>(LARGE_SMITHING_RECIPE, recipe));
        recipeManager.replaceRecipes(recipes);
        try {
            var menu = tablet(helper);
            var provider = menu.getProvider();
            provider.addKnowledge(new ItemStack(Items.SMITHING_TABLE));
            alternatives.stream().filter(stack -> !stack.is(Items.IRON_INGOT) && !stack.is(Items.EMERALD))
                    .forEach(provider::addKnowledge);

            var ordered = RecipeTransferUtil.prioritizeSmithingCandidates(alternatives, iron, List.of(iron, emerald),
                    recipe::isAdditionIngredient, provider, false);
            helper.assertValueEqual(ordered.size(), 256, "Shift selection limit");
            helper.assertTrue(ordered.get(0).is(Items.IRON_INGOT), "The planned option must remain first");
            helper.assertTrue(ordered.get(1).is(Items.EMERALD), "A second owned option after the cutoff must survive");
            helper.assertTrue(ordered.get(2).is(Items.COAL), "The cheapest learned option must follow the owned items");
            var full = RecipeTransferUtil.prioritizeSmithingCandidates(alternatives, iron, List.of(iron, emerald),
                    recipe::isAdditionIngredient, provider, true);
            helper.assertValueEqual(full.size(), alternatives.size(), "All distinct options survive with the limit disabled");

            var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 4);
            var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamageValue(12);
            var inventory = menu.getPlayer().getInventory();
            inventory.setItem(0, template.copy());
            inventory.setItem(1, pickaxe.copy());
            inventory.setItem(2, iron.copy());
            menu.getSlot(ContainerArcaneTransmutationTablet.SMITHING + 2).set(emerald.copy());

            // A loaded grid contributes its real ingredients when cleared for the new transfer.
            helper.assertTrue(menu.transferSmithingRecipe(recipe, List.of(List.of(template), List.of(pickaxe), ordered), true),
                    "Shift transfer must create a result from the prioritized list");
            helper.assertTrue(menu.getSlot(ContainerArcaneTransmutationTablet.SMITHING + 2).getItem().is(Items.IRON_INGOT),
                    "The planned real ingredient must be used first");
            helper.assertValueEqual(provider.getEmc(), BigInteger.ZERO, "Owned material can be transferred without EMC");
            helper.assertValueEqual(menu.getSlot(ContainerArcaneTransmutationTablet.SMITHING + 3).getItem().getDamageValue(),
                    12, "Shift transfer preserves base durability");

            var emcMenu = tablet(helper);
            emcMenu.getProvider().addKnowledge(new ItemStack(Items.SMITHING_TABLE));
            emcMenu.getProvider().addKnowledge(coal);
            emcMenu.getProvider().setEmc(BigInteger.valueOf(coalCost * 4));
            var emcInventory = emcMenu.getPlayer().getInventory();
            emcInventory.setItem(0, template.copy());
            emcInventory.setItem(1, pickaxe.copy());
            helper.assertTrue(emcMenu.transferSmithingRecipe(recipe, List.of(List.of(template), List.of(pickaxe), ordered), true),
                    "Shift transfer must also work using the cheap EMC alternative");
            var addition = emcMenu.getSlot(ContainerArcaneTransmutationTablet.SMITHING + 2).getItem();
            helper.assertTrue(addition.is(Items.COAL), "Shift transfer must choose coal");
            helper.assertValueEqual(addition.getCount(), 4, "Shift transfer fills multiple cheap additions");
            helper.assertValueEqual(emcMenu.getProvider().getEmc(), BigInteger.ZERO, "Bulk EMC charge");
        } finally {
            recipeManager.replaceRecipes(originalRecipes);
        }
        helper.succeed();
    }

    @GameTest(batch = BATCH, templateNamespace = "minecraft", template = EMPTY_TEMPLATE)
    public static void configuredTransfersAllowMoreThan256Candidates(GameTestHelper helper) {
        var setting = Config.server.allowLargeRecipeTransferCandidates;
        boolean previous = setting.get();
        var menu = tablet(helper);
        var inventory = menu.getPlayer().getInventory();
        var coal = new ItemStack(Items.COAL);
        var stick = new ItemStack(Items.STICK);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            setting.set(true);
            Config.server.Spec.resetCaches(ModConfigSpec.RestartType.WORLD);
            var configCodec = PacketJeiEmcConfig.STREAM_CODEC;
            configCodec.encode(buffer, new PacketJeiEmcConfig(true, false, setting.get()));
            boolean allowLargeCandidates = configCodec.decode(buffer).allowLargeRecipeTransferCandidates();
            helper.assertTrue(allowLargeCandidates, "The client config packet must carry the server setting");
            buffer.clear();

            inventory.setItem(0, coal.copy());
            inventory.setItem(1, stick.copy());
            var candidates = new ArrayList<>(Collections.nCopies(256, new ItemStack(Items.DIAMOND)));
            candidates.add(coal);
            var crafting = RecipeTransferUtil.filterAvailableCandidates(candidates, menu, allowLargeCandidates);
            var craftingCodec = PacketArcaneTransmutationTabletRecipeTransfer.STREAM_CODEC;
            craftingCodec.encode(buffer, new PacketArcaneTransmutationTabletRecipeTransfer(TORCH,
                    List.of(crafting, List.of(), List.of(), List.of(stick)), false));
            var decodedCrafting = craftingCodec.decode(buffer);
            helper.assertValueEqual(decodedCrafting.recipe().getFirst().size(), 257, "Configured crafting packet size");
            helper.assertTrue(menu.onRecipeTransfer(decodedCrafting.recipeId(), decodedCrafting.recipe(), false),
                    "Configured crafting transfer must make a result using the final candidate");
            buffer.clear();

            menu.getProvider().addKnowledge(new ItemStack(Items.SMITHING_TABLE));
            var template = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            var pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            var ingot = new ItemStack(Items.NETHERITE_INGOT);
            inventory.setItem(0, template.copy());
            inventory.setItem(1, pickaxe.copy());
            inventory.setItem(2, ingot.copy());
            var smithingCandidates = new ArrayList<>(Collections.nCopies(256, new ItemStack(Items.DIAMOND)));
            smithingCandidates.add(template);
            var smithingCodec = PacketArcaneSmithingTransfer.STREAM_CODEC;
            smithingCodec.encode(buffer, new PacketArcaneSmithingTransfer(menu.containerId, NETHERITE_PICKAXE,
                    List.of(smithingCandidates, List.of(pickaxe), List.of(ingot)), true, -1));
            var decodedSmithing = smithingCodec.decode(buffer);
            var smithing = (SmithingRecipe) helper.getLevel().getRecipeManager().byKey(NETHERITE_PICKAXE).orElseThrow().value();
            helper.assertValueEqual(decodedSmithing.choices().getFirst().size(), 257, "Configured smithing packet size");
            helper.assertTrue(menu.transferSmithingRecipe(smithing, decodedSmithing.choices(), decodedSmithing.transferAll()),
                    "Configured smithing transfer must make a result using the final candidate");
        } finally {
            buffer.release();
            setting.set(previous);
            Config.server.Spec.resetCaches(ModConfigSpec.RestartType.WORLD);
        }
        helper.succeed();
    }

    private static <T> void assertRejectsCandidateList(GameTestHelper helper, RegistryFriendlyByteBuf buffer,
            StreamCodec<RegistryFriendlyByteBuf, T> codec, T packet) {
        codec.encode(buffer, packet);
        try {
            codec.decode(buffer);
            helper.fail("The default server codec must reject an oversized candidate list");
        } catch (DecoderException expected) {
            buffer.clear();
        }
    }

    private static ContainerArcaneTransmutationTablet tablet(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "recipe-transfer"));
        var provider = Objects.requireNonNull(Util.getKnowledgeProvider(player));
        var menu = new ContainerArcaneTransmutationTablet(1, player.getInventory(), provider);
        player.containerMenu = menu;
        return menu;
    }
}
