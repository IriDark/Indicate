package github.iri.indicate.network;

import github.iri.indicate.*;
import net.minecraft.network.protocol.common.custom.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.network.*;
import net.neoforged.neoforge.network.event.*;
import net.neoforged.neoforge.network.registration.*;
import pro.komaru.tridot.api.networking.*;
import pro.komaru.tridot.common.networking.*;

public class IndicateNetwork extends AbstractPacketHandler {
    public static final String PROTOCOL = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = registrar(event, Indicate.ID, PROTOCOL);
        registrar.playToClient(IndicatorPacket.TYPE, IndicatorPacket.STREAM_CODEC, Packet::handle);
    }

    public static void sendTracking(Entity entity, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
