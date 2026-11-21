package github.iri.indicate.network;

import github.iri.indicate.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.*;
import pro.komaru.tridot.common.networking.*;

public class IndicateNetwork extends AbstractPacketHandler {
    public static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Indicate.loc("network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, IndicatorPacket.class, IndicatorPacket::encode, IndicatorPacket::decode, IndicatorPacket::handle);
    }

    public static void sendTracking(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
