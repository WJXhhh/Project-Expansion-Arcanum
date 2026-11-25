package com.wjx.neoforge.projectexa.net;

import com.wjx.neoforge.projectexa.net.packets.IPacket;
import com.wjx.neoforge.projectexa.net.packets.to_client.*;
import com.wjx.neoforge.projectexa.net.packets.to_server.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.maven.artifact.versioning.ArtifactVersion;

// based on ProjectE's PacketHandler
// https://github.com/sinkillerj/ProjectE/blob/d90e367b058ade5631b709e5feb7d3eacedb6350/src/main/java/moze_intel/projecte/network/PacketHandler.java
@SuppressWarnings("unused")
public final class PacketHandler {

    public PacketHandler(IEventBus modEventBus, ArtifactVersion version) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            // Station slots, input actions, and EMC retrieval require the same protocol on both sides.
            PayloadRegistrar registrar = event.registrar(version + "-arcane-features-6");
            register(registrar);
        });
    }

    public void register(PayloadRegistrar registrar) {
        if (com.wjx.neoforge.projectexa.integrations.beyond.BeyondSupport.terminal())
            com.wjx.neoforge.projectexa.integrations.beyond.TerminalPackets.register(registrar);
        if (com.wjx.neoforge.projectexa.integrations.beyond.BeyondSupport.orders())
            com.wjx.neoforge.projectexa.integrations.beyond.craftlines.OrderPackets.register(registrar);
        registrar.playToServer(PacketArcaneAnvilName.TYPE, PacketArcaneAnvilName.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketArcaneSmithingTransfer.TYPE, PacketArcaneSmithingTransfer.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketJeiTransmute.TYPE, PacketJeiTransmute.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketJeiCreativeCursorAck.TYPE, PacketJeiCreativeCursorAck.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketWorldEmcPick.TYPE, PacketWorldEmcPick.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketJeiCreativeCursor.TYPE, PacketJeiCreativeCursor.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketJeiEmcConfig.TYPE, PacketJeiEmcConfig.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketRecipeMaterialTransmutation.TYPE, PacketRecipeMaterialTransmutation.STREAM_CODEC, IPacket::handle);
        registrar.optional().playToServer(PacketTaczTransmutation.TYPE, PacketTaczTransmutation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketStoneTableAction.TYPE, PacketStoneTableAction.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketOpenArcaneTransmutationTabletWithRecipe.TYPE, PacketOpenArcaneTransmutationTabletWithRecipe.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketArcaneTransmutationTabletRecipeTransfer.TYPE, PacketArcaneTransmutationTabletRecipeTransfer.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketArcaneTransmutationTabletSmallButton.TYPE, PacketArcaneTransmutationTabletSmallButton.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketCreateTeleportLocation.TYPE, PacketCreateTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketDeleteTeleportLocation.TYPE, PacketDeleteTeleportLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketOpenTransmutationTablet.TYPE, PacketOpenTransmutationTablet.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportBack.TYPE, PacketTeleportBack.STREAM_CODEC, IPacket::handle);
        registrar.playToServer(PacketTeleportToLocation.TYPE, PacketTeleportToLocation.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(ClearKnowledgePacket.TYPE, ClearKnowledgePacket.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketArcaneRecipeTransferResult.TYPE, PacketArcaneRecipeTransferResult.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketOpenAlchemicalBookGUI.TYPE, PacketOpenAlchemicalBookGUI.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketSyncAlchemicalBookLocations.TYPE, PacketSyncAlchemicalBookLocations.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateCondenserLock.TYPE, PacketUpdateCondenserLock.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowLong.TYPE, PacketUpdateWindowLong.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowInt.TYPE, PacketUpdateWindowInt.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(PacketUpdateWindowBigInteger.TYPE, PacketUpdateWindowBigInteger.STREAM_CODEC, IPacket::handle);
        registrar.playToClient(UpdateTransmutationTargetsPacket.TYPE, UpdateTransmutationTargetsPacket.STREAM_CODEC, IPacket::handle);
    }

}
