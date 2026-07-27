package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.ShowBossBarPacket;
import org.bukkit.event.Event;

@Name("Show a boss bar to a bungee player")
@Description({"Shows a timed boss bar to a player anywhere on the network, then removes it automatically.",
        "Colour is one of pink, blue, red, green, yellow, purple, white (default purple). Style is one of",
        "solid, segmented_6, segmented_10, segmented_12, segmented_20 (default solid). Progress is 0 to 1",
        "(default 1). Duration defaults to 5 seconds.",
        "Note: only works for players on a server running BungeeSK, which shows the boss bar."})
@Examples({"show boss bar \"&cEvent starts soon!\" to bungee player named \"Notch\"",
        "show boss bar \"&aLoading\" with colour \"green\" with progress 0.5 for 10 seconds to {_bp}"})
@Since("2.2.0")
public class EffShowBossBar extends Effect {

    static {
        Syntax.effect(EffShowBossBar.class, EffShowBossBar::new,
                "show [(bungee|proxy)] boss[ ]bar [title] %string% [with colo[u]r %-string%] [with style %-string%] [with progress %-number%] [for %-timespan%] to %bungeeplayers%");
    }

    private Expression<String> title;
    private Expression<String> color;
    private Expression<String> style;
    private Expression<Number> progress;
    private Expression<Timespan> duration;
    private Expression<BungeePlayer> players;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.title = (Expression<String>) exprs[0];
        this.color = (Expression<String>) exprs[1];
        this.style = (Expression<String>) exprs[2];
        this.progress = (Expression<Number>) exprs[3];
        this.duration = (Expression<Timespan>) exprs[4];
        this.players = (Expression<BungeePlayer>) exprs[5];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        final String title = this.title.getSingle(e);
        if (players.length == 0 || title == null)
            return;

        final String color = this.color == null ? null : this.color.getSingle(e);
        final String style = this.style == null ? null : this.style.getSingle(e);

        double progress = 1.0;
        if (this.progress != null) {
            final Number value = this.progress.getSingle(e);
            if (value != null)
                progress = Math.max(0.0, Math.min(1.0, value.doubleValue()));
        }

        long durationTicks = 100L; // 5 seconds
        if (this.duration != null) {
            final Timespan timespan = this.duration.getSingle(e);
            if (timespan != null)
                durationTicks = timespan.getAs(Timespan.TimePeriod.TICK);
        }

        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new ShowBossBarPacket(player, title, color, style, progress, durationTicks));
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "show boss bar " + this.title.toString(e, debug) + " to " + this.players.toString(e, debug);
    }

}
