package com.wjx.forge.projectexa.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.wjx.forge.projectexa.Main;
import com.wjx.forge.projectexa.util.Lang;
import moze_intel.projecte.api.nss.AbstractNSSTag;
import moze_intel.projecte.config.CustomEMCParser;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.network.PacketHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

public final class CommandReloadEMC {

    private CommandReloadEMC() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("reloadEMC")
                .requires(Permissions.RELOAD_EMC)
                .executes(ctx -> reload(ctx.getSource()));
    }

    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        long startTime = System.currentTimeMillis();
        try {
            AbstractNSSTag.clearCreatedTags();
            CustomEMCParser.init();
            EMCMappingHandler.map(
                    server.getServerResources().managers(),
                    server.registryAccess(),
                    server.getResourceManager()
            );
            PacketHandler.sendFragmentedEmcPacketToAll();

            int mappedValues = EMCMappingHandler.getEmcMapSize();
            long elapsedTime = System.currentTimeMillis() - startTime;
            source.sendSuccess(
                    () -> Lang.Commands.RELOAD_EMC_SUCCESS.translateColored(ChatFormatting.GREEN, mappedValues, elapsedTime),
                    true
            );
            return 1;
        } catch (Throwable error) {
            Main.Logger.error("Error calculating EMC values during fast EMC reload", error);
            source.sendFailure(Lang.Commands.RELOAD_EMC_FAIL.translateColored(ChatFormatting.RED));
            return 0;
        }
    }
}
