package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.GetBungeePlayerPingPacket;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class GetBungeePlayerPingListener extends BungeeSKListener {

    @Override
    public Object onFutureRequest(UUID uuid, SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof GetBungeePlayerPingPacket) {
            final ProxiedPlayer player = BungeeUtils.getPlayer(((GetBungeePlayerPingPacket) packet).getBungeePlayer());
            if (player == null)
                return new EmptyFutureResponse();
            return (long) player.getPing();
        }
        return null;
    }

}
