package fr.zorg.bungeesk.bukkit.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.BroadcastActionBarPacket;
import org.bukkit.event.Event;

@Name("Broadcast action bar to the network or a server")
@Description({"Shows an action bar message to every player on the network, or to every player on a specific server.",
        "Note: reaches players even on servers without BungeeSK, unless 'affect_all_servers' is disabled in the proxy config."})
@Examples({"broadcast action bar \"&eServer restarting soon\" to the network",
        "broadcast action bar \"&aWelcome to the lobby\" to bungee server named \"lobby\""})
@Since("2.2.0")
public class EffBroadcastActionBar extends Effect {

    static {
        Skript.registerEffect(EffBroadcastActionBar.class,
                "broadcast [bungee[cord]] action bar [message] %string% to [the] network",
                "broadcast [bungee[cord]] action bar [message] %string% to %bungeeserver%");
    }

    private Expression<String> message;
    private Expression<BungeeServer> server;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.message = (Expression<String>) exprs[0];
        this.server = pattern == 1 ? (Expression<BungeeServer>) exprs[1] : null;
        return true;
    }

    @Override
    protected void execute(Event e) {
        final String message = this.message.getSingle(e);
        if (message == null)
            return;

        String serverName = null;
        if (this.server != null) {
            final BungeeServer bungeeServer = this.server.getSingle(e);
            if (bungeeServer == null)
                return;
            serverName = bungeeServer.getName();
        }

        PacketClient.sendPacket(new BroadcastActionBarPacket(message, serverName));
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "broadcast action bar " + this.message.toString(e, debug) + (this.server == null ? " to the network" : " to " + this.server.toString(e, debug));
    }

}
