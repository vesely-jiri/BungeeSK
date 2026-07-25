package fr.zorg.velocitysk.packets.listeners;

import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.BroadcastToNetworkPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.PacketServer;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.BungeeConfig;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BroadcastToNetworkListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof BroadcastToNetworkPacket) {
            final String message = ((BroadcastToNetworkPacket) packet).getMessage();

            // Default: proxy-wide broadcast, reaching every player regardless of backend.
            if (BungeeConfig.BROADCAST$NETWORK_INCLUDES_ALL_SERVERS.get()) {
                BungeeSK.getServer().sendMessage(VelocityUtils.getTextComponent(message));
                return;
            }

            // Opt-in: only players whose current server is connected through BungeeSK.
            final Set<String> bungeeSkServers = PacketServer.getClientSockets().stream()
                    .filter(SocketServer::isAuthenticated)
                    .map(VelocityUtils::getServerFromSocket)
                    .filter(Objects::nonNull)
                    .map(BungeeServer::getName)
                    .collect(Collectors.toSet());
            final Component component = VelocityUtils.getTextComponent(message);
            BungeeSK.getServer().getAllPlayers().forEach(player ->
                    player.getCurrentServer().ifPresent(connection -> {
                        if (bungeeSkServers.contains(connection.getServerInfo().getName()))
                            player.sendMessage(component);
                    }));
        }
    }

}
