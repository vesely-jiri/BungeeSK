package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.GetBungeePlayerProtocolPacket;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;

import java.util.UUID;

public class GetBungeePlayerProtocolListener extends BungeeSKListener {

    @Override
    public Object onFutureRequest(UUID uuid, SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof GetBungeePlayerProtocolPacket) {
            final Player player = VelocityUtils.getPlayer(((GetBungeePlayerProtocolPacket) packet).getBungeePlayer().getUuid());
            if (player == null)
                return new EmptyFutureResponse();
            return (long) player.getProtocolVersion().getProtocol();
        }
        return null;
    }

}
