package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeConfig;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.packets.BroadcastToNetworkPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import net.md_5.bungee.api.chat.BaseComponent;

public class BroadcastToNetworkListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof BroadcastToNetworkPacket) {
            final String message = ((BroadcastToNetworkPacket) packet).getMessage();

            // Default: proxy-wide broadcast, reaching every player regardless of backend.
            if (BungeeConfig.AFFECT_ALL_SERVERS.get()) {
                BungeeSK.getInstance().getProxy().broadcast(BungeeUtils.getTextComponent(message));
                return;
            }

            // Restricted: only players whose current server is connected through BungeeSK.
            final BaseComponent[] component = BungeeUtils.getTextComponent(message);
            BungeeSK.getInstance().getProxy().getPlayers().forEach(player -> {
                if (BungeeUtils.isOnBungeeSkServer(player))
                    player.sendMessage(component);
            });
        }
    }

}
