package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.zorg.bungeesk.common.packets.BroadcastTitlePacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.BungeeConfig;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class BroadcastTitleListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof BroadcastTitlePacket))
            return;
        final BroadcastTitlePacket p = (BroadcastTitlePacket) packet;
        final Title title = buildTitle(p);

        // Default: deliver through the whole-proxy / whole-server Audience.
        if (BungeeConfig.AFFECT_ALL_SERVERS.get()) {
            if (p.getServerName() == null) {
                BungeeSK.getServer().showTitle(title);
                return;
            }
            BungeeSK.getServer().getServer(p.getServerName())
                    .ifPresent(server -> server.getPlayersConnected().forEach(player -> player.showTitle(title)));
            return;
        }

        // Restricted: only players on BungeeSK-connected servers.
        final Collection<Player> targets;
        if (p.getServerName() == null) {
            targets = BungeeSK.getServer().getAllPlayers();
        } else {
            final Optional<RegisteredServer> server = BungeeSK.getServer().getServer(p.getServerName());
            targets = server.map(RegisteredServer::getPlayersConnected).orElseGet(Collections::emptyList);
        }
        targets.forEach(player -> {
            if (VelocityUtils.isOnBungeeSkServer(player))
                player.showTitle(title);
        });
    }

    private static Title buildTitle(BroadcastTitlePacket p) {
        final Component titleComponent = VelocityUtils.getTextComponent(p.getTitle());
        final Component subComponent = p.getSubTitle() == null ? Component.empty() : VelocityUtils.getTextComponent(p.getSubTitle());
        if (p.getStayTicks() == null && p.getFadeInTicks() == null && p.getFadeOutTicks() == null)
            return Title.title(titleComponent, subComponent);
        final Duration fadeIn = Duration.ofMillis((p.getFadeInTicks() == null ? 10L : p.getFadeInTicks()) * 50L);
        final Duration stay = Duration.ofMillis((p.getStayTicks() == null ? 70L : p.getStayTicks()) * 50L);
        final Duration fadeOut = Duration.ofMillis((p.getFadeOutTicks() == null ? 20L : p.getFadeOutTicks()) * 50L);
        return Title.title(titleComponent, subComponent, Title.Times.times(fadeIn, stay, fadeOut));
    }

}
