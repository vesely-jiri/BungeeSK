package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.zorg.bungeesk.common.packets.BroadcastActionBarPacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class BroadcastActionBarListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (!(packet instanceof BroadcastActionBarPacket))
            return;
        final BroadcastActionBarPacket p = (BroadcastActionBarPacket) packet;
        final Component component = VelocityUtils.getTextComponent(p.getMessage());

        if (p.getServerName() == null) {
            BungeeSK.getServer().sendActionBar(component);
            return;
        }
        final Optional<RegisteredServer> server = BungeeSK.getServer().getServer(p.getServerName());
        server.ifPresent(registeredServer -> registeredServer.getPlayersConnected().forEach(player -> player.sendActionBar(component)));
    }

}
