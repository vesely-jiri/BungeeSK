package fr.zorg.bungeesk.bungee.packets.listeners;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.bungee.api.BungeeSKListener;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.bungee.utils.BungeeUtils;
import fr.zorg.bungeesk.common.packets.BroadcastActionBarPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.Collections;

public class BroadcastActionBarListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof BroadcastActionBarPacket))
            return;
        final BroadcastActionBarPacket p = (BroadcastActionBarPacket) packet;

        final Collection<ProxiedPlayer> targets;
        if (p.getServerName() == null) {
            targets = BungeeSK.getInstance().getProxy().getPlayers();
        } else {
            final ServerInfo serverInfo = BungeeSK.getInstance().getProxy().getServerInfo(p.getServerName());
            targets = serverInfo == null ? Collections.emptyList() : serverInfo.getPlayers();
        }
        if (targets.isEmpty())
            return;

        final BaseComponent[] component = BungeeUtils.getTextComponent(p.getMessage());
        targets.forEach(player -> player.sendMessage(ChatMessageType.ACTION_BAR, component));
    }

}
