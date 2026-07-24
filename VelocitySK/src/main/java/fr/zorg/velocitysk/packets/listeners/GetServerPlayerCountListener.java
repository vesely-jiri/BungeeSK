package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.GetServerPlayerCountPacket;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;

import java.util.UUID;

public class GetServerPlayerCountListener extends BungeeSKListener {

    @Override
    public Object onFutureRequest(UUID uuid, SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof GetServerPlayerCountPacket) {
            final RegisteredServer registeredServer = VelocityUtils.getRegisteredServer(((GetServerPlayerCountPacket) packet).getBungeeServer());
            if (registeredServer == null)
                return new EmptyFutureResponse();
            return (long) registeredServer.getPlayersConnected().size();
        }
        return null;
    }

}
