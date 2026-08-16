package cool.furry.mc.forge.projectexpansion.net.packets.to_server;

import cool.furry.mc.forge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.forge.projectexpansion.net.packets.IPacket;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;

public record PacketArcaneTransmutationTabletOutputUpdate(int index, ItemStack stack) implements IPacket {
    @Override
    public void handle(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !(player.containerMenu instanceof ContainerArcaneTransmutationTablet container)
                || container.getPlayer() != player || !container.stillValid(player)) return;
        if (index < 0 || index >= 16 || stack == null || stack.isEmpty()) return;

        ItemStack output = stack.copyWithCount(1);
        long value = IEMCProxy.INSTANCE.getValue(output);
        if (value <= 0 || !container.getProvider().hasKnowledge(output)) return;
        if (container.transmutationInventory.getAvailableEmc().compareTo(BigInteger.valueOf(value)) < 0) return;

        container.transmutationInventory.writeIntoOutputSlot(index, output);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(index);
        buffer.writeItem(stack);
    }

    public static PacketArcaneTransmutationTabletOutputUpdate decode(FriendlyByteBuf buffer) {
        return new PacketArcaneTransmutationTabletOutputUpdate(buffer.readVarInt(), buffer.readItem());
    }
}
