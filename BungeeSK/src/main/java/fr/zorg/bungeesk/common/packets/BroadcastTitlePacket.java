package fr.zorg.bungeesk.common.packets;

/**
 * Shows a title to every player on the network, or to every player on one server when
 * {@link #getServerName()} is non-null. Times are in ticks (null = client default).
 */
public class BroadcastTitlePacket implements BungeeSKPacket {

    private final String title;
    private final String subTitle;
    private final Long stayTicks;
    private final Long fadeInTicks;
    private final Long fadeOutTicks;
    private final String serverName;

    public BroadcastTitlePacket(String title, String subTitle, Long stayTicks, Long fadeInTicks, Long fadeOutTicks, String serverName) {
        this.title = title;
        this.subTitle = subTitle;
        this.stayTicks = stayTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.serverName = serverName;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSubTitle() {
        return this.subTitle;
    }

    public Long getStayTicks() {
        return this.stayTicks;
    }

    public Long getFadeInTicks() {
        return this.fadeInTicks;
    }

    public Long getFadeOutTicks() {
        return this.fadeOutTicks;
    }

    /** The target server's name, or null to broadcast to the whole network. */
    public String getServerName() {
        return this.serverName;
    }

}
