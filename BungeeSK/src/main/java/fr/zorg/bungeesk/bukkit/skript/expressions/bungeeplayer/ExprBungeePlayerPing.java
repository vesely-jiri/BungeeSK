package fr.zorg.bungeesk.bukkit.skript.expressions.bungeeplayer;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import fr.zorg.bungeesk.bukkit.skript.Syntax;
import fr.zorg.bungeesk.bukkit.utils.CompletableFutureUtils;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.GetBungeePlayerPingPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Ping of BungeePlayer")
@Description("Gets a network player's ping (latency) to the proxy, in milliseconds.")
@Examples("send \"%(bungee player named \"\"Notch\"\")'s bungee ping% ms\"")
@Since("2.2.0")
public class ExprBungeePlayerPing extends SimplePropertyExpression<BungeePlayer, Long> {

    static {
        Syntax.property(ExprBungeePlayerPing.class,
                Long.class,
                "[bungee] ping",
                "bungeeplayer");
    }

    @Nullable
    @Override
    public Long convert(BungeePlayer player) {
        if (player == null)
            return null;
        final GetBungeePlayerPingPacket packet = new GetBungeePlayerPingPacket(player);
        return (Long) CompletableFutureUtils.generateFuture(packet);
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    protected String getPropertyName() {
        return "bungee player's ping";
    }

}
