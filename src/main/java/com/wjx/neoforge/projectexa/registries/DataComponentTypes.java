package com.wjx.neoforge.projectexa.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wjx.neoforge.projectexa.Main;
import com.wjx.neoforge.projectexa.capability.CapabilityAlchemicalBookLocations;
import com.wjx.neoforge.projectexa.util.BasicDataComponentTypes;
import com.wjx.neoforge.projectexa.util.TagNames;
import com.wjx.neoforge.projectexa.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;

public class DataComponentTypes {
    public static final DeferredRegister.DataComponents Registry = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Main.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> WAREHOUSE_ID = Registry.registerComponentType("warehouse_id",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> WAREHOUSE_FILTERS = Registry.registerComponentType("warehouse_filters",
            builder -> builder.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC).networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.List<Integer>>> WAREHOUSE_AVAILABILITY = Registry.registerComponentType("warehouse_availability",
            builder -> builder.persistent(Codec.INT.listOf(27, 27)).networkSynchronized(ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(27))));

    public record OwnerData(UUID uuid, String name) {
        public static final Codec<OwnerData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(UUIDUtil.CODEC.fieldOf(TagNames.OWNER).forGetter(OwnerData::uuid), Codec.STRING.fieldOf(TagNames.OWNER_NAME).forGetter(OwnerData::name)).apply(instance, OwnerData::new)
        );
        public static final StreamCodec<ByteBuf, OwnerData> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, OwnerData::uuid, ByteBufCodecs.STRING_UTF8, OwnerData::name, OwnerData::new);

        public boolean isNone() {
            return uuid == Util.DUMMY_UUID;
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OwnerData>> OWNER = Registry.registerComponentType("uuid", (builder) -> builder.persistent(OwnerData.CODEC).networkSynchronized(OwnerData.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapabilityAlchemicalBookLocations.AlchemicalBookLocationData>> ALCHEMICAL_BOOK_LOCATIONS = Registry.registerComponentType("alchemical_book_locations", (builder) -> builder.persistent(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.CODEC).networkSynchronized(CapabilityAlchemicalBookLocations.AlchemicalBookLocationData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> LAST_USED = Registry.registerComponentType("last_used", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BasicDataComponentTypes.LongValue>> KNOWLEDGE_GAINED = Registry.registerComponentType("knowledge_gained", (builder) -> builder.persistent(BasicDataComponentTypes.LongValue.CODEC).networkSynchronized(BasicDataComponentTypes.LongValue.STREAM_CODEC));



}
