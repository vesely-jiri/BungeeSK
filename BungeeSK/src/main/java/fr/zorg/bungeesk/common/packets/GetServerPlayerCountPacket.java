package fr.zorg.bungeesk.common.packets;

import fr.zorg.bungeesk.common.entities.BungeeServer;

/**
 * Requests the number of players connected to a specific server on the network.
 */
public class GetServerPlayerCountPacket implements BungeeSKPacket {

    private final BungeeServer bungeeServer;

    public GetServerPlayerCountPacket(BungeeServer bungeeServer) {
        this.bungeeServer = bungeeServer;
    }

    public BungeeServer getBungeeServer() {
        return this.bungeeServer;
    }

}
