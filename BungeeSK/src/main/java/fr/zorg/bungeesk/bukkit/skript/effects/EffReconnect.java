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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Reconnect to the proxy")
@Description({"Forces an immediate reconnection to the proxy using the last-used connection settings",
        "(or the ones from config.yml if auto-connect is enabled). Also re-enables auto-reconnect if a",
        "previous 'disconnect the client' had turned it off."})
@Examples({"command /reconnect:",
        "\ttrigger:",
        "\t\treconnect to the proxy"})
@Since("2.1.0")
public class EffReconnect extends Effect {

    static {
        Syntax.effect(EffReconnect.class, EffReconnect::new, "reconnect [to] [the] [bungee[cord]|proxy]");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected void execute(Event e) {
        PacketClient.reconnectNow();
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "reconnect to the proxy";
    }
}
