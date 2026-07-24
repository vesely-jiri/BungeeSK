package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.GetBungeePlayerProtocolPacket;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class GetBungeePlayerProtocolListener extends BungeeSKListener {

    @Override
    public Object onFutureRequest(UUID uuid, SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof GetBungeePlayerProtocolPacket) {
            final ProxiedPlayer player = BungeeUtils.getPlayer(((GetBungeePlayerProtocolPacket) packet).getBungeePlayer());
            if (player == null)
                return new EmptyFutureResponse();
            return (long) player.getPendingConnection().getVersion();
        }
        return null;
    }

}
