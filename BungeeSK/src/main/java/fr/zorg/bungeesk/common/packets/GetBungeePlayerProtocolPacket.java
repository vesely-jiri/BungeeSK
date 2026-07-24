package fr.zorg.bungeesk.common.packets;

import fr.zorg.bungeesk.common.entities.BungeePlayer;

public class GetBungeePlayerProtocolPacket implements BungeeSKPacket {

    private final BungeePlayer bungeePlayer;

    public GetBungeePlayerProtocolPacket(BungeePlayer bungeePlayer) {
        this.bungeePlayer = bungeePlayer;
    }

    public BungeePlayer getBungeePlayer() {
        return this.bungeePlayer;
    }

}
