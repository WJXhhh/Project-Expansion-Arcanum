package com.wjx.neoforge.projectexa.item;

import com.wjx.neoforge.projectexa.util.IHasMatter;
import com.wjx.neoforge.projectexa.util.Lang;
import com.wjx.neoforge.projectexa.util.Matter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class ItemCompressedCollector extends Item implements IHasMatter {
    public final Matter matter;
    public ItemCompressedCollector(Matter matter) {
        super(new Properties().rarity(matter.getRarity()));
        this.matter = matter;
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, context, list, flag);
        list.add(Lang.Items.COMRESSED_COLLECTOR_TOOLTIP.translateColored(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
