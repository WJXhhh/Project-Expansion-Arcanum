package com.wjx.neoforge.projectexa.gui.container;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.wjx.neoforge.projectexa.gui.container.slots.PXCraftingSlot;
import com.wjx.neoforge.projectexa.gui.container.slots.PXInputSlot;
import com.wjx.neoforge.projectexa.gui.container.slots.PXOutputSlot;
import com.wjx.neoforge.projectexa.gui.container.slots.PXResultSlot;
import com.wjx.neoforge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import com.wjx.neoforge.projectexa.registries.MenuTypes;
import com.wjx.neoforge.projectexa.util.ContainerData;
import com.wjx.neoforge.projectexa.util.RecipeTransferUtil;
import com.wjx.neoforge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.container.PEHandContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.*;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

public class ContainerArcaneTransmutationTablet extends PEHandContainer {
    public static final int STATION_PANEL_X = -144;
    public static final int STATION_PANEL_Y = 10;
    public static final int STATION_PANEL_WIDTH = 140;
    public static final int STATION_PANEL_HEIGHT = 124;
    public static final int STONE_RECIPE_X = STATION_PANEL_X + 37;
    public static final int STONE_RECIPE_Y = STATION_PANEL_Y + 23;
    public static final int STONE_COLUMNS = 4;
    public static final int STONE_ROWS = 3;
    public static final int STONE_CELL = 18;

    public static int stationSlotX(int station, int slot) {
        if (station == 1) return STATION_PANEL_X + (slot == 3 ? 115 : 11 + slot * 18);
        if (station == 2) return STATION_PANEL_X + (slot == 0 ? 11 : 115);
        return STATION_PANEL_X + 11 + slot * 52;
    }

    public static int stationSlotY(int station, int slot) {
        return STATION_PANEL_Y + switch (station) {
            case 1 -> 99;
            case 2 -> slot == 0 ? 31 : 101;
            default -> 65;
        };
    }

    private static final int[] ROTATION_SLOTS = {0, 1, 2, 5, 8, 7, 6, 3};
    private final List<SlotInput> inputSlots = new ArrayList<>();
    private final List<PXOutputSlot> outputSlots = new ArrayList<>();
    public final TransmutationInventory transmutationInventory;
    private TransientCraftingContainer craftSlots;
    private ResultContainer resultSlots;
    private SlotUnlearn unlearn;
    private static final int INPUT = 0;
    private static final int INPUT_COUNT = 8;
    private static final int LOCK = 8;
    private static final int CONSUME = 9;
    private static final int UNLEARN = 10;
    private static final int OUTPUT = 11;
    private static final int OUTPUT_COUNT = 16;
    private static final int PLAYER = 27;
    private static final int PLAYER_COUNT = 36;
    private static final int CRAFTING = 64;
    private static final int RESULT = 63;
    private static final int CRAFTING_COUNT = 9;
    public static final int SMITHING = 73;
    private static final int STONECUTTING = 77;
    private static final int ANVIL = 79;
    private final SmithingMenu smithing;
    private final StonecutterMenu stonecutter;
    private final AnvilMenu anvil;
    private int station;
    private int smithingTransferSequence;
    private boolean craftingPanelVisible = true;
    public int lockX, lockY, consumeX, consumeY, unlearnX, unlearnY;
    private final Player player;
    private final IKnowledgeProvider provider;
    public boolean isCrafting = false;
    public boolean skipRefill = false;

    public static ContainerArcaneTransmutationTablet fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        @Nullable IKnowledgeProvider provider = playerInv.player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (provider == null) throw new NullPointerException("provider is null");
        ContainerData data = ContainerData.decode(buf);
        return new ContainerArcaneTransmutationTablet(windowId, playerInv, provider, data.hand().orElse(null), data.selected());
    }

    public ContainerArcaneTransmutationTablet(int windowId, Inventory playerInv, IKnowledgeProvider provider) {
        this(windowId, playerInv, provider, null, 0);
    }

    public ContainerArcaneTransmutationTablet(int windowId, Inventory playerInv, IKnowledgeProvider provider, @Nullable InteractionHand hand, int selected) {
        super(new ContainerTypeRegistryObject<>(MenuTypes.ARCANE_TRANSMUTATION_TABLET.getKey()), windowId, playerInv, hand, selected);
        this.player = playerInv.player;
        this.provider = provider;
        this.transmutationInventory = new TransmutationInventory(playerInv.player);
        this.smithing = new SmithingMenu(windowId, playerInv);
        this.stonecutter = new StonecutterMenu(windowId, playerInv);
        this.anvil = new AnvilMenu(windowId, playerInv);
        initSlots();
        addDataSlot(new DataSlot() {
            public int get() { return anvil.getCost(); }
            public void set(int value) { anvil.setMaximumCost(value); }
        });
        addDataSlot(new DataSlot() {
            public int get() { return station; }
            public void set(int value) {
                station = value;
                craftingPanelVisible = value == 0;
            }
        });
        addDataSlot(new DataSlot() {
            public int get() { return smithingTransferSequence; }
            public void set(int value) { smithingTransferSequence = value; }
        });
    }

    private void initSlots() {
        addSlot(new PXInputSlot(transmutationInventory, INPUT, 30, 21));
        addSlot(new PXInputSlot(transmutationInventory, 1, 130,21));
        addSlot(new PXInputSlot(transmutationInventory, 2, 8, 43));
        addSlot(new PXInputSlot(transmutationInventory, 3, 152, 43));
        addSlot(new PXInputSlot(transmutationInventory, 4, 8, 94));
        addSlot(new PXInputSlot(transmutationInventory, 5, 152, 94));
        addSlot(new PXInputSlot(transmutationInventory, 6, 30, 115));
        addSlot(new PXInputSlot(transmutationInventory, 7, 130, 115));
        addSlot(new SlotLock(transmutationInventory, LOCK, lockX = 80, lockY = 68));
        addSlot(new SlotConsume(transmutationInventory, CONSUME, consumeX = 152, consumeY = 115));
        addSlot(unlearn = new SlotUnlearn(transmutationInventory, UNLEARN, unlearnX = 8, unlearnY = 115));
        addSlot(new PXOutputSlot(transmutationInventory, OUTPUT, 80, 20));
        addSlot(new PXOutputSlot(transmutationInventory, 12, 105, 26));
        addSlot(new PXOutputSlot(transmutationInventory, 13, 55, 26));
        addSlot(new PXOutputSlot(transmutationInventory, 14, 123, 44));
        addSlot(new PXOutputSlot(transmutationInventory, 15, 37, 44));
        addSlot(new PXOutputSlot(transmutationInventory, 16, 128, 68));
        addSlot(new PXOutputSlot(transmutationInventory, 17, 32, 68));
        addSlot(new PXOutputSlot(transmutationInventory, 18, 123, 92));
        addSlot(new PXOutputSlot(transmutationInventory, 19, 37, 92));
        addSlot(new PXOutputSlot(transmutationInventory, 20, 105, 110));
        addSlot(new PXOutputSlot(transmutationInventory, 21, 55, 110));
        addSlot(new PXOutputSlot(transmutationInventory, 22, 80, 116));
        addSlot(new PXOutputSlot(transmutationInventory, 23, 60, 48));
        addSlot(new PXOutputSlot(transmutationInventory, 24, 100, 48));
        addSlot(new PXOutputSlot(transmutationInventory, 25, 60, 88));
        addSlot(new PXOutputSlot(transmutationInventory, 26, 100, 88));
        addPlayerInventory(8, 135);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        addSlot(new PXResultSlot(player, craftSlots, resultSlots, this, 0, -23, 75) {
            @Override
            public boolean isActive() {
                return craftingPanelVisible;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new PXCraftingSlot(craftSlots, column + row * 3, -59 + column * 18, 17 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return craftingPanelVisible;
                    }
                });
            }
        }
        for (int i = 0; i < 4; i++) addStationSlot(smithing.getSlot(i), 1, i);
        for (int i = 0; i < 2; i++) addStationSlot(stonecutter.getSlot(i), 2, i);
        for (int i = 0; i < 3; i++) addStationSlot(anvil.getSlot(i), 3, i);
    }

    private void addStationSlot(Slot original, int stationId, int stationSlot) {
        addSlot(new Slot(original.container, original.getSlotIndex(), stationSlotX(stationId, stationSlot), stationSlotY(stationId, stationSlot)) {
            @Override public boolean isActive() { return isStation(stationId); }
            @Override public boolean mayPlace(ItemStack stack) { return original.mayPlace(stack); }
            @Override public boolean mayPickup(Player player) { return original.mayPickup(player); }
            @Override public void onTake(Player player, ItemStack stack) { original.onTake(player, stack); }
            @Override public void onQuickCraft(ItemStack stack, ItemStack originalStack) { original.onQuickCraft(stack, originalStack); }
        });
    }

    public void selectStation(int value) {
        // -1 closes every side panel while preserving its ingredients.
        if (value < -1 || value > 3 || value >= 0 && !canUseStation(value)) return;
        station = value;
        craftingPanelVisible = value == 0;
    }

    public static Item stationItem(int value) {
        return switch (value) {
            case 0 -> Items.CRAFTING_TABLE;
            case 1 -> Items.SMITHING_TABLE;
            case 2 -> Items.STONECUTTER;
            case 3 -> Items.ANVIL;
            default -> Items.AIR;
        };
    }

    public boolean canUseStation(int value) {
        return value == 0 || value > 0 && value <= 3
                && provider.hasKnowledge(new ItemStack(stationItem(value)));
    }

    public boolean isStation(int value) { return station == value && canUseStation(value); }
    public int getStation() { return station; }
    public int getSmithingTransferSequence() { return smithingTransferSequence; }

    public boolean transferSmithingRecipe(SmithingRecipe recipe, List<List<ItemStack>> choices, boolean transferAll) {
        boolean allowLargeCandidates = RecipeTransferUtil.serverAllowsLargeCandidates();
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player)
                || !canUseStation(1) || choices == null || choices.size() != 3
                || choices.stream().anyMatch(options -> options == null
                        || !allowLargeCandidates && options.size() > RecipeTransferUtil.MAX_CANDIDATES_PER_SLOT)) return false;
        selectStation(1);
        smithingTransferSequence = (smithingTransferSequence + 1) & 0x7FFF;
        for (int index = 0; index < 3; index++) {
            Slot slot = slots.get(SMITHING + index);
            if (slot.hasItem()) slot.set(Util.returnToInventory(playerInv, player, slot.getItem(), false));
        }
        int passes = transferAll ? 64 : 1;
        for (int pass = 0; pass < passes; pass++) {
            boolean supplied = false;
            for (int index = 0; index < 3; index++) {
                supplied |= supplySmithingSlot(recipe, index, choices.get(index), !transferAll);
            }
            if (!supplied) break;
        }
        playerInv.setChanged();
        provider.syncEmc(serverPlayer);
        smithing.createResult();
        broadcastChanges();
        return recipe.matches(new SmithingRecipeInput(slots.get(SMITHING).getItem(),
                slots.get(SMITHING + 1).getItem(), slots.get(SMITHING + 2).getItem()), player.level())
                && slots.get(SMITHING + 3).hasItem();
    }

    private boolean supplySmithingSlot(SmithingRecipe recipe, int index, List<ItemStack> choices, boolean exactChoices) {
        Slot slot = slots.get(SMITHING + index);
        ItemStack existing = slot.getItem();
        if (!existing.isEmpty() && existing.getCount() >= slot.getMaxStackSize(existing)) return false;
        List<ItemStack> candidates = choices.stream().filter(stack -> !stack.isEmpty()).map(Util::cleanStack)
                .filter(stack -> !stack.isEmpty() && isSmithingIngredient(recipe, index, stack) && slot.mayPlace(stack))
                .sorted(Comparator.comparingLong(IEMCProxy.INSTANCE::getValue)).toList();

        // Use real items first and preserve their damage, enchantments and other components.
        for (ItemStack candidate : choices) {
            if (candidate.isEmpty()) continue;
            for (int inventorySlot = 0; inventorySlot < playerInv.getContainerSize(); inventorySlot++) {
                ItemStack stack = playerInv.getItem(inventorySlot);
                if (stack.isEmpty() || !slot.mayPlace(stack) || !isSmithingIngredient(recipe, index, stack)
                        || candidate.getItem() != stack.getItem()
                        || (exactChoices && !ItemStack.isSameItemSameComponents(candidate, stack))
                        || (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack))) continue;
                ItemStack added = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty()) playerInv.setItem(inventorySlot, ItemStack.EMPTY);
                if (existing.isEmpty()) slot.set(added);
                else { existing.grow(1); slot.setChanged(); }
                return true;
            }
        }
        for (ItemStack candidate : candidates) {
            if ((!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, candidate))
                    || !provider.hasKnowledge(candidate)) continue;
            long value = IEMCProxy.INSTANCE.getValue(candidate);
            if (value <= 0 || transmutationInventory.getAvailableEmc().compareTo(BigInteger.valueOf(value)) < 0) continue;
            transmutationInventory.removeEmc(BigInteger.valueOf(value));
            if (existing.isEmpty()) slot.set(candidate);
            else { existing.grow(1); slot.setChanged(); }
            return true;
        }
        return false;
    }

    private static boolean isSmithingIngredient(SmithingRecipe recipe, int index, ItemStack stack) {
        return switch (index) {
            case 0 -> recipe.isTemplateIngredient(stack);
            case 1 -> recipe.isBaseIngredient(stack);
            default -> recipe.isAdditionIngredient(stack);
        };
    }

    public StonecutterMenu getStonecutter() { return stonecutter; }
    public AnvilMenu getAnvil() { return anvil; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return isStation(2) && id >= 0 && id < stonecutter.getNumRecipes()
                && stonecutter.clickMenuButton(player, id);
    }

    public void setCraftingPanelVisible(boolean visible) {
        craftingPanelVisible = visible;
    }

    @Override
    protected Slot addSlot(Slot slot) {
        switch (slot) {
            case PXInputSlot inputSlot -> this.inputSlots.add(inputSlot);
            case PXOutputSlot outputSlot -> this.outputSlots.add(outputSlot);
            default -> {}
        }
        return super.addSlot(slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;

        for (int index : new int[] {SMITHING, SMITHING + 1, SMITHING + 2, STONECUTTING, ANVIL, ANVIL + 1}) {
            ItemStack stack = slots.get(index).remove(Integer.MAX_VALUE);
            if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
                if (!stack.isEmpty()) player.drop(stack, false);
            } else {
                Util.returnToInventory(playerInv, player, stack, true);
            }
        }

        // The result is only a preview; ingredients are consumed by ResultSlot.onTake.
        resultSlots.clearContent();
        resultSlots.setRecipeUsed(null);

        ItemStack unlearnStack = unlearn.getItem().copy();
        unlearn.set(ItemStack.EMPTY);
        ItemStack carriedStack = getCarried().copy();
        setCarried(ItemStack.EMPTY);
        if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            if (!unlearnStack.isEmpty()) player.drop(unlearnStack, false);
            if (!carriedStack.isEmpty()) player.drop(carriedStack, false);
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.removeItemNoUpdate(i);
                if (!stack.isEmpty()) player.drop(stack, false);
            }
        } else {
            // The unlearn slot already removed this item's knowledge. Return the physical item.
            Util.returnToInventory(playerInv, player, unlearnStack, true);
            Util.returnToInventoryOrTransmutation(playerInv, player, provider, carriedStack, false, true);
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                Util.returnToInventoryOrTransmutation(playerInv, player, provider, craftSlots.removeItemNoUpdate(i), false, true);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot currentStationSlot = tryGetSlot(slotIndex);
        if (currentStationSlot == null || !currentStationSlot.isActive()) return ItemStack.EMPTY;
        if (slotIndex >= SMITHING && slotIndex < slots.size()) {
            if (!currentStationSlot.hasItem() || !currentStationSlot.mayPickup(player)) return ItemStack.EMPTY;
            ItemStack moving = currentStationSlot.getItem();
            ItemStack before = moving.copy();
            if (!moveItemStackTo(moving, PLAYER, PLAYER + PLAYER_COUNT, true)) return ItemStack.EMPTY;
            currentStationSlot.onQuickCraft(moving, before);
            if (moving.isEmpty()) currentStationSlot.set(ItemStack.EMPTY);
            else currentStationSlot.setChanged();
            if (moving.getCount() == before.getCount()) return ItemStack.EMPTY;
            currentStationSlot.onTake(player, moving);
            return before;
        }
        if (this.tryGetSlot(slotIndex) instanceof PXCraftingSlot slot && slot.hasItem()) {
            ItemStack original = slot.getItem().copy();
            ItemStack remainder = original.copy();
            if (!this.moveItemStackTo(remainder, PLAYER, PLAYER + PLAYER_COUNT, true)) return ItemStack.EMPTY;
            slot.set(remainder);
            slot.setChanged();
            return original;
        }

        if (this.tryGetSlot(slotIndex) instanceof PXResultSlot || slotIndex == RESULT) {
            Slot slot = this.tryGetSlot(slotIndex);
            if (slot != null && slot.hasItem()) {
                ItemStack itemStack1 = slot.getItem();
                ItemStack itemStack = itemStack1.copy();
                itemStack1.getItem().onCraftedBy(itemStack, player.level(), player);
                if (!this.moveItemStackTo(itemStack1, PLAYER, PLAYER + PLAYER_COUNT, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack1, itemStack);
                if (itemStack1.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }

                if (itemStack1.getCount() == itemStack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.onTake(player, itemStack1);
                if (slotIndex == 0) {
                    player.drop(itemStack1, false);
                }

                return itemStack;
            }
        }

        if ((slotIndex >= INPUT && slotIndex < (INPUT + INPUT_COUNT)) || slotIndex == LOCK || slotIndex == UNLEARN) {
            //Input Slots, lock slot, and unlearn slot, defer to super (allow basic sneak clicking out of container)
            return super.quickMoveStack(player, slotIndex);
        }

        Slot currentSlot = tryGetSlot(slotIndex);
        if (currentSlot == null || !currentSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= OUTPUT && slotIndex < (OUTPUT + OUTPUT_COUNT)) {
            ItemStack stack = currentSlot.getItem().copy();
            long itemEmc = IEMCProxy.INSTANCE.getValue(stack);
            if (itemEmc <= 0 || !provider.hasKnowledge(stack)) return ItemStack.EMPTY;

            var handler = new net.neoforged.neoforge.items.wrapper.InvWrapper(player.getInventory());

            stack.setCount(stack.getMaxStackSize());
            int room = stack.getCount() - ItemHelper.simulateFit(player.getInventory().items, stack);
            if (room <= 0) return ItemStack.EMPTY;

            BigInteger unitCost = BigInteger.valueOf(itemEmc);
            BigInteger affordable = transmutationInventory.getAvailableEmc().divide(unitCost);
            int count = affordable.compareTo(BigInteger.valueOf(room)) >= 0 ? room : affordable.intValue();
            if (count <= 0) return ItemStack.EMPTY;

            stack.setCount(count);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            int inserted = count - remainder.getCount();
            if (inserted > 0 && transmutationInventory.isServer()) {
                transmutationInventory.removeEmc(unitCost.multiply(BigInteger.valueOf(inserted)));
            }
        } else if (slotIndex >= (OUTPUT + OUTPUT_COUNT) && slotIndex < CRAFTING) {
            ItemStack slotStack = currentSlot.getItem();
            ItemStack stackToInsert = slotStack;
            if (stackToInsert.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY) != null) {
                //We are in the main inventory or the hot bar and are handling an item that can store EMC
                //Start by trying to insert it into the input slots, first attempting to stack with other items
                stackToInsert = insertItem(inputSlots, stackToInsert, true);
                if (slotStack.getCount() == stackToInsert.getCount()) {
                    //Then as long as if we still have the same number of items (failed to insert), try to insert it into the input slots allowing for empty items
                    stackToInsert = insertItem(inputSlots, stackToInsert, false);
                }
                if (slotStack.getCount() != stackToInsert.getCount()) {
                    return transferSuccess(currentSlot, player, slotStack, stackToInsert);
                }
            }
            //Else if we failed to do that also, transfer to the learn slot if the item has EMC
            long emc = IEMCProxy.INSTANCE.getSellValue(stackToInsert);
            if (emc > 0 || stackToInsert.getItem() instanceof Tome) {
                if (transmutationInventory.isServer()) {
                    BigInteger emcBigInt = BigInteger.valueOf(emc);
                    transmutationInventory.handleKnowledge(stackToInsert);
                    transmutationInventory.addEmc(emcBigInt.multiply(BigInteger.valueOf(stackToInsert.getCount())));
                }
                currentSlot.set(ItemStack.EMPTY);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clickPostValidate(int slotIndex, int dragType, ClickType clickType, Player player) {
        if (player.level().isClientSide && transmutationInventory.getHandlerForSlot(slotIndex) == transmutationInventory.outputs && tryGetSlot(slotIndex) instanceof Slot slot) {
            PacketDistributor.sendToServer(new SearchUpdatePKT(transmutationInventory.getIndexFromSlot(slotIndex), slot.getItem()));
        }
        super.clickPostValidate(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive() && !(slot instanceof SlotConsume || slot instanceof SlotUnlearn || slot instanceof SlotInput || slot instanceof SlotLock || slot instanceof SlotOutput);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && !slots.get(slotId).isActive()) return;
        if (clickType == ClickType.QUICK_MOVE) skipRefill = true;
        super.clicked(slotId, dragType, clickType, player);
        if (clickType == ClickType.QUICK_MOVE) skipRefill = false;
    }

    @Override
    public void slotsChanged(Container container) {
        slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots);
        super.slotsChanged(container);
    }

    protected static void slotChangedCraftingGrid(ContainerArcaneTransmutationTablet menu, Level level, Player player, CraftingContainer crafting, ResultContainer result) {
        if (!level.isClientSide) {
            CraftingInput input = CraftingInput.of(3, 3, crafting.getItems());
            RegistryAccess registryAccess = level.registryAccess();
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ItemStack stack = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> optional = ModList.get().isLoaded("polymorph")
                    ? com.wjx.neoforge.projectexa.integrations.polymorph.PolymorphIntegration.getRecipe(menu, level, player, input)
                    : (Objects.requireNonNull(level.getServer())).getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (optional.isPresent()) {
                RecipeHolder<CraftingRecipe> recipe = optional.get();
                if (result.setRecipeUsed(level, serverPlayer, recipe)) {
                    stack = recipe.value().assemble(input, registryAccess);
                }
            } else {
                result.setRecipeUsed(null);
            }

            result.setItem(0, stack);
            menu.setRemoteSlot(RESULT, stack);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), RESULT, stack));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && slot.index != SMITHING + 3 && slot.index != STONECUTTING + 1
                && slot.index != ANVIL + 2 && slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public boolean onRecipeTransfer(ResourceLocation recipeId, List<List<ItemStack>> recipe, boolean transferAll) {
        if (!(player instanceof ServerPlayer) || !stillValid(player) || recipe == null || recipe.size() > 9
                || recipe.stream().anyMatch(items -> items == null)) return false;
        var selected = player.level().getRecipeManager().byKey(recipeId).orElse(null);
        if (selected == null || !(selected.value() instanceof CraftingRecipe craftingRecipe)
                || !craftingRecipe.canCraftInDimensions(3, 3)) return false;
        selectStation(0);
        recipe = recipe.stream().map(items -> (List<ItemStack>) new ArrayList<>(items)).toList();
        clearCrafting(false);
        fillCraftingSlots(recipe, transferAll);
        // Filling fires intermediate grid changes. Apply the requested selection only once it is complete.
        boolean matches = craftingRecipe.matches(CraftingInput.of(3, 3, craftSlots.getItems()), player.level());
        if (ModList.get().isLoaded("polymorph") && matches) {
            com.wjx.neoforge.projectexa.integrations.polymorph.PolymorphIntegration.selectRecipe(
                    player, new RecipeHolder<>(selected.id(), craftingRecipe));
            slotsChanged(craftSlots);
        }
        return matches && !resultSlots.getItem(0).isEmpty();
    }

    public void fillCraftingSlots(List<List<ItemStack>> recipe, boolean transferAll) {
        int max = Math.min(recipe.size(), craftSlots.getContainerSize());
        transferItems(recipe, max);

        if (transferAll) {
            for (int i = 0; i < 63; i++) {
                transferItems(recipe, max);
            }
        }

        if (player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotChangedCraftingGrid(this, this.player.level(), this.player, this.craftSlots, this.resultSlots);
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean transferFromTablet(int i, List<ItemStack> possibilities) {
        if (!possibilities.isEmpty()) {
            possibilities.sort(Comparator.comparingLong(IEMCProxy.INSTANCE::getValue));
        }

        for (ItemStack stack : possibilities) {
            ItemStack stack1 = Util.cleanStack(stack);
            if (provider.hasKnowledge(stack1)) {
                long value = IEMCProxy.INSTANCE.getValue(stack1);

                if (value > 0L && provider.getEmc().compareTo(BigInteger.valueOf(value)) >= 0) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, stack1);
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && slotItem.getItem() == stack1.getItem() && Util.areStacksEqual(player.registryAccess(), slotItem, stack1)) {
                        slotItem.grow(1);
                    } else {
                        continue;
                    }

                    provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(value)));
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean transferFromInventory(int i, List<ItemStack> possibilities) {
        for (ItemStack possibility : possibilities) {
            ItemStack fixedp = Util.cleanStack(possibility);
            for (int j = 0; j < playerInv.getContainerSize(); ++j) {
                ItemStack stack = playerInv.getItem(j);
                if (Util.areStacksEqual(player.registryAccess(), fixedp, Util.cleanStack(stack))) {
                    ItemStack slotItem = craftSlots.getItem(i);

                    if (slotItem.isEmpty()) {
                        craftSlots.setItem(i, stack.copyWithCount(1));
                    } else if (slotItem.getCount() < slotItem.getMaxStackSize() && Util.areStacksEqual(player.registryAccess(), slotItem, stack)) {
                        slotItem.grow(1);
                    } else {
                        continue;
                    }

                    stack.shrink(1);

                    if (stack.isEmpty()) {
                        playerInv.setItem(j, ItemStack.EMPTY);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public void transferItems(List<List<ItemStack>> recipe, int max) {
        for (int i = 0; i < max; ++i) {
            if (recipe.get(i) != null && !recipe.get(i).isEmpty()) {
                // One requested unit per pass, with real inventory taking precedence over EMC.
                if (!transferFromInventory(i, recipe.get(i))) transferFromTablet(i, recipe.get(i));
            }
        }

    }

    /** @apiNote only call on server */
    public void clearCrafting(boolean force) {
        boolean emcUpdate = false;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) continue;

            if (ProjectEConfig.server.difficulty.covalenceLoss.get() >= 1D && IEMCProxy.INSTANCE.hasValue(stack)) {
                Util.AddKnowledgeResult result = Util.addKnowledge(player, provider, stack);

                if (!result.fail()) {
                    BigInteger emc = provider.getEmc().add(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack)).multiply(BigInteger.valueOf(stack.getCount())));
                    provider.setEmc(emc);
                    emcUpdate = true;
                    craftSlots.setItem(i, ItemStack.EMPTY);
                    continue;
                }
            }

            ItemStack result = Util.returnToInventory(playerInv, player, stack, force);
            craftSlots.setItem(i, result);
        }

        if (emcUpdate && player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void rotateCrafting(boolean clockwise) {
        ItemStack[] stacks = new ItemStack[ROTATION_SLOTS.length];

        if (clockwise) {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                int j = i - 1;

                if (j < 0) j = ROTATION_SLOTS.length - 1;
                stacks[i] = craftSlots.getItem(ROTATION_SLOTS[j % ROTATION_SLOTS.length]);
            }
        } else {
            for (int i = 0; i < ROTATION_SLOTS.length; i++) {
                stacks[i] = craftSlots.getItem(ROTATION_SLOTS[(i + 1) % ROTATION_SLOTS.length]);
            }
        }

        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            craftSlots.setItem(ROTATION_SLOTS[i], stacks[i]);
        }

        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void balanceCrafting() {
        ArrayListMultimap<CompoundTag, ItemStack> map = ArrayListMultimap.create();
        Multiset<CompoundTag> itemCount = HashMultiset.create();

        RegistryAccess registryAccess = player.registryAccess();
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
                CompoundTag tag = (CompoundTag) stack.save(registryAccess, new CompoundTag());
                tag.remove("count");
                map.put(tag, stack);
                itemCount.add(tag, stack.getCount());
            }
        }

        for (CompoundTag tag : map.keySet()) {
            List<ItemStack> list = map.get(tag);
            int totalCount = itemCount.count(tag);
            int countPerStack = totalCount / list.size();
            int restCount = totalCount % list.size();

            for (ItemStack stack : list) stack.setCount(countPerStack);

            int idx = 0;
            while (restCount > 0) {
                ItemStack stack = list.get(idx);

                if (stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    restCount--;
                }

                idx++;

                if (idx >= list.size()) {
                    idx = 0;
                }
            }
        }


        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    /** @apiNote only call on server */
    public void spreadCrafting() {
        while (true) {
            ItemStack biggestStack = null;
            int biggestSize = 1;

            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.getItem(i);

                if (!stack.isEmpty() && stack.getCount() > biggestSize) {
                    biggestStack = stack;
                    biggestSize = stack.getCount();
                }
            }

            if (biggestStack == null) {
                return;
            }

            boolean emptyBiggestSlot = false;

            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                ItemStack stack = craftSlots.getItem(i);

                if (stack.isEmpty()) {
                    if (biggestStack.getCount() > 1) {
                        craftSlots.setItem(i, biggestStack.split(1));
                    } else {
                        emptyBiggestSlot = true;
                    }
                }
            }

            if (!emptyBiggestSlot) {
                break;
            }
        }

        balanceCrafting();
    }

    /** @apiNote only call on client */
    public void action(PacketArcaneTransmutationTabletSmallButton.Action action) {
        PacketDistributor.sendToServer(new PacketArcaneTransmutationTabletSmallButton(action));
    }

    public IKnowledgeProvider getProvider() {
        return provider;
    }

    public Player getPlayer() {
        return player;
    }

    public List<ItemStack> getCraftingInputs() {
        return List.copyOf(craftSlots.getItems());
    }

    public ImmutableList<PXOutputSlot> getOutputSlots() {
        return ImmutableList.copyOf(outputSlots);
    }
}
