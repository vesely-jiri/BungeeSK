package fr.zorg.bungeesk.bukkit.packets.listeners;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.api.BungeeSKBukkitListener;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.PlaySoundPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Plays a {@link PlaySoundPacket}'s sound on the local player (this is the server the proxy routed
 * the packet to). Runs on the main thread — Bukkit's sound API is not thread-safe.
 */
public class PlaySoundListener extends BungeeSKBukkitListener {

    @Override
    public void onReceive(BungeeSKPacket packet) {
        if (!(packet instanceof PlaySoundPacket))
            return;
        final PlaySoundPacket p = (PlaySoundPacket) packet;
        Bukkit.getScheduler().runTask(BungeeSK.getInstance(), () -> {
            final Player player = Bukkit.getPlayer(p.getBungeePlayer().getUuid());
            if (player == null)
                return;
            player.playSound(player.getLocation(), p.getSound(), p.getVolume(), p.getPitch());
        });
    }

}
