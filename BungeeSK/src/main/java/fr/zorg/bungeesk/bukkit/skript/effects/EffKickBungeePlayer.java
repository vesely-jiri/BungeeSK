package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.KickBungeePlayerPacket;
import org.bukkit.event.Event;

@Name("Kick bungee player")
@Description({"Kicks a player on the network from the network",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("kick bungee player named \"Notch\" from bungeecord due to \"&cYou're the fake Notch !\"")
@Since("1.1.0")
public class EffKickBungeePlayer extends Effect {

    static {
        Syntax.effect(EffKickBungeePlayer.class, EffKickBungeePlayer::new,
                "kick %bungeeplayers% from (bungee[cord]|proxy|[the] network) [(due to|because of) %-string%]");
    }

    private Expression<BungeePlayer> players;
    private Expression<String> reason;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<BungeePlayer>) exprs[0];
        this.reason = (Expression<String>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        if (players.length == 0)
            return;

        final String reason = this.reason == null ? null : this.reason.getSingle(e);
        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new KickBungeePlayerPacket(player, reason));
    }

    @Override
    public String toString(Event event, boolean b) {
        return "kick " + this.players.toString(event, b) + " from bungeecord";
    }

}