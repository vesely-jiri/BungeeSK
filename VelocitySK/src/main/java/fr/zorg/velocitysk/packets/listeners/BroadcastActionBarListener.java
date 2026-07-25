package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.zorg.bungeesk.common.packets.BroadcastActionBarPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.BungeeConfig;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class BroadcastActionBarListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof BroadcastActionBarPacket))
            return;
        final BroadcastActionBarPacket p = (BroadcastActionBarPacket) packet;
        final Component component = VelocityUtils.getTextComponent(p.getMessage());

        // Default: deliver through the whole-proxy / whole-server Audience.
        if (BungeeConfig.AFFECT_ALL_SERVERS.get()) {
            if (p.getServerName() == null) {
                BungeeSK.getServer().sendActionBar(component);
                return;
            }
            BungeeSK.getServer().getServer(p.getServerName())
                    .ifPresent(server -> server.getPlayersConnected().forEach(player -> player.sendActionBar(component)));
            return;
        }

        // Restricted: only players on BungeeSK-connected servers.
        final Collection<Player> targets;
        if (p.getServerName() == null) {
            targets = BungeeSK.getServer().getAllPlayers();
        } else {
            final Optional<RegisteredServer> server = BungeeSK.getServer().getServer(p.getServerName());
            targets = server.map(RegisteredServer::getPlayersConnected).orElseGet(Collections::emptyList);
        }
        targets.forEach(player -> {
            if (VelocityUtils.isOnBungeeSkServer(player))
                player.sendActionBar(component);
        });
    }

}
