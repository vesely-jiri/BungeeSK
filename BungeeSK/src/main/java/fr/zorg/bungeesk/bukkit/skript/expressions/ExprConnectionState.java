package fr.zorg.bungeesk.bukkit.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Bungee connection state")
@Description({"The current state of this server's connection to the proxy.",
        "Returns one of: \"connected\", \"connecting\", \"reconnecting\" or \"disconnected\"."})
@Examples({"on load:",
        "\tif bungee connection state is \"disconnected\":",
        "\t\tsend \"Not linked to the proxy yet\" to console"})
@Since("2.1.0")
public class ExprConnectionState extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprConnectionState.class, String.class, ExpressionType.SIMPLE,
                "[the] [bungee[cord]|proxy] connection (state|status)");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    @Nullable
    protected String[] get(Event e) {
        return new String[]{PacketClient.getState().name().toLowerCase()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "bungee connection state";
    }
}
