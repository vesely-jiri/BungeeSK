package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.GetServerPlayerCountPacket;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.UUID;

public class GetServerPlayerCountListener extends BungeeSKListener {

    @Override
    public Object onFutureRequest(UUID uuid, SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof GetServerPlayerCountPacket) {
            final ServerInfo serverInfo = BungeeSK.getInstance().getProxy()
                    .getServerInfo(((GetServerPlayerCountPacket) packet).getBungeeServer().getName());
            if (serverInfo == null)
                return new EmptyFutureResponse();
            return (long) serverInfo.getPlayers().size();
        }
        return null;
    }

}
