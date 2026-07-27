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
import fr.zorg.bungeesk.common.packets.SendActionBarPacket;
import org.bukkit.event.Event;

@Name("Send action bar message")
@Description({"Send an action bar message to a player on the bungeecord network",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("send bungee player named \"Notch\" action bar \"&6Welcome ! :)\"")
@Since("1.1.0")
public class EffSendActionBar extends Effect {

    static {
        Syntax.effect(EffSendActionBar.class, EffSendActionBar::new,
                "send [bungee] action bar %string% to %bungeeplayers%");
    }

    private Expression<BungeePlayer> players;
    private Expression<String> message;

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.message = (Expression<String>) exprs[0];
        this.players = (Expression<BungeePlayer>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        final String message = this.message.getSingle(e);
        if (players.length == 0 || message == null)
            return;

        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new SendActionBarPacket(player, message));
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "send action bar " + this.message.toString(e, debug) + " to " + this.players.toString(e, debug);
    }

}