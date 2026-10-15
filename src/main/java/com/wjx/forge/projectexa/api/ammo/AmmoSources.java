package com.wjx.forge.projectexa.api.ammo;

import com.wjx.forge.projectexa.item.ItemPersonalEmcWarehouse;
import com.wjx.forge.projectexa.item.PersonalEmcWarehouseHandler;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.integration.curios.CuriosIntegration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * Transactional ammunition sources for ANY item predicate, not just ArrowItem.
 * Query/simulation never pays. A successful server-thread take returns paid ammunition
 * to the caller, never inserts it into an inventory. No per-weapon Mixin is needed.
 */
public final class AmmoSources {
    // ItemStack uses identity equality. Values must not retain their weak preview keys.
    private static final Map<ItemStack, ProjectilePreview> PREVIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AmmoSources() {
    }

    /** Physical inventory/Curios first, then bound warehouses. One exact batch from one source. */
    public static Optional<Selection> find(Player player, Predicate<ItemStack> accepts, int amount) {
        Objects.requireNonNull(accepts, "accepts");
        if (amount <= 0) return Optional.empty();
        for (IItemHandler inventory : inventories(player)) {
            if (inventory == null) continue;
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && !(stack.getItem() instanceof ItemPersonalEmcWarehouse)
                        && accepts.test(stack)) {
                    Selection selection = new Selection(player, inventory, slot, stack, null, amount);
                    if (!selection.simulate().isEmpty()) return Optional.of(selection);
                }
            }
        }
        return findWarehouse(player, accepts, amount);
    }

    /** Virtual-only lookup, used after native/other-mod projectile lookup returned empty. */
    public static Optional<Selection> findWarehouse(Player player, Predicate<ItemStack> accepts, int amount) {
        Objects.requireNonNull(accepts, "accepts");
        if (!ItemPersonalEmcWarehouse.ENABLED || amount <= 0) return Optional.empty();
        for (IItemHandler inventory : inventories(player)) {
            if (inventory == null) continue;
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack warehouse = inventory.getStackInSlot(i);
                if (!(warehouse.getItem() instanceof ItemPersonalEmcWarehouse)
                        || !ItemPersonalEmcWarehouse.isBound(warehouse)) continue;
                IItemHandler handler = new PersonalEmcWarehouseHandler(warehouse, player);
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.extractItem(slot, amount, true);
                    if (!stack.isEmpty() && stack.getCount() >= amount && accepts.test(stack)) {
                        Selection selection = new Selection(player, handler, slot, stack, warehouse, amount);
                        if (!selection.simulate().isEmpty()) return Optional.of(selection);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Adapter-backed, single-round preview for LivingGetProjectileEvent. Unsupported
     * weapons get EMPTY; opt-in prevents an unknown shrink/split path from creating free ammo.
     */
    public static ItemStack previewProjectile(Player player, ItemStack weapon) {
        ProjectileAmmoAdapter adapter = ProjectileAmmoAdapters.find(weapon).orElse(null);
        if (adapter == null) return ItemStack.EMPTY;
        Selection selection = findWarehouse(player, stack -> adapter.accepts(player, weapon, stack), 1).orElse(null);
        if (selection == null) return ItemStack.EMPTY;
        ItemStack preview = selection.preview();
        PREVIEWS.put(preview, new ProjectilePreview(selection, weapon.getItem(), adapter));
        return preview;
    }

    /**
     * Commit the ORIGINAL event preview after charge/cancellation checks and before
     * firing/loading. Replays fail. Non-warehouse stacks pass through: their ordinary
     * shrink/split remains the weapon's responsibility. Copy only AFTER this call.
     */
    public static boolean consumeProjectile(Player player, ItemStack weapon, ItemStack projectile) {
        ProjectilePreview preview = PREVIEWS.get(projectile);
        if (preview == null) return true;
        Selection selection = preview.selection();
        if (selection.player != player || preview.weapon() != weapon.getItem()
                || !ItemStack.isSameItemSameTags(selection.template, projectile)
                || !preview.adapter().accepts(player, weapon, projectile)) return false;
        if (player.level().isClientSide) return !selection.simulate().isEmpty();
        boolean free = player.getAbilities().instabuild || preview.adapter().isInfinite(player, weapon, projectile);
        return !selection.take(free).isEmpty();
    }

    /** A single-use exact-quantity transaction. Availability, ownership and NBT are rechecked. */
    public static final class Selection {
        private final Player player;
        private final IItemHandler handler;
        private final int slot;
        private final ItemStack template;
        private final @Nullable ItemStack physical;
        private final @Nullable ItemStack warehouse;
        private final @Nullable UUID owner;
        private final int amount;
        private boolean completed;

        private Selection(Player player, IItemHandler handler, int slot, ItemStack stack,
                          @Nullable ItemStack warehouse, int amount) {
            this.player = player;
            this.handler = handler;
            this.slot = slot;
            this.template = stack.copyWithCount(1);
            this.physical = warehouse == null ? stack : null;
            this.warehouse = warehouse;
            this.owner = warehouse == null ? null : ItemPersonalEmcWarehouse.getOwner(warehouse).orElse(null);
            this.amount = amount;
        }

        /** Defensive snapshot for presentation; not a promise that a later take will succeed. */
        public ItemStack preview() {
            return completed ? ItemStack.EMPTY : template.copyWithCount(amount);
        }

        public ItemStack simulate() {
            if (completed || !isSourceValid()) return ItemStack.EMPTY;
            ItemStack available = handler.extractItem(slot, amount, true);
            return available.getCount() >= amount && ItemStack.isSameItemSameTags(available, template)
                    ? template.copyWithCount(amount) : ItemStack.EMPTY;
        }

        public ItemStack take() {
            return take(player.getAbilities().instabuild);
        }

        /** Trusted server-side weapon policy; free=true must match its own infinite-ammo rules. */
        public ItemStack take(boolean free) {
            if (completed || player.level().isClientSide || player.level().getServer() == null
                    || !player.level().getServer().isSameThread()) return ItemStack.EMPTY;
            ItemStack available = simulate();
            completed = true;
            if (available.isEmpty()) return ItemStack.EMPTY;
            if (free) return available;
            return handler.extractItem(slot, amount, false);
        }

        private boolean isSourceValid() {
            if (warehouse == null) return handler.getStackInSlot(slot) == physical;
            if (owner == null || !ItemPersonalEmcWarehouse.getOwner(warehouse).filter(owner::equals).isPresent()) {
                return false;
            }
            for (IItemHandler inventory : inventories(player)) {
                if (inventory == null) continue;
                for (int i = 0; i < inventory.getSlots(); i++) {
                    if (inventory.getStackInSlot(i) == warehouse) return true;
                }
            }
            return false;
        }
    }

    private static IItemHandler[] inventories(Player player) {
        // Never scan the wrapped capability here: its appended virtual slots would recurse.
        return new IItemHandler[]{new InvWrapper(player.getInventory()),
                ModList.get().isLoaded(IntegrationHelper.CURIO_MODID) ? CuriosIntegration.getAll(player) : null};
    }

    private record ProjectilePreview(Selection selection, Item weapon, ProjectileAmmoAdapter adapter) {
    }
}
