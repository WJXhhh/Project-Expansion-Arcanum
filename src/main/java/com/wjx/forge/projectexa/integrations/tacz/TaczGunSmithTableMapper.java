package com.wjx.forge.projectexa.integrations.tacz;

import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import moze_intel.projecte.api.mapper.collector.IMappingCollector;
import moze_intel.projecte.api.mapper.recipe.INSSFakeGroupManager;
import moze_intel.projecte.api.mapper.recipe.IRecipeTypeMapper;
import moze_intel.projecte.api.mapper.recipe.RecipeTypeMapper;
import moze_intel.projecte.api.nss.NormalizedSimpleStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feeds TACZ gun smith table recipes into ProjectE's normal EMC calculation.
 *
 * <p>This is a plain {@link IRecipeTypeMapper}, so it runs inside ProjectE's own {@code CraftingMapper}
 * during the regular EMC mapping pass (datapack load / {@code /reload}) and the values are produced by
 * ProjectE's graph solver from the recipe materials, exactly like a vanilla crafting recipe. It can be
 * switched off through ProjectE's own {@code config/ProjectE/mapping.toml}
 * ({@code CraftingMapper.TACZGunSmithTable.enabled}).</p>
 *
 * <p>Guns, ammo and attachments are registered under their canonical NBT (see {@link TaczEmcHelper}), so
 * one value is calculated per gun/cartridge/attachment id instead of one value for the shared item.</p>
 */
@RecipeTypeMapper(priority = 1, requiredMods = "tacz")
public class TaczGunSmithTableMapper implements IRecipeTypeMapper {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getName() {
        return "TACZGunSmithTable";
    }

    @Override
    public String getDescription() {
        return "Calculates EMC for TACZ guns, ammunition and attachments from their gun smith table recipes.";
    }

    @Override
    public boolean canHandle(RecipeType<?> recipeType) {
        RegistryObject<RecipeType<GunSmithTableRecipe>> type = ModRecipe.GUN_SMITH_TABLE_CRAFTING;
        return type != null && type.isPresent() && recipeType == type.get();
    }

    @Override
    public boolean handleRecipe(IMappingCollector<NormalizedSimpleStack, Long> mapper, Recipe<?> recipe,
                                RegistryAccess registryAccess, INSSFakeGroupManager fakeGroupManager) {
        if (!(recipe instanceof GunSmithTableRecipe tableRecipe)) {
            return false;
        }

        ResourceLocation recipeId = tableRecipe.getId();
        ItemStack output;
        List<GunSmithTableIngredient> inputs;
        try {
            // TACZ builds the result lazily from its gun pack index on TagsUpdatedEvent, and ProjectE maps on
            // that very same event. Doing it here keeps the mapping correct whichever listener runs first;
            // the call is idempotent.
            tableRecipe.init();
            output = tableRecipe.getOutput().copy();
            inputs = tableRecipe.getInputs();
        } catch (Exception e) {
            LOGGER.error("Failed to read TACZ gun smith table recipe {}", recipeId, e);
            return true;
        }

        if (output.isEmpty() || inputs == null || inputs.isEmpty()) {
            // Nothing to work with. Claim the recipe anyway so it is not valued as a free conversion.
            return true;
        }

        Map<NormalizedSimpleStack, Integer> ingredients = new HashMap<>();
        List<Tuple<NormalizedSimpleStack, List<Map<NormalizedSimpleStack, Integer>>>> groups = new ArrayList<>();

        for (GunSmithTableIngredient input : inputs) {
            if (input == null || input.getIngredient() == null) {
                continue;
            }

            int count = input.getCount();
            if (count <= 0) {
                continue;
            }

            ItemStack[] matches;
            try {
                matches = input.getIngredient().getItems();
            } catch (Exception e) {
                LOGGER.error("Failed to resolve the materials of TACZ gun smith table recipe {}", recipeId, e);
                return addGroupConversions(mapper, groups);
            }

            List<ItemStack> options = new ArrayList<>();
            for (ItemStack match : matches) {
                if (match != null && !match.isEmpty()) {
                    options.add(match);
                }
            }

            if (options.isEmpty()) {
                // An unresolvable material: the recipe cannot be priced without inventing free EMC.
                return addGroupConversions(mapper, groups);
            }

            if (options.size() == 1) {
                addIngredient(ingredients, options.get(0), count);
                continue;
            }

            Set<NormalizedSimpleStack> alternatives = new HashSet<>();
            for (ItemStack option : options) {
                alternatives.add(TaczEmcHelper.toNormalizedStack(option));
            }

            Tuple<NormalizedSimpleStack, Boolean> group = fakeGroupManager.getOrCreateFakeGroup(alternatives);
            addIngredient(ingredients, group.getA(), count);
            if (group.getB()) {
                List<Map<NormalizedSimpleStack, Integer>> members = new ArrayList<>();
                for (ItemStack option : options) {
                    Map<NormalizedSimpleStack, Integer> member = new HashMap<>();
                    addIngredient(member, option, 1);
                    members.add(member);
                }
                groups.add(new Tuple<>(group.getA(), members));
            }
        }

        if (!ingredients.isEmpty()) {
            mapper.addConversion(Math.max(1, output.getCount()), TaczEmcHelper.toNormalizedStack(output), ingredients);
        }
        return addGroupConversions(mapper, groups);
    }

    private void addIngredient(Map<NormalizedSimpleStack, Integer> ingredients, ItemStack stack, int count) {
        addIngredient(ingredients, TaczEmcHelper.toNormalizedStack(stack), count);
    }

    private void addIngredient(Map<NormalizedSimpleStack, Integer> ingredients, NormalizedSimpleStack stack, int count) {
        ingredients.merge(stack, count, Integer::sum);
    }

    private boolean addGroupConversions(IMappingCollector<NormalizedSimpleStack, Long> mapper,
                                        List<Tuple<NormalizedSimpleStack, List<Map<NormalizedSimpleStack, Integer>>>> groups) {
        for (Tuple<NormalizedSimpleStack, List<Map<NormalizedSimpleStack, Integer>>> group : groups) {
            for (Map<NormalizedSimpleStack, Integer> member : group.getB()) {
                mapper.addConversion(1, group.getA(), member);
            }
        }
        return true;
    }
}
