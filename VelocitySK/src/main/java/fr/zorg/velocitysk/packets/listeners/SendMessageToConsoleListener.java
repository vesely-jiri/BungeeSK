package fr.zorg.velocitysk.packets.listeners;

import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.SendMessageToConsolePacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.api.BungeeSKListener;
import fr.zorg.velocitysk.packets.SocketServer;
import fr.zorg.velocitysk.utils.VelocityUtils;

public class SendMessageToConsoleListener extends BungeeSKListener {

    @Override
    public void onReceive(SocketServer socketServer, BungeeSKPacket packet) {
        if (packet instanceof SendMessageToConsolePacket) {
            final SendMessageToConsolePacket sendMessageToConsolePacket = (SendMessageToConsolePacket) packet;
            final String message = sendMessageToConsolePacket.getMessage();
            // Render the format string (MiniMessage/legacy/hex) down to plain text for the slf4j console logger.
            BungeeSK.getLogger().info(VelocityUtils.formatPlain(message));
        }
    }

}