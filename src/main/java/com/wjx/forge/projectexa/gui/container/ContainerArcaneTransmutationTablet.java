package com.wjx.forge.projectexa.gui.container;

import com.wjx.forge.projectexa.gui.container.slots.PXCraftingSlot;
import com.wjx.forge.projectexa.gui.container.slots.PXInputSlot;
import com.wjx.forge.projectexa.gui.container.slots.PXOutputSlot;
import com.wjx.forge.projectexa.gui.container.slots.PXResultSlot;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletOutputUpdate;
import com.wjx.forge.projectexa.net.packets.to_server.PacketArcaneTransmutationTabletSmallButton;
import com.wjx.forge.projectexa.registries.MenuTypes;
import com.wjx.forge.projectexa.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.container.PEHandContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotInput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotLock;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotOutput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotUnlearn;
import moze_intel.projecte.gameObjs.items.Tome;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ContainerArcaneTransmutationTablet extends PEHandContainer {
    private static final int[] ROTATION_SLOTS = {0, 1, 2, 5, 8, 7, 6, 3};
    private static final int INPUT = 0;
    private static final int INPUT_COUNT = 8;
    private static final int LOCK = 8;
    private static final int CONSUME = 9;
    private static final int UNLEARN = 10;
    private static final int OUTPUT = 11;
    private static final int OUTPUT_COUNT = 16;
    private static final int PLAYER = 27;
    private static final int PLAYER_COUNT = 36;
    private static final int RESULT = 63;
    private static final int CRAFTING = 64;
    private static final int CRAFTING_COUNT = 9;

    private final List<SlotInput> inputSlots = new ArrayList<>();
    private final List<PXOutputSlot> outputSlots = new ArrayList<>();
    public final TransmutationInventory transmutationInventory;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private SlotUnlearn unlearn;
    private final Player player;
    private final IKnowledgeProvider provider;

    public int lockX;
    public int lockY;
    public int consumeX;
    public int consumeY;
    public int unlearnX;
    public int unlearnY;
    public boolean isCrafting;
    public boolean skipRefill;

    public static ContainerArcaneTransmutationTablet fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf buffer) {
        boolean inHand = buffer.readBoolean();
        InteractionHand hand = inHand ? buffer.readEnum(InteractionHand.class) : null;
        int selected = buffer.readByte();
        return new ContainerArcaneTransmutationTablet(windowId, playerInv, hand, selected);
    }

    public ContainerArcaneTransmutationTablet(int windowId, Inventory playerInv, @Nullable InteractionHand hand, int selected) {
        super(new ContainerTypeRegistryObject<>(MenuTypes.ARCANE_TRANSMUTATION_TABLET), windowId, playerInv, hand, selected);
        this.player = playerInv.player;
        this.provider = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Player does not have ProjectE knowledge capability"));
        this.transmutationInventory = new TransmutationInventory(player);
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();
        initSlots();
    }

    private void initSlots() {
        addSlot(new PXInputSlot(transmutationInventory, 0, 30, 21));
        addSlot(new PXInputSlot(transmutationInventory, 1, 130, 21));
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
        addSlot(new PXResultSlot(player, craftSlots, resultSlots, this, 0, -23, 75));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new PXCraftingSlot(craftSlots, column + row * 3, -59 + column * 18, 17 + row * 18));
            }
        }
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (slot instanceof PXInputSlot inputSlot) inputSlots.add(inputSlot);
        if (slot instanceof PXOutputSlot outputSlot) outputSlots.add(outputSlot);
        return super.addSlot(slot);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;

        ItemStack unlearnStack = unlearn.getItem().copy();
        unlearn.set(ItemStack.EMPTY);
        ItemStack carriedStack = getCarried().copy();
        setCarried(ItemStack.EMPTY);
        if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
            dropStack(player, unlearnStack);
            dropStack(player, carriedStack);
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                dropStack(player, craftSlots.removeItemNoUpdate(i));
            }
            dropStack(player, resultSlots.removeItemNoUpdate(0));
            return;
        }

        // SlotUnlearn already removed this item's knowledge when it was placed in
        // the slot. Return it as-is; treating it as a transmutable stack here
        // would learn it again and consume the physical item into EMC on close.
        Util.returnToInventory(playerInv, player, unlearnStack, true);
        Util.returnToInventoryOrTransmutation(playerInv, player, provider, carriedStack, false, true);
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            Util.returnToInventoryOrTransmutation(playerInv, player, provider, craftSlots.removeItemNoUpdate(i), false, true);
        }
        Util.returnToInventoryOrTransmutation(playerInv, player, provider, resultSlots.removeItemNoUpdate(0), false, true);
    }

    private static void dropStack(Player player, ItemStack stack) {
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot currentSlot = tryGetSlot(slotIndex);
        if (currentSlot == null || !currentSlot.hasItem()) return ItemStack.EMPTY;

        if (slotIndex >= CRAFTING && slotIndex < CRAFTING + CRAFTING_COUNT) {
            ItemStack original = currentSlot.getItem().copy();
            ItemStack remainder = original.copy();
            if (!moveItemStackTo(remainder, PLAYER, PLAYER + PLAYER_COUNT, true)) return ItemStack.EMPTY;
            currentSlot.set(remainder);
            currentSlot.setChanged();
            return original;
        }

        if (slotIndex == RESULT) {
            return quickMoveResult(player, currentSlot);
        }

        if (slotIndex >= INPUT && slotIndex < INPUT + INPUT_COUNT || slotIndex == LOCK || slotIndex == UNLEARN) {
            return super.quickMoveStack(player, slotIndex);
        }

        if (slotIndex >= OUTPUT && slotIndex < OUTPUT + OUTPUT_COUNT) {
            return quickMoveOutput(player, currentSlot);
        }

        if (slotIndex >= PLAYER && slotIndex < PLAYER + PLAYER_COUNT) {
            return quickMovePlayerItem(player, currentSlot);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack quickMoveResult(Player player, Slot slot) {
        ItemStack before = slot.getItem().copy();
        ItemStack moving = before.copy();
        if (!moveItemStackTo(moving, PLAYER, PLAYER + PLAYER_COUNT, true)) return ItemStack.EMPTY;
        slot.onQuickCraft(moving, before);
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (moving.getCount() == before.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return before;
    }

    private ItemStack quickMoveOutput(Player player, Slot slot) {
        ItemStack output = slot.getItem().copy();
        if (!provider.hasKnowledge(output)) return ItemStack.EMPTY;
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0) return ItemStack.EMPTY;

        output.setCount(output.getMaxStackSize());
        int room = output.getCount() - ItemHelper.simulateFit(player.getInventory().items, output);
        if (room <= 0) return ItemStack.EMPTY;

        BigInteger unitCost = BigInteger.valueOf(value);
        BigInteger affordable = transmutationInventory.getAvailableEmc().divide(unitCost);
        int count = affordable.compareTo(BigInteger.valueOf(room)) >= 0 ? room : affordable.intValue();
        if (count <= 0) return ItemStack.EMPTY;

        BigInteger totalCost = unitCost.multiply(BigInteger.valueOf(count));
        if (transmutationInventory.isServer()) transmutationInventory.removeEmc(totalCost);
        output.setCount(count);
        ItemStack remainder = player.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .map(handler -> ItemHandlerHelper.insertItemStacked(handler, output, false))
                .orElse(output);
        if (!remainder.isEmpty() && transmutationInventory.isServer()) {
            transmutationInventory.addEmc(unitCost.multiply(BigInteger.valueOf(remainder.getCount())));
        }
        // Output slots are virtual and stay populated after a transfer. Returning the
        // generated stack here makes AbstractContainerMenu repeat QUICK_MOVE until the
        // inventory is full; an empty result correctly limits this click to one stack.
        return ItemStack.EMPTY;
    }

    private ItemStack quickMovePlayerItem(Player player, Slot slot) {
        ItemStack slotStack = slot.getItem();
        ItemStack stackToInsert = slotStack;
        if (stackToInsert.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
            stackToInsert = insertItem(inputSlots, stackToInsert, true);
            if (stackToInsert.getCount() == slotStack.getCount()) {
                stackToInsert = insertItem(inputSlots, stackToInsert, false);
            }
            if (stackToInsert.getCount() != slotStack.getCount()) {
                return transferSuccess(slot, player, slotStack, stackToInsert);
            }
        }

        long emc = IEMCProxy.INSTANCE.getSellValue(stackToInsert);
        if (emc > 0 || stackToInsert.getItem() instanceof Tome) {
            if (transmutationInventory.isServer()) {
                transmutationInventory.handleKnowledge(stackToInsert);
                transmutationInventory.addEmc(BigInteger.valueOf(emc).multiply(BigInteger.valueOf(stackToInsert.getCount())));
            }
            slot.set(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clickPostValidate(int slotIndex, int dragType, ClickType clickType, Player player) {
        if (player.level().isClientSide && slotIndex >= OUTPUT && slotIndex < OUTPUT + OUTPUT_COUNT) {
            Slot slot = tryGetSlot(slotIndex);
            if (slot instanceof PXOutputSlot) {
                PacketHandlerAccess.sendOutputUpdate(slotIndex - OUTPUT, slot.getItem());
            }
        }
        super.clickPostValidate(slotIndex, dragType, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !(slot instanceof SlotConsume || slot instanceof SlotUnlearn || slot instanceof SlotInput
                || slot instanceof SlotLock || slot instanceof SlotOutput);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (clickType == ClickType.QUICK_MOVE) skipRefill = true;
        super.clicked(slotId, dragType, clickType, player);
        if (clickType == ClickType.QUICK_MOVE) skipRefill = false;
    }

    @Override
    public void slotsChanged(Container container) {
        slotChangedCraftingGrid(this, player.level(), player, craftSlots, resultSlots);
        super.slotsChanged(container);
    }

    private static void slotChangedCraftingGrid(ContainerArcaneTransmutationTablet menu, Level level, Player player,
                                                 CraftingContainer crafting, ResultContainer result) {
        if (level.isClientSide || level.getServer() == null) return;
        ItemStack output = ItemStack.EMPTY;
        Optional<CraftingRecipe> recipe = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, crafting, level);
        if (recipe.isPresent()) {
            result.setRecipeUsed(recipe.get());
            output = recipe.get().assemble(crafting, level.registryAccess());
        } else {
            result.setRecipeUsed(null);
        }
        result.setItem(0, output);
        menu.setRemoteSlot(RESULT, output);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), RESULT, output));
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public void onRecipeTransfer(List<List<ItemStack>> recipe, boolean transferAll) {
        if (!player.level().isClientSide) {
            clearCrafting(false);
            fillCraftingSlots(recipe, transferAll);
        }
    }

    public void fillCraftingSlots(List<List<ItemStack>> recipe, boolean transferAll) {
        int max = Math.min(recipe.size(), craftSlots.getContainerSize());
        transferItems(recipe, max);
        if (transferAll) {
            for (int i = 0; i < 63; i++) transferItems(recipe, max);
        }
        if (player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotChangedCraftingGrid(this, player.level(), player, craftSlots, resultSlots);
    }

    private boolean transferFromTablet(int index, List<ItemStack> possibilities) {
        List<ItemStack> sortedPossibilities = new ArrayList<>(possibilities);
        sortedPossibilities.sort(Comparator.comparingLong(IEMCProxy.INSTANCE::getValue));
        for (ItemStack possibility : sortedPossibilities) {
            ItemStack clean = Util.cleanStack(possibility);
            if (!provider.hasKnowledge(clean)) continue;
            long value = IEMCProxy.INSTANCE.getValue(clean);
            if (value <= 0 || provider.getEmc().compareTo(BigInteger.valueOf(value)) < 0) continue;

            ItemStack existing = craftSlots.getItem(index);
            if (existing.isEmpty()) craftSlots.setItem(index, clean);
            else if (existing.getCount() < existing.getMaxStackSize() && Util.areStacksEqual(player.level().registryAccess(), existing, clean)) existing.grow(1);
            else continue;

            provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(value)));
            return true;
        }
        return false;
    }

    private boolean transferFromInventory(int index, List<ItemStack> possibilities) {
        for (ItemStack possibility : possibilities) {
            ItemStack clean = Util.cleanStack(possibility);
            for (int inventoryIndex = 0; inventoryIndex < playerInv.getContainerSize(); inventoryIndex++) {
                ItemStack stack = playerInv.getItem(inventoryIndex);
                if (!Util.areStacksEqual(player.level().registryAccess(), clean, Util.cleanStack(stack))) continue;

                ItemStack existing = craftSlots.getItem(index);
                if (existing.isEmpty()) craftSlots.setItem(index, stack.copyWithCount(1));
                else if (existing.getCount() < existing.getMaxStackSize() && Util.areStacksEqual(player.level().registryAccess(), existing, stack)) existing.grow(1);
                else continue;

                stack.shrink(1);
                if (stack.isEmpty()) playerInv.setItem(inventoryIndex, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private void transferItems(List<List<ItemStack>> recipe, int max) {
        for (int i = 0; i < max; i++) {
            if (i < recipe.size() && recipe.get(i) != null && !recipe.get(i).isEmpty()) transferFromInventory(i, recipe.get(i));
        }
        for (int i = 0; i < max; i++) {
            if (i < recipe.size() && recipe.get(i) != null && !recipe.get(i).isEmpty()) transferFromTablet(i, recipe.get(i));
        }
    }

    public void clearCrafting(boolean force) {
        if (player.level().isClientSide) return;
        boolean emcChanged = false;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty()) continue;

            if (ProjectEConfig.server.difficulty.covalenceLoss.get() >= 1D && IEMCProxy.INSTANCE.hasValue(stack)) {
                if (learnForTransmutation(stack)) {
                    BigInteger value = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(stack))
                            .multiply(BigInteger.valueOf(stack.getCount()));
                    provider.setEmc(provider.getEmc().add(value));
                    craftSlots.setItem(i, ItemStack.EMPTY);
                    emcChanged = true;
                    continue;
                }
            }
            craftSlots.setItem(i, Util.returnToInventory(playerInv, player, stack, force));
        }

        if (emcChanged && player instanceof ServerPlayer serverPlayer) provider.syncEmc(serverPlayer);
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    private boolean learnForTransmutation(ItemStack stack) {
        ItemStack cleanStack = Util.cleanStack(stack);
        if (!provider.hasKnowledge(cleanStack)) {
            transmutationInventory.handleKnowledge(stack);
        }
        return provider.hasKnowledge(cleanStack);
    }

    public void rotateCrafting(boolean clockwise) {
        if (player.level().isClientSide) return;
        ItemStack[] rotated = new ItemStack[ROTATION_SLOTS.length];
        for (int i = 0; i < ROTATION_SLOTS.length; i++) {
            int source = clockwise ? (i - 1 + ROTATION_SLOTS.length) % ROTATION_SLOTS.length : (i + 1) % ROTATION_SLOTS.length;
            rotated[i] = craftSlots.getItem(ROTATION_SLOTS[source]).copy();
        }
        for (int i = 0; i < ROTATION_SLOTS.length; i++) craftSlots.setItem(ROTATION_SLOTS[i], rotated[i]);
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    public void balanceCrafting() {
        if (player.level().isClientSide) return;
        List<List<ItemStack>> groups = new ArrayList<>();
        for (ItemStack stack : craftSlots.getItems()) {
            if (stack.isEmpty() || stack.getMaxStackSize() <= 1) continue;
            List<ItemStack> group = groups.stream().filter(list -> Util.areStacksEqual(player.level().registryAccess(), list.get(0), stack))
                    .findFirst().orElseGet(() -> {
                        List<ItemStack> created = new ArrayList<>();
                        groups.add(created);
                        return created;
                    });
            group.add(stack);
        }

        for (List<ItemStack> group : groups) {
            int total = group.stream().mapToInt(ItemStack::getCount).sum();
            int each = total / group.size();
            int remainder = total % group.size();
            for (ItemStack stack : group) stack.setCount(each);
            for (int i = 0; i < group.size() && remainder > 0; i++) {
                ItemStack stack = group.get(i);
                if (stack.getCount() < stack.getMaxStackSize()) {
                    stack.grow(1);
                    remainder--;
                }
            }
        }
        slotsChanged(craftSlots);
        craftSlots.setChanged();
    }

    public void spreadCrafting() {
        if (player.level().isClientSide) return;
        while (true) {
            ItemStack biggest = null;
            int size = 1;
            for (ItemStack stack : craftSlots.getItems()) {
                if (!stack.isEmpty() && stack.getCount() > size) {
                    biggest = stack;
                    size = stack.getCount();
                }
            }
            if (biggest == null) break;

            boolean hadEmpty = false;
            for (int i = 0; i < craftSlots.getContainerSize(); i++) {
                if (craftSlots.getItem(i).isEmpty()) {
                    if (biggest.getCount() > 1) craftSlots.setItem(i, biggest.split(1));
                    else hadEmpty = true;
                }
            }
            if (!hadEmpty) break;
        }
        balanceCrafting();
    }

    public void action(PacketArcaneTransmutationTabletSmallButton.Action action) {
        com.wjx.forge.projectexa.net.PacketHandler.sendToServer(new PacketArcaneTransmutationTabletSmallButton(action));
    }

    public Player getPlayer() {
        return player;
    }

    public IKnowledgeProvider getProvider() {
        return provider;
    }

    public List<PXOutputSlot> getOutputSlots() {
        return List.copyOf(outputSlots);
    }

    private static final class PacketHandlerAccess {
        private static void sendOutputUpdate(int index, ItemStack stack) {
            com.wjx.forge.projectexa.net.PacketHandler.sendToServer(
                    new PacketArcaneTransmutationTabletOutputUpdate(index, stack));
        }
    }
}
