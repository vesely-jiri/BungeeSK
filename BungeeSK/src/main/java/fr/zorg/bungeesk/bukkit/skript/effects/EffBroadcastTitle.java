package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.BroadcastTitlePacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Broadcast title to the network or a server")
@Description("Shows a title to every player on the network, or to every player on a specific server.")
@Examples({"broadcast title \"&aRestart\" with subtitle \"&7in 60s\" to the network",
        "broadcast title \"&cEvent starting!\" for 5 seconds to bungee server named \"lobby\""})
@Since("2.2.0")
public class EffBroadcastTitle extends Effect {

    static {
        Skript.registerEffect(EffBroadcastTitle.class,
                "broadcast [bungee[cord]] title %string% [with subtitle %-string%] [for %-timespan%] [with fade-in %-timespan%] [(and|with) fade-out %-timespan%] to [the] network",
                "broadcast [bungee[cord]] title %string% [with subtitle %-string%] [for %-timespan%] [with fade-in %-timespan%] [(and|with) fade-out %-timespan%] to %bungeeserver%");
    }

    private Expression<String> title;
    private Expression<String> subTitle;
    private Expression<Timespan> stay;
    private Expression<Timespan> fadeIn;
    private Expression<Timespan> fadeOut;
    private Expression<BungeeServer> server;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.title = (Expression<String>) exprs[0];
        this.subTitle = (Expression<String>) exprs[1];
        this.stay = (Expression<Timespan>) exprs[2];
        this.fadeIn = (Expression<Timespan>) exprs[3];
        this.fadeOut = (Expression<Timespan>) exprs[4];
        this.server = pattern == 1 ? (Expression<BungeeServer>) exprs[5] : null;
        return true;
    }

    @Override
    protected void execute(Event e) {
        final String title = this.title == null ? "" : this.title.getSingle(e);
        final String subTitle = this.subTitle == null ? null : this.subTitle.getSingle(e);
        final Long stay = ticks(this.stay, e);
        final Long fadeIn = ticks(this.fadeIn, e);
        final Long fadeOut = ticks(this.fadeOut, e);

        String serverName = null;
        if (this.server != null) {
            final BungeeServer bungeeServer = this.server.getSingle(e);
            if (bungeeServer == null)
                return;
            serverName = bungeeServer.getName();
        }

        PacketClient.sendPacket(new BroadcastTitlePacket(title, subTitle, stay, fadeIn, fadeOut, serverName));
    }

    @Nullable
    private static Long ticks(@Nullable Expression<Timespan> expr, Event e) {
        if (expr == null)
            return null;
        final Timespan timespan = expr.getSingle(e);
        return timespan == null ? null : timespan.getTicks_i();
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "broadcast title " + this.title.toString(e, debug) + (this.server == null ? " to the network" : " to " + this.server.toString(e, debug));
    }

}
