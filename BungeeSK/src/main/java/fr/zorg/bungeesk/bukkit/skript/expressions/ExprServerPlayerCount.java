package fr.zorg.bungeesk.bukkit.skript.expressions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.utils.CompletableFutureUtils;
import org.skriptlang.skript.registration.SyntaxInfo;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.bungeesk.common.packets.GetServerPlayerCountPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Server player count")
@Description("The number of players currently connected to a specific server on the network.")
@Examples({"send \"Lobby has %player count of bungee server named \"\"lobby\"\"% players\"",
        "set {_full} to (player count of bungee server named \"lobby\") >= 100"})
@Since("2.2.0")
public class ExprServerPlayerCount extends SimpleExpression<Long> {

    static {
        Syntax.expression(ExprServerPlayerCount.class, ExprServerPlayerCount::new, Long.class, SyntaxInfo.COMBINED,
                "[the] [bungee] player[s] count (of|on) %bungeeserver%",
                "%bungeeserver%'[s] [bungee] player[s] count");
    }

    private Expression<BungeeServer> server;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.server = (Expression<BungeeServer>) exprs[0];
        return true;
    }

    @Override
    @Nullable
    protected Long[] get(Event e) {
        final BungeeServer bungeeServer = this.server.getSingle(e);
        if (bungeeServer == null)
            return new Long[0];
        final Object response = CompletableFutureUtils.generateFuture(new GetServerPlayerCountPacket(bungeeServer));
        return response == null ? new Long[0] : new Long[]{(Long) response};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "player count of " + this.server.toString(e, debug);
    }
}
