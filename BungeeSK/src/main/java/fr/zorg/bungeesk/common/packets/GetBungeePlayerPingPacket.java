package fr.zorg.bungeesk.common.packets;

import fr.zorg.bungeesk.common.entities.BungeePlayer;

public class GetBungeePlayerPingPacket implements BungeeSKPacket {

    private final BungeePlayer bungeePlayer;

    public GetBungeePlayerPingPacket(BungeePlayer bungeePlayer) {
        this.bungeePlayer = bungeePlayer;
    }

    public BungeePlayer getBungeePlayer() {
        return this.bungeePlayer;
    }

}
