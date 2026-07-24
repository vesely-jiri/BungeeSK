package fr.zorg.bungeesk.common.packets;

import fr.zorg.bungeesk.common.entities.BungeePlayer;

/**
 * Shows a timed boss bar to a networked player. Sent server → proxy → the player's game server,
 * which creates a Bukkit boss bar, shows it and auto-removes it after {@link #getDurationTicks()}.
 * {@code color}/{@code style} may be null (the game server falls back to defaults).
 */
public class ShowBossBarPacket implements BungeeSKPacket {

    private final BungeePlayer bungeePlayer;
    private final String title;
    private final String color;
    private final String style;
    private final double progress;
    private final long durationTicks;

    public ShowBossBarPacket(BungeePlayer bungeePlayer, String title, String color, String style, double progress, long durationTicks) {
        this.bungeePlayer = bungeePlayer;
        this.title = title;
        this.color = color;
        this.style = style;
        this.progress = progress;
        this.durationTicks = durationTicks;
    }

    public BungeePlayer getBungeePlayer() {
        return this.bungeePlayer;
    }

    public String getTitle() {
        return this.title;
    }

    public String getColor() {
        return this.color;
    }

    public String getStyle() {
        return this.style;
    }

    public double getProgress() {
        return this.progress;
    }

    public long getDurationTicks() {
        return this.durationTicks;
    }

}
