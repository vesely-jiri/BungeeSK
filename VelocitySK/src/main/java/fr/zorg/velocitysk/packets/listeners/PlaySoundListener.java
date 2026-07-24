package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.PlaySoundPacket;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;

import java.util.Optional;

/**
 * Forwards a {@link PlaySoundPacket} down to the game server the target player is on, where the
 * Bukkit side plays it locally.
 */
public class PlaySoundListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof PlaySoundPacket))
            return;
        final PlaySoundPacket p = (PlaySoundPacket) packet;
        final Player player = VelocityUtils.getPlayer(p.getBungeePlayer().getUuid());
        if (player == null)
            return;
        final Optional<ServerConnection> connection = player.getCurrentServer();
        if (!connection.isPresent())
            return;
        final BungeeServer server = VelocityUtils.getServerFromInfo(connection.get().getServerInfo());
        if (server == null)
            return;
        final SocketServer target = VelocityUtils.getSocketFromBungeeServer(server);
        if (target != null)
            target.sendPacket(p);
    }

}
