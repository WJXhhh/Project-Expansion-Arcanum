package cool.furry.mc.forge.projectexpansion.util;
import cool.furry.mc.forge.projectexpansion.config.Config;
import cool.furry.mc.forge.projectexpansion.registries.SoundEvents;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.emc.nbt.NBTManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
public class AlchemicalCollectionCollector {
    public static HashMap<UUID, Collected> saved = new HashMap<>();
    public record Collected(UUID player, BigInteger emc, List<ItemStack> items, long lastUpdatedAt) {
        public boolean inCooldown() {
            return System.currentTimeMillis() < lastUpdatedAt + 250;
        }
        public boolean process() {
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            @Nullable ServerPlayer serverPlayer = Util.getPlayer(player);
            if(provider == null || serverPlayer == null) return false;
            for (ItemStack item : items) {
                if(provider.addKnowledge(item)) {
                    provider.syncKnowledgeChange(serverPlayer, NBTManager.getPersistentInfo(ItemInfo.fromStack(item)), true);
                }
            }
            provider.setEmc(provider.getEmc().add(emc));
            provider.syncEmc(serverPlayer);
            if (Config.alchemicalCollectionSound.get()) {
                serverPlayer.getLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.ALCHEMICAL_COLLECTION_COLLECT.get(), SoundSource.BLOCKS, 1.0F, 0.85F);
            }
            return true;
        }
    }
    public static void add(UUID player, BigInteger emc, List<ItemStack> items) {
        @Nullable Collected existing = get(player);
        if (existing != null) {
            items.addAll(existing.items);
            saved.put(player, new Collected(player, existing.emc.add(emc), items, System.currentTimeMillis()));
        } else {
            saved.put(player, new Collected(player, emc, items, System.currentTimeMillis()));
        }
    }
    public static @Nullable Collected get(UUID player) {
        return saved.get(player);
    }
    public static void process() {
        List<UUID> toRemove = new ArrayList<>();
        for (UUID uuid : saved.keySet()) {
            Collected collected = saved.get(uuid);
            if (!collected.inCooldown() && collected.process()) {
                toRemove.add(uuid);
            }
        }
        for (UUID uuid : toRemove) {
            saved.remove(uuid);
        }
    }
}