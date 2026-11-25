package com.wjx.neoforge.projectexa.integrations.tacz;

import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@RecipeTypeMapper(priority = 1, requiredMods = "tacz")
public final class TaczGunSmithTableMapper implements IRecipeTypeMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaczGunSmithTableMapper.class);

    @Override public String getTranslationKey() { return "projectexa.integration.tacz.gun_smith_table"; }
    @Override public String getName() { return "TACZGunSmithTable"; }
    @Override public String getDescription() { return "Calculates EMC from TaCZ gun smith table recipes."; }
    @Override public boolean canHandle(RecipeType<?> type) { return type == ModRecipe.GUN_SMITH_TABLE_CRAFTING.get(); }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, RecipeHolder<?> holder,
                                RegistryAccess access, INSSFakeGroupManager groups) {
        if (!(holder.value() instanceof GunSmithTableRecipe recipe)) return false;
        try {
            // TaCZ and ProjectE both handle tag updates. Resolve the lazy result before the EMC pass uses it.
            recipe.init(access);
            ItemStack output = recipe.getOutput();
            if (output.isEmpty() || recipe.getInputs().isEmpty()) return true;

            Object2IntMap<NormalizedSimpleStack> inputs = new Object2IntOpenHashMap<>();
            for (GunSmithTableIngredient input : recipe.getInputs()) {
                if (input.getCount() <= 0) return true;
                Set<NormalizedSimpleStack> alternatives = new HashSet<>();
                for (ItemStack option : input.getIngredient().getItems()) {
                    if (!option.isEmpty()) alternatives.add(TaczEmc.normalized(option));
                }
                if (alternatives.isEmpty()) return true;
                NormalizedSimpleStack material = alternatives.iterator().next();
                if (alternatives.size() > 1) {
                    var group = groups.getOrCreateFakeGroup(alternatives);
                    material = group.dummy();
                    if (group.created()) {
                        for (NormalizedSimpleStack option : alternatives) {
                            Object2IntMap<NormalizedSimpleStack> conversion = new Object2IntOpenHashMap<>();
                            conversion.put(option, 1);
                            mapper.addConversion(1, material, conversion);
                        }
                    }
                }
                inputs.mergeInt(material, input.getCount(), Math::addExact);
            }
            mapper.addConversion(output.getCount(), TaczEmc.normalized(output), inputs);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to map TaCZ gun smith table recipe {}", holder.id(), e);
        }
        return true;
    }
}
