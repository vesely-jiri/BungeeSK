package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.PlaySoundPacket;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
        final ProxiedPlayer player = BungeeUtils.getPlayer(p.getBungeePlayer());
        if (player == null || player.getServer() == null)
            return;
        final BungeeServer server = BungeeUtils.getServerFromInfo(player.getServer().getInfo());
        if (server == null)
            return;
        final SocketServer target = BungeeUtils.getSocketFromBungeeServer(server);
        if (target != null)
            target.sendPacket(p);
    }

}
