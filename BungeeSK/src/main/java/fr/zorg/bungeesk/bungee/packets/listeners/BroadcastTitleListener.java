package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.packets.BroadcastTitlePacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.Collections;

public class BroadcastTitleListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof BroadcastTitlePacket))
            return;
        final BroadcastTitlePacket p = (BroadcastTitlePacket) packet;

        final Collection<ProxiedPlayer> targets;
        if (p.getServerName() == null) {
            targets = BungeeSK.getInstance().getProxy().getPlayers();
        } else {
            final ServerInfo serverInfo = BungeeSK.getInstance().getProxy().getServerInfo(p.getServerName());
            targets = serverInfo == null ? Collections.emptyList() : serverInfo.getPlayers();
        }
        if (targets.isEmpty())
            return;

        final Title title = BungeeSK.getInstance().getProxy().createTitle();
        title.title(BungeeUtils.getTextComponent(p.getTitle()));
        if (p.getSubTitle() != null)
            title.subTitle(BungeeUtils.getTextComponent(p.getSubTitle()));
        if (p.getStayTicks() != null)
            title.stay(p.getStayTicks().intValue());
        if (p.getFadeInTicks() != null)
            title.fadeIn(p.getFadeInTicks().intValue());
        if (p.getFadeOutTicks() != null)
            title.fadeOut(p.getFadeOutTicks().intValue());

        targets.forEach(player -> player.sendTitle(title));
    }

}
