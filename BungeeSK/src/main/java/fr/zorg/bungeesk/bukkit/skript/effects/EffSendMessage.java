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
import fr.zorg.bungeesk.common.packets.SendMessagePacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Send message to bungee player")
@Description({"Send a message to a bungee player on the network",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("send bungee message \"&6Hello !\" to bungee player named \"Notch\"")
@Since("1.0.0")
public class EffSendMessage extends Effect {

    static {
        Syntax.effect(EffSendMessage.class, EffSendMessage::new, "send (bungee|proxy) message %string% to %bungeeplayers%");
    }

    private Expression<BungeePlayer> players;
    private Expression<String> message;

    public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
        this.message = (Expression<String>) exprs[0];
        this.players = (Expression<BungeePlayer>) exprs[1];
        return true;
    }

    protected void execute(final Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        final String message = this.message.getSingle(e);
        if (players.length == 0 || message == null)
            return;
        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new SendMessagePacket(player, message));
    }

    public String toString(@Nullable Event e, boolean debug) {
        return "send bungee message " + message.toString(e, debug) + " to " + players.toString(e, debug);
    }

}