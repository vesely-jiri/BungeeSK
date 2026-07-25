package fr.zorg.velocitysk.packets.listeners;

import fr.zorg.bungeesk.common.packets.BroadcastToNetworkPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.BungeeConfig;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;

public class BroadcastToNetworkListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof BroadcastToNetworkPacket) {
            final String message = ((BroadcastToNetworkPacket) packet).getMessage();

            // Default: proxy-wide broadcast, reaching every player regardless of backend.
            if (BungeeConfig.AFFECT_ALL_SERVERS.get()) {
                BungeeSK.getServer().sendMessage(VelocityUtils.getTextComponent(message));
                return;
            }

            // Restricted: only players whose current server is connected through BungeeSK.
            final Component component = VelocityUtils.getTextComponent(message);
            BungeeSK.getServer().getAllPlayers().forEach(player -> {
                if (VelocityUtils.isOnBungeeSkServer(player))
                    player.sendMessage(component);
            });
        }
    }

}
