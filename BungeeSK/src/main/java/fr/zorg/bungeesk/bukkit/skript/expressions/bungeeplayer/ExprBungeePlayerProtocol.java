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
import fr.zorg.bungeesk.common.packets.GetBungeePlayerProtocolPacket;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Protocol version of BungeePlayer")
@Description({"Gets a network player's client protocol version number (e.g. 767 for 1.21).",
        "See https://minecraft.wiki/w/Protocol_version for the number-to-version mapping."})
@Examples("send \"Protocol: %(bungee player named \"\"Notch\"\")'s protocol version%\"")
@Since("2.2.0")
public class ExprBungeePlayerProtocol extends SimplePropertyExpression<BungeePlayer, Long> {

    static {
        Syntax.property(ExprBungeePlayerProtocol.class,
                Long.class,
                "protocol [version]",
                "bungeeplayer");
    }

    @Nullable
    @Override
    public Long convert(BungeePlayer player) {
        if (player == null)
            return null;
        final GetBungeePlayerProtocolPacket packet = new GetBungeePlayerProtocolPacket(player);
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
        return "bungee player's protocol version";
    }

}
