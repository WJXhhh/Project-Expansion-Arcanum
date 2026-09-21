package com.wjx.forge.projectexa.net.packets.to_server;

import com.wjx.forge.projectexa.integrations.create.CreateRecipeIntegration;
import com.wjx.forge.projectexa.integrations.jei.CreateJeiRecipeTypes;
import com.wjx.forge.projectexa.net.packets.IPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Requests conversion of missing materials for one Create JEI recipe. */
public record PacketCreateRecipeTransmutation(ResourceLocation recipeTypeId,
                                              @Nullable ResourceLocation recipeId,
                                              List<List<ItemStack>> fallbackIngredients) implements IPacket {
    private static final int MAX_FALLBACK_GROUPS = 64;
    private static final int MAX_FALLBACK_OPTIONS = 32;

    public PacketCreateRecipeTransmutation {
        if (recipeTypeId == null || !CreateJeiRecipeTypes.isSupportedRecipeType(recipeTypeId)) {
            throw new IllegalArgumentException("recipe type must be a supported Create recipe type");
        }

        List<List<ItemStack>> sanitized = new ArrayList<>();
        if (fallbackIngredients != null) {
            for (List<ItemStack> options : fallbackIngredients) {
                if (sanitized.size() >= MAX_FALLBACK_GROUPS || options == null) {
                    break;
                }

                List<ItemStack> sanitizedOptions = new ArrayList<>();
                for (ItemStack stack : options) {
                    if (sanitizedOptions.size() >= MAX_FALLBACK_OPTIONS) {
                        break;
                    }
                    if (stack != null && !stack.isEmpty()) {
                        sanitizedOptions.add(stack.copyWithCount(1));
                    }
                }
                if (!sanitizedOptions.isEmpty()) {
                    sanitized.add(List.copyOf(sanitizedOptions));
                }
            }
        }
        fallbackIngredients = List.copyOf(sanitized);
    }

    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !ModList.get().isLoaded("create")
                || !PacketOpenArcaneTransmutationTablet.hasTablet(player)) {
            return;
        }

        CreateRecipeIntegration.transmute(player, recipeTypeId, recipeId, fallbackIngredients);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeTypeId);
        buffer.writeBoolean(recipeId != null);
        if (recipeId != null) {
            buffer.writeResourceLocation(recipeId);
        }

        buffer.writeVarInt(fallbackIngredients.size());
        for (List<ItemStack> options : fallbackIngredients) {
            buffer.writeVarInt(options.size());
            for (ItemStack stack : options) {
                buffer.writeItem(stack);
            }
        }
    }

    public static PacketCreateRecipeTransmutation decode(FriendlyByteBuf buffer) {
        ResourceLocation recipeTypeId = buffer.readResourceLocation();
        ResourceLocation recipeId = buffer.readBoolean() ? buffer.readResourceLocation() : null;
        int groupCount = buffer.readVarInt();
        if (groupCount < 0 || groupCount > MAX_FALLBACK_GROUPS) {
            throw new IllegalArgumentException("too many Create recipe fallback groups");
        }

        List<List<ItemStack>> fallbackIngredients = new ArrayList<>(groupCount);
        for (int group = 0; group < groupCount; group++) {
            int optionCount = buffer.readVarInt();
            if (optionCount < 0 || optionCount > MAX_FALLBACK_OPTIONS) {
                throw new IllegalArgumentException("too many Create recipe fallback options");
            }

            List<ItemStack> options = new ArrayList<>(optionCount);
            for (int option = 0; option < optionCount; option++) {
                options.add(buffer.readItem());
            }
            fallbackIngredients.add(options);
        }
        return new PacketCreateRecipeTransmutation(recipeTypeId, recipeId, fallbackIngredients);
    }
}
