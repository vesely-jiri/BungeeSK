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
import fr.zorg.bungeesk.bukkit.utils.CompletableFutureUtils;
import fr.zorg.bungeesk.common.packets.GetNetworkPlayerCountPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Network player count")
@Description("The total number of players currently online across the whole network.")
@Examples("send \"There are %network player count% players online\" to the network")
@Since("2.2.0")
public class ExprNetworkPlayerCount extends SimpleExpression<Long> {

    static {
        Skript.registerExpression(ExprNetworkPlayerCount.class, Long.class, ExpressionType.SIMPLE,
                "[the] (network|total) [online] [bungee] player[s] count",
                "[the] [online] [bungee] player count of [the] network");
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    @Nullable
    protected Long[] get(Event e) {
        final Object response = CompletableFutureUtils.generateFuture(new GetNetworkPlayerCountPacket());
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
        return "network player count";
    }
}
