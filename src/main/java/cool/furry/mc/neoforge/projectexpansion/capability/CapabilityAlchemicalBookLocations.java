package cool.furry.mc.neoforge.projectexpansion.capability;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cool.furry.mc.neoforge.projectexpansion.item.ItemAlchemicalBook;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_client.PacketSyncAlchemicalBookLocations;
import cool.furry.mc.neoforge.projectexpansion.registries.AttachmentTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.Capabilities;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.TagNames;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class CapabilityAlchemicalBookLocations implements IAlchemicalBookLocationsProvider {
    public static final StreamCodec<RegistryFriendlyByteBuf, CapabilityAlchemicalBookLocations> STREAM_CODEC = StreamCodec.composite(
        ItemAlchemicalBook.Mode.STREAM_CODEC, CapabilityAlchemicalBookLocations::getMode,
        ByteBufCodecs.optional(Util.SERVER_PLAYER_STREAM_CODEC), (cap)-> Optional.ofNullable(cap.getPlayer()),
        ByteBufCodecs.optional(ItemStack.STREAM_CODEC), (cap)-> Optional.ofNullable(cap.getItemStack()),
        (mode, player, itemStack) -> new CapabilityAlchemicalBookLocations(mode, player.orElse(null), itemStack.orElse(null))
    );
    public static final int BASIC_DISTANCE_RATIO = 1000;
    public static final int ADVANCED_DISTANCE_RATIO = 500;
    public static final int MASTER_DISTANCE_RATIO = 100;
    public static final int ARCANE_DISTANCE_RATIO = 0;
    private final ItemAlchemicalBook.Mode mode;
    private final @Nullable ServerPlayer player;
    private final @Nullable ItemStack itemStack;
    public CapabilityAlchemicalBookLocations(ItemAlchemicalBook.Mode mode, @Nullable ServerPlayer player, @Nullable ItemStack itemStack) {
        this.mode = mode;
        this.player = player;
        this.itemStack = itemStack;
    }

    public static IAlchemicalBookLocationsProvider fromPlayer(Player player) {
        IAlchemicalBookLocationsProvider provider = player.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ENTITY);
        if (provider == null) {
            throw new IllegalStateException("Player does not have expected capability");
        }
        return provider;
    }
    public static IAlchemicalBookLocationsProvider fromItemStack(ItemStack stack) {
        IAlchemicalBookLocationsProvider provider = stack.getCapability(Capabilities.ALCHEMICAL_BOOK_LOCATIONS_ITEM);
        if (provider == null) {
            throw new IllegalStateException("ItemStack does not have expected capability");
        }
        return provider;
    }

    public static IAlchemicalBookLocationsProvider from(ItemStack stack) throws BookError.OwnerOfflineError {
        if(!(stack.getItem() instanceof ItemAlchemicalBook book)) {
            throw new IllegalArgumentException("ItemStack is not an alchemical book");
        }

        if(book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            ServerPlayer owner = book.getPlayer(stack);
            if(owner == null) {
                DataComponentTypes.OwnerData ownerData = stack.get(DataComponentTypes.OWNER);
                throw new BookError.OwnerOfflineError(ownerData == null ? "None" : ownerData.name());
            }
            return fromPlayer(owner);
        } else {
            return fromItemStack(stack);
        }
    }

    public static boolean isForbiddenName(String name) {
        return name.equalsIgnoreCase(BACK_KEY);
    }

    public record TeleportLocation(String name, int x, int y, int z, ResourceKey<Level> dimension, int index) {
        public static final Codec<TeleportLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(TeleportLocation::name),
                Codec.INT.fieldOf("x").forGetter(TeleportLocation::x),
                Codec.INT.fieldOf("y").forGetter(TeleportLocation::y),
                Codec.INT.fieldOf("z").forGetter(TeleportLocation::z),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(TeleportLocation::dimension),
                Codec.INT.fieldOf("index").forGetter(TeleportLocation::index)
        ).apply(instance, TeleportLocation::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TeleportLocation> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TeleportLocation::name,
                ByteBufCodecs.INT, TeleportLocation::x,
                ByteBufCodecs.INT, TeleportLocation::y,
                ByteBufCodecs.INT, TeleportLocation::z,
                ResourceKey.streamCodec(Registries.DIMENSION), TeleportLocation::dimension,
                ByteBufCodecs.INT, TeleportLocation::index,
                TeleportLocation::new
        );
        public void teleportTo(ServerPlayer player, boolean acrossDimensions) throws BookError.DimensionNotFoundError, BookError.WrongDimensionError {
            ResourceKey<Level> dim = player.level().dimension();

            ServerLevel level = Util.getDimension(dimension);
            if(level == null) {
                throw new BookError.DimensionNotFoundError(dimension);
            }

            if(!dim.equals(dimension) && !acrossDimensions) {
                throw new BookError.WrongDimensionError();
            }

            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        }

        public double distanceFrom(BlockPos pos) {
            return Math.sqrt(Math.pow(pos.getX() - x, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() - z, 2));
        }

        public int getCost(ItemStack stack, Player player) {
            if (player.isCreative()) {
                return 0;
            }

            return getCost(stack, player.getOnPos());
        }

        public int getCost(ItemStack stack, BlockPos pos) {
            if(!(stack.getItem() instanceof ItemAlchemicalBook book)) {
                return (int) Math.ceil(distanceFrom(pos) * BASIC_DISTANCE_RATIO);
            }
            return (int) Math.ceil(distanceFrom(pos) * book.getTier().distanceRatio());
        }

        // records are immutable
        public TeleportLocation withIndex(int index) {
            return from(name, new BlockPos(x, y, z), dimension, index);
        }

        public boolean isBack() {
            return name.equals(BACK_KEY);
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putString(TagNames.NAME, name);
            tag.putInt(TagNames.X, x);
            tag.putInt(TagNames.Y, y);
            tag.putInt(TagNames.Z, z);
            tag.putInt(TagNames.INDEX, index);
            tag.putString(TagNames.DIMENSION, dimension.location().toString());
            return tag;
        }

        public static TeleportLocation deserialize(CompoundTag tag) {
            String name = tag.getString(TagNames.NAME);
            int x = tag.getInt(TagNames.X);
            int y = tag.getInt(TagNames.Y);
            int z = tag.getInt(TagNames.Z);
            int index = tag.getInt(TagNames.INDEX);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString(TagNames.DIMENSION)));
            return new TeleportLocation(name, x, y, z, dimension, index);
        }

        public TeleportLocation copy() {
            return new TeleportLocation(name, x, y, z, dimension, index);
        }

        public static TeleportLocation from(String name, GlobalPos pos, int index) {
            return from(name, pos.pos(), pos.dimension(), index);
        }

        public static TeleportLocation from(String name, BlockPos pos, ResourceKey<Level> dimension, int index) {
            return new TeleportLocation(name, pos.getX(), pos.getY(), pos.getZ(), dimension, index);
        }
    }

    public static class BookError extends Exception {
        public static class WrongDimensionError extends BookError {
            public WrongDimensionError() {
                super(Type.WRONG_DIMENSION);
            }
        }
        public static class DimensionNotFoundError extends BookError {
            private final Component name;
            public DimensionNotFoundError(Component name) {
                super(Type.DIMENSION_NOT_FOUND);
                this.name = name;
            }
            public DimensionNotFoundError(ResourceKey<Level> level) {
                this(Component.translatable(level.location().toLanguageKey()));
            }
            public DimensionNotFoundError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }
        public static class NoBackLocationError extends BookError {
            public NoBackLocationError() {
                super(Type.NO_BACK_LOCATION);
            }
        }
        public static class DuplicateNameError extends BookError {
            private final Component name;
            public DuplicateNameError(Component name) {
                super(Type.DUPLICATE_NAME);
                this.name = name;
            }
            public DuplicateNameError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }
        public static class NameNotFoundError extends BookError {
            private final Component name;
            public NameNotFoundError(Component name) {
                super(Type.NAME_NOT_FOUND);
                this.name = name;
            }
            public NameNotFoundError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, name);
            }
        }
        public static class OwnerOfflineError extends BookError {
            private final Component player;
            public OwnerOfflineError(Component name) {
                super(Type.OWNER_OFFLINE);
                this.player = name;
            }
            public OwnerOfflineError(String name) {
                this(Component.literal(name));
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, player);
            }
        }
        public static class NotEnoughEMCError extends BookError {
            private final String emc;
            public NotEnoughEMCError(String emc) {
                super(Type.NOT_ENOUGH_EMC);
                this.emc = emc;
            }

            @Override
            public MutableComponent getComponent() {
                return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED, emc);
            }
        }
        public static class EditNotAllowedError extends BookError {
            public EditNotAllowedError() {
                super(Type.EDIT_NOT_ALLOWED);
            }
        }
        public enum Type {
            // Teleportation
            WRONG_DIMENSION,
            DIMENSION_NOT_FOUND,
            NO_BACK_LOCATION,
            NOT_ENOUGH_EMC,
            // Destinations
            DUPLICATE_NAME,
            NAME_NOT_FOUND,
            // Book Interaction
            OWNER_OFFLINE,
            EDIT_NOT_ALLOWED,
        }
        private final Type type;
        public BookError(Type type) {
            super("Book error: " + type.name());
            this.type = type;
        }

        protected String getKey() {
            return type.name().toLowerCase();
        }
        public Type getType() {
            return type;
        }

        public MutableComponent getComponent() {
            return Lang.Items.ALCHEMICAL_BOOK_ERROR.extendColored(getKey(), ChatFormatting.RED);
        }
    }

    protected @Nullable AttachmentAlchemicalBookLocations getAttachment() {
        if (player != null) {
            return player.getData(AttachmentTypes.ALCHEMICAL_BOOK_LOCATIONS);
        } else if (itemStack != null) {
            return null;
        } else {
            throw new NullPointerException("Both Player and ItemStack are null");
        }
    }

    protected @Nullable ItemStackDataProvider getItemStackDataProvider() {
        if (itemStack != null) {
            return itemStack.getOrDefault(DataComponentTypes.ALCHEMICAL_BOOK_LOCATIONS, new ItemStackDataProvider(new HashMap<>()));
        } else if (player != null) {
            return null;
        } else {
            throw new NullPointerException("Both Player and ItemStack are null");
        }
    }

    protected ILocationsProvider getLocationsProvider() {
        @Nullable ILocationsProvider provider = null;
        if (itemStack != null) {
            provider = getItemStackDataProvider();
        } else if (player != null) {
            provider = getAttachment();
        } else {
            throw new NullPointerException("Both Player and ItemStack are null");
        }

        if (provider == null) {
            throw new NullPointerException("Failed to get locations provider");
        }

        return provider;
    }

    @Override
    public ImmutableList<TeleportLocation> getLocations() {
        return getLocationsProvider().getLocations().values().stream().sorted(Comparator.comparingInt(TeleportLocation::index)).collect(ImmutableList.toImmutableList());
    }

    @Override
    public void addLocation(String name, GlobalPos pos) throws BookError.DuplicateNameError {
        ILocationsProvider provider = getLocationsProvider();
        if(provider.getLocations().containsKey(name) && !name.equals(BACK_KEY)) {
            throw new BookError.DuplicateNameError(name);
        }
        int index = provider.getLocations().size();
        if(name.equals(BACK_KEY)) {
            index = -1;
        } else if(provider.getLocations().containsKey(BACK_KEY)) {
            index -= 1;
        }
        TeleportLocation location = TeleportLocation.from(name, pos, index);
        provider = provider.addLocation(name, location);
        markDirty(provider);
    }

    @Override
    public void addLocation(Player player, String name) throws BookError.DuplicateNameError {
        addLocation(name, GlobalPos.of(player.level().dimension(), player.blockPosition()));
    }

    @Override
    public void removeLocation(String name) throws BookError.NameNotFoundError {
        ILocationsProvider provider = getLocationsProvider();
        if(!provider.getLocations().containsKey(name)) {
            throw new BookError.NameNotFoundError(name);
        }

        int index = provider.getLocations().get(name).index();
        provider = provider.removeLocation(name);
        markDirty(provider);
    }

    @Override
    public void saveBackLocation(Player player, GlobalPos pos) {
        try {
            if(getLocationsProvider().getLocations().containsKey(BACK_KEY)) {
                removeLocation(BACK_KEY);
            }

            addLocation(BACK_KEY, pos);
            sync((ServerPlayer) player);
        } catch (BookError.NameNotFoundError | BookError.DuplicateNameError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveBackLocation(Player player) {
        saveBackLocation(player, GlobalPos.of(player.level().dimension(), player.blockPosition()));
    }

    private static final String BACK_KEY = "@back";
    @Override
    public @Nullable TeleportLocation getBackLocation() {
        ILocationsProvider provider = getLocationsProvider();
        if(!provider.getLocations().containsKey(BACK_KEY)) {
            return null;
        }

        return provider.getLocations().get(BACK_KEY);
    }

    @Override
    public TeleportLocation getBackLocationOrThrow() throws BookError.NoBackLocationError {
        TeleportLocation location = getBackLocation();
        if(location == null) {
            throw new BookError.NoBackLocationError();
        }

        return location;
    }

    @Override
    public void teleportBack(ServerPlayer player, boolean acrossDimensions) throws BookError.NoBackLocationError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        @Nullable TeleportLocation backLocation = getBackLocation();
        if(backLocation == null) {
            throw new BookError.NoBackLocationError();
        }

        ResourceKey<Level> dim = player.level().dimension();

        ServerLevel level = Util.getDimension(backLocation.dimension());
        if(level == null) {
            throw new BookError.DimensionNotFoundError(backLocation.dimension());
        }

        if(!dim.equals(backLocation.dimension()) && !acrossDimensions) {
            throw new BookError.WrongDimensionError();
        }

        try {
            removeLocation(BACK_KEY);
        } catch(BookError.NameNotFoundError e) {
            throw new RuntimeException(e);
        }
        sync(player);
        backLocation.teleportTo(player, acrossDimensions);
    }

    @Override
    public void teleportTo(String name, ServerPlayer player, boolean acrossDimensions) throws BookError.NameNotFoundError, BookError.WrongDimensionError, BookError.DimensionNotFoundError {
        GlobalPos pos = GlobalPos.of(player.level().dimension(), player.blockPosition());
        getLocationOrThrow(name).teleportTo(player, acrossDimensions);
        saveBackLocation(player, pos);
    }

    @Override
    public @Nullable TeleportLocation getLocation(String name) {
        return getLocationsProvider().getLocations().get(name);
    }

    @Override
    public TeleportLocation getLocationOrThrow(String name) throws BookError.NameNotFoundError {
        @Nullable TeleportLocation location = getLocation(name);
        if(location == null) {
            throw new BookError.NameNotFoundError(name);
        }

        return location;
    }

    @Override
    public void resetLocations() {
        ILocationsProvider provider = getLocationsProvider();
        provider = provider.clearLocations();
        markDirty(provider);
    }

    @Override
    public void sync(ServerPlayer player) {
        boolean canEdit = false;
        if(this.player != null) {
            canEdit = ItemAlchemicalBook.canEdit(player, this.player);
        } else if(itemStack != null) {
            canEdit = ItemAlchemicalBook.canEdit(itemStack, player);
        }
        PacketDistributor.sendToPlayer(player, new PacketSyncAlchemicalBookLocations(getLocations(), canEdit));
    }

    @Override
    public ItemAlchemicalBook.Mode getMode() {
        return mode;
    }

    @Override
    public @Nullable ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public ServerPlayer getPlayerOrException() {
        if (player == null) {
            throw new NullPointerException("Player is null");
        }
        return player;
    }

    @Override
    public @Nullable ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public ItemStack getItemStackOrException() {
        if (itemStack == null) {
            throw new NullPointerException("ItemStack is null");
        }
        return itemStack;
    }

    @Override
    public void syncToOtherPlayers() {
        if (player == null) return;
        for (String playerName : ServerLifecycleHooks.getCurrentServer().getPlayerNames()) {
            ServerPlayer target = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(playerName);
            if(target == null) continue;
            syncToPlayer(target);
        }
    }

    @Override
    public void syncToPlayer(ServerPlayer target) {
        if (player == null) return;
        ItemStack stack = target.getMainHandItem();
        if (stack.getItem() instanceof ItemAlchemicalBook book && book.getMode(stack) == ItemAlchemicalBook.Mode.PLAYER) {
            @Nullable ServerPlayer stackOwner = book.getPlayer(stack);
            if(player.equals(stackOwner)) {
                fromPlayer(player).sync(target);
            }
        }
    }

    @Override
    public void ensureEditable(ServerPlayer editor) throws BookError.EditNotAllowedError {
        if(player != null && !ItemAlchemicalBook.canEdit(editor, player)) {
            throw new BookError.EditNotAllowedError();
        }
    }

    protected void markDirty(ILocationsProvider provider) {
        if (itemStack != null) {
            itemStack.set(DataComponentTypes.ALCHEMICAL_BOOK_LOCATIONS, (ItemStackDataProvider) provider);
        } else if (player != null) {
            player.setData(AttachmentTypes.ALCHEMICAL_BOOK_LOCATIONS, (AttachmentAlchemicalBookLocations) provider);
        } else {
            throw new NullPointerException("Both Player and ItemStack are null");
        }
    }

    public static class AttachmentAlchemicalBookLocations implements ILocationsProvider {
        public static final Codec<AttachmentAlchemicalBookLocations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
             Codec.unboundedMap(Codec.STRING, TeleportLocation.CODEC).fieldOf("locations").forGetter(AttachmentAlchemicalBookLocations::getLocations)
        ).apply(instance, AttachmentAlchemicalBookLocations::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AttachmentAlchemicalBookLocations> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, TeleportLocation.STREAM_CODEC), AttachmentAlchemicalBookLocations::getLocations,
                AttachmentAlchemicalBookLocations::new
        );

        private final Map<String, TeleportLocation> locations;

        public AttachmentAlchemicalBookLocations() {
            this(new HashMap<>());
        }

        private AttachmentAlchemicalBookLocations(Map<String, TeleportLocation> locations) {
            this.locations = locations;
        }

        @Nullable
        public AttachmentAlchemicalBookLocations copy(IAttachmentHolder holder, HolderLookup.Provider registries) {
            HashMap<String, TeleportLocation> locationsCopy = new HashMap<>();
            for (Map.Entry<String, TeleportLocation> entry : locations.entrySet()) {
                locationsCopy.put(entry.getKey(), entry.getValue().copy());
            }
            return new AttachmentAlchemicalBookLocations(locationsCopy);
        }

        public Map<String, TeleportLocation> getLocations() {
            return locations;
        }

        @Override
        public AttachmentAlchemicalBookLocations removeLocation(String name) {
            HashMap<String, TeleportLocation> newLocations = new HashMap<>();
            int index = locations.get(name).index();
            locations.forEach((locationName, location) -> {
                int locationIndex = location.index();
                if(locationIndex < index) {
                    newLocations.put(locationName, location.copy());
                } else if (locationIndex > index) {
                    newLocations.put(locationName, location.withIndex(location.index() + 1));
                }
                // ignore if locationIndex == index
            });

            return new AttachmentAlchemicalBookLocations(newLocations);
        }

        @Override
        public AttachmentAlchemicalBookLocations addLocation(String name, TeleportLocation location) {
            HashMap<String, TeleportLocation> newLocations = new HashMap<>();
            for (Map.Entry<String, TeleportLocation> entry : locations.entrySet()) {
                newLocations.put(entry.getKey(), entry.getValue().copy());
            }
            newLocations.put(name, location);
            return new AttachmentAlchemicalBookLocations(newLocations);
        }

        @Override
        public AttachmentAlchemicalBookLocations clearLocations() {
            return new AttachmentAlchemicalBookLocations(new HashMap<>());
        }
    }

    public record ItemStackDataProvider(Map<String, TeleportLocation> locations) implements ILocationsProvider {
        public static final Codec<ItemStackDataProvider> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, TeleportLocation.CODEC).fieldOf("locations").forGetter(ItemStackDataProvider::getLocations)
        ).apply(instance, ItemStackDataProvider::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackDataProvider> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, TeleportLocation.STREAM_CODEC), ItemStackDataProvider::getLocations,
                ItemStackDataProvider::new
        );

        public Map<String, TeleportLocation> getLocations() {
            return locations;
        }

        @Override
        public ItemStackDataProvider removeLocation(String name) {
            HashMap<String, TeleportLocation> newLocations = new HashMap<>();
            int index = locations.get(name).index();

            locations.forEach((locationName, location) -> {
                int locationIndex = location.index();
                if (locationIndex < index) {
                    newLocations.put(locationName, location.copy());
                } else if (locationIndex > index) {
                    newLocations.put(locationName, location.withIndex(location.index() + 1));
                }
                // ignore if locationIndex == index
            });

            return new ItemStackDataProvider(newLocations);
        }

        @Override
        public ItemStackDataProvider addLocation(String name, TeleportLocation location) {
            HashMap<String, TeleportLocation> newLocations = new HashMap<>();
            for (Map.Entry<String, TeleportLocation> entry : locations.entrySet()) {
                newLocations.put(entry.getKey(), entry.getValue().copy());
            }
            newLocations.put(name, location);
            return new ItemStackDataProvider(newLocations);
        }

        @Override
        public ItemStackDataProvider clearLocations() {
            return new ItemStackDataProvider(new HashMap<>());
        }
    }

    public interface ILocationsProvider {
        Map<String, TeleportLocation> getLocations();
        ILocationsProvider removeLocation(String name);
        ILocationsProvider addLocation(String name, TeleportLocation location);
        ILocationsProvider clearLocations();
    }
}
