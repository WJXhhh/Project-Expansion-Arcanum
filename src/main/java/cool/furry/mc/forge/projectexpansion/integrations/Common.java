package cool.furry.mc.forge.projectexpansion.integrations;


import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityNBTFilterable;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityOwnable;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityRelay;
import cool.furry.mc.forge.projectexpansion.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.math.BigInteger;
import java.util.Objects;
import java.util.function.Consumer;

public class Common {
    public static void registerCommonTooltips(Consumer<Component> addTooltip, Block block, BlockEntity blockEntity) {
        if(blockEntity instanceof IGeneratesEMC gen) {
            BigInteger total = gen.getStoredEmcBigInteger();
            BigInteger maximum = gen.getMaximumEmcBigInteger();
            addTooltip.accept(Lang.EMC_STORAGE.translateColored(ChatFormatting.GRAY, formatEMC(total), formatEMC(maximum)));

            BigInteger generated = gen.getGeneratedEMC();
            addTooltip.accept(Lang.EMC_PER_SECOND.translateColored(ChatFormatting.GRAY, formatEMC(generated)));
        }

        if(blockEntity instanceof IHasSunBonus gen && gen.hasSunBonus()) {
            int bonus = Objects.requireNonNull(gen.getSunBonus());
            addTooltip.accept(Lang.SUN_BONUS.translateColored(ChatFormatting.GRAY, Component.literal(String.valueOf(bonus)).withStyle(Style.EMPTY.withBold(true).withColor(ChatFormatting.YELLOW))));
        }

        if (blockEntity instanceof BlockEntityEMCLink link) {
            Matter matter = link.getMatter();
            BigInteger emcLimit = matter.getEMCLinkEMCLimit();
            int importExportLimit = matter.getEMCLinkItemLimit();
            int fluidLimit = matter.getEMCLinkFluidLimit();

            addTooltip.accept(Lang.EMC_IMPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(link.remainingEMC), formatEMC(emcLimit)));
            addTooltip.accept(Lang.ITEM_EXPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingExport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.ITEM_IMPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingImport)), formatEMC(BigInteger.valueOf(importExportLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_LIMIT.translateColored(ChatFormatting.GRAY, formatEMC(BigInteger.valueOf(link.remainingFluid)), formatEMC(BigInteger.valueOf(fluidLimit))));
            addTooltip.accept(Lang.FLUID_EXPORT_EFFICIENCY.translateColored(ChatFormatting.GRAY, Component.literal(String.valueOf(matter.getFluidEfficiencyPercentage())).withStyle(ChatFormatting.GREEN)));
        }

        if (blockEntity instanceof BlockEntityNBTFilterable filter) {
            addTooltip.accept(Lang.FILTER_STATUS.translateColored(ChatFormatting.GRAY, filter.getFilterStatus() ? Lang.ENABLED.translateColored(ChatFormatting.GREEN) : Lang.DISABLED.translateColored(ChatFormatting.RED)));
        }

        if (blockEntity instanceof IHasColor color) {
            addTooltip.accept(Lang.COLOR.translateColored(ChatFormatting.GRAY, Component.literal(Util.ucwords(color.getColor().toString())).withStyle(Style.EMPTY.withColor(color.getColor().getTextColor()))));
        }

        if (blockEntity instanceof BlockEntityRelay relay) {
            BigInteger bonus = relay.getMatter().getRelayBonus();
            BigInteger transfer = relay.getMatter().getRelayTransfer();
            addTooltip.accept(Lang.RELAY_BONUS.translateColored(ChatFormatting.GRAY, formatEMC(bonus)));
            addTooltip.accept(Lang.EMC_TRANSFER_RATE.translateColored(ChatFormatting.GRAY, formatEMC(transfer)));
        }

        if(blockEntity instanceof BlockEntityOwnable ownable) {
            boolean isOwner = Objects.requireNonNull(Minecraft.getInstance().player).getUUID().equals(ownable.owner);
            String ownerName = ownable.ownerName;
            if (ownerName.isEmpty()) {
                ownerName = ownable.owner.toString();
            }
            addTooltip.accept(Lang.OWNER.translateColored(ChatFormatting.GRAY, Component.literal(ownerName).withStyle(isOwner ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED)));
        }
    }

    private static MutableComponent formatEMC(BigInteger value) {
        return EMCFormat.getComponent(value).withStyle(ChatFormatting.GREEN);
    }
}
