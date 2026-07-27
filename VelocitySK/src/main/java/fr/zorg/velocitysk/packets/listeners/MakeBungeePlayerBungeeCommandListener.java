package fr.zorg.velocitysk.packets.listeners;

import com.velocitypowered.api.proxy.Player;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.MakeBungeePlayerBungeeCommandPacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;

public class MakeBungeePlayerBungeeCommandListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof MakeBungeePlayerBungeeCommandPacket) {
            final MakeBungeePlayerBungeeCommandPacket makeBungeePlayerBungeeCommandPacket = (MakeBungeePlayerBungeeCommandPacket) packet;
            final BungeePlayer bungeePlayer = makeBungeePlayerBungeeCommandPacket.getPlayer();
            final String command = makeBungeePlayerBungeeCommandPacket.getCommand();
            final Player player = VelocityUtils.getManipulablePlayer(bungeePlayer);
            if (player == null)
                return;
            // A "proxy command" is one the proxy itself handles (e.g. /server, /lpv), so run it through
            // Velocity's CommandManager as the player. spoofChatInput sends the line to the player's
            // BACKEND server instead, which doesn't know the proxy's commands ("Unknown command").
            final String cmdLine = command.startsWith("/") ? command.substring(1) : command;
            BungeeSK.getServer().getCommandManager().executeAsync(player, cmdLine);
        }
    }

}