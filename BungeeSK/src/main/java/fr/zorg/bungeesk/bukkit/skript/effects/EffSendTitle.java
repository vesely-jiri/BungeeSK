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
import fr.zorg.bungeesk.common.packets.SendTitlePacket;
import org.bukkit.event.Event;

@Name("Send Bungeecord title to bungee player")
@Description({"Send a Bungeecord title to a player on the network",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("send bungee title \"&cHey you !\" with subtitle \"&6How are you ? :)\" to bungee player named \"Notch\" for 3 seconds with fade in 10 ticks and fade out 2 seconds")
@Since("1.1.0")
public class EffSendTitle extends Effect {

    static {
        Syntax.effect(EffSendTitle.class, EffSendTitle::new,
                "send (bungee|proxy) title %string% [with subtitle %-string%] to %bungeeplayers% [for %-timespan%] [with fade[(-| )]in %-timespan%] [(and|with) fade[(-| )]out %-timespan%]");
    }

    private Expression<String> title;
    private Expression<String> subTitle;
    private Expression<Timespan> time;
    private Expression<BungeePlayer> players;
    private Expression<Timespan> fadeIn;
    private Expression<Timespan> fadeOut;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.title = (Expression<String>) exprs[0];
        this.subTitle = (Expression<String>) exprs[1];
        this.players = (Expression<BungeePlayer>) exprs[2];
        this.time = (Expression<Timespan>) exprs[3];
        this.fadeIn = (Expression<Timespan>) exprs[4];
        this.fadeOut = (Expression<Timespan>) exprs[5];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        if (players.length == 0)
            return;

        final String title = this.title == null ? "NONE" : this.title.getSingle(e);
        final String subTitle = this.subTitle == null ? null : this.subTitle.getSingle(e);
        final Long time = this.time == null ? null : this.time.getSingle(e).getAs(Timespan.TimePeriod.TICK);
        final Long fadeIn = this.fadeIn == null ? null : this.fadeIn.getSingle(e).getAs(Timespan.TimePeriod.TICK);
        final Long fadeOut = this.fadeOut == null ? null : this.fadeOut.getSingle(e).getAs(Timespan.TimePeriod.TICK);

        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new SendTitlePacket(player, title, subTitle, time, fadeIn, fadeOut));
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "send title " + this.title.toString(e, debug) + " to " + this.players.toString(e, debug);
    }

}