package com.wjx.forge.projectexa.util;

import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.config.Config;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StoneTableHelper {
    public static final TagKey<Item> WHITELIST = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            Main.rl("stone_table_whitelist"));

    private StoneTableHelper() {
    }

    public static boolean isWhitelisted(ItemStack stack) {
        return !Config.enableStoneTableWhitelist.get() || stack.is(WHITELIST);
    }

    public static boolean isItemValid(ItemStack stack) {
        return !stack.isEmpty() && isWhitelisted(stack);
    }

    public static List<ItemStack> getKnownItems(IKnowledgeProvider provider) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemInfo info : provider.getKnowledge()) {
            ItemStack stack = info.createStack();
            if (stack.isEmpty() || !isItemValid(stack) || IEMCProxy.INSTANCE.getValue(stack) <= 0) {
                continue;
            }
            items.add(stack.copyWithCount(1));
        }
        items.sort(Comparator
                .comparingLong((ItemStack stack) -> IEMCProxy.INSTANCE.getValue(stack))
                .reversed()
                .thenComparing(StoneTableHelper::registryName));
        return items;
    }

    public static List<ItemStack> filter(List<ItemStack> knownItems, String rawFilter) {
        String filter = rawFilter == null ? "" : rawFilter.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            return knownItems;
        }

        boolean modFilter = filter.charAt(0) == '@';
        String query = modFilter ? filter.substring(1) : filter;
        return knownItems.stream().filter(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (modFilter) {
                return id.getNamespace().contains(query);
            }
            return id.toString().contains(query) || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
        }).toList();
    }

    public static String registryName(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
