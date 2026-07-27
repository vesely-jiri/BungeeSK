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
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.SendBungeePlayerToServerPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Send bungee player to server")
@Description({"Send a player on the network to a specific server",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples("send bungee player named \"Zorg_btw\" to bungee server named \"lobby2\"")
@Since("1.0.0 - 1.1.0: Usage of BungeeServer type")
public class EffSendBungeePlayerToServer extends Effect {

    static {
        Syntax.effect(EffSendBungeePlayerToServer.class, EffSendBungeePlayerToServer::new,
                "send %bungeeplayers% to %bungeeserver%");
    }

    private Expression<BungeePlayer> players;
    private Expression<BungeeServer> server;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<BungeePlayer>) exprs[0];
        this.server = (Expression<BungeeServer>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        final BungeePlayer[] players = this.players.getArray(e);
        final BungeeServer server = this.server.getSingle(e);
        if (players.length == 0 || server == null)
            return;

        for (final BungeePlayer player : players)
            PacketClient.sendPacket(new SendBungeePlayerToServerPacket(player, server));
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "send " + players.toString(e, debug) + " to server " + server.toString(e, debug);
    }

}