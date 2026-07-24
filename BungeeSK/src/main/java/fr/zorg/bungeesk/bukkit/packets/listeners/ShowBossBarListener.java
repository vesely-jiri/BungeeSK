package fr.zorg.bungeesk.bukkit.packets.listeners;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.api.BungeeSKBukkitListener;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.ShowBossBarPacket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

/**
 * Shows a timed boss bar on the local player (this is the server the proxy routed the packet to),
 * auto-removing it after the requested duration. Runs on the main thread — Bukkit's boss-bar API is
 * not thread-safe.
 */
public class ShowBossBarListener extends BungeeSKBukkitListener {

    @Override
    public void onReceive(BungeeSKPacket packet) {
        if (!(packet instanceof ShowBossBarPacket))
            return;
        final ShowBossBarPacket p = (ShowBossBarPacket) packet;
        Bukkit.getScheduler().runTask(BungeeSK.getInstance(), () -> {
            final Player player = Bukkit.getPlayer(p.getBungeePlayer().getUuid());
            if (player == null)
                return;
            final BossBar bar = Bukkit.createBossBar(
                    ChatColor.translateAlternateColorCodes('&', p.getTitle()),
                    parseColor(p.getColor()),
                    parseStyle(p.getStyle()));
            bar.setProgress(Math.max(0.0, Math.min(1.0, p.getProgress())));
            bar.addPlayer(player);
            bar.setVisible(true);
            Bukkit.getScheduler().runTaskLater(BungeeSK.getInstance(), bar::removeAll, Math.max(1L, p.getDurationTicks()));
        });
    }

    private static BarColor parseColor(String color) {
        if (color == null)
            return BarColor.PURPLE;
        try {
            return BarColor.valueOf(color.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            return BarColor.PURPLE;
        }
    }

    private static BarStyle parseStyle(String style) {
        if (style == null)
            return BarStyle.SOLID;
        try {
            return BarStyle.valueOf(style.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            return BarStyle.SOLID;
        }
    }

}
