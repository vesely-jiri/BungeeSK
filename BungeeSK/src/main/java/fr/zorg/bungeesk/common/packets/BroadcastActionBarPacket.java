package fr.zorg.bungeesk.common.packets;

/**
 * Shows an action bar to every player on the network, or to every player on one server when
 * {@link #getServerName()} is non-null.
 */
public class BroadcastActionBarPacket implements BungeeSKPacket {

    private final String message;
    private final String serverName;

    public BroadcastActionBarPacket(String message, String serverName) {
        this.message = message;
        this.serverName = serverName;
    }

    public String getMessage() {
        return this.message;
    }

    /** The target server's name, or null to broadcast to the whole network. */
    public String getServerName() {
        return this.serverName;
    }

}
