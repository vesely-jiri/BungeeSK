package fr.zorg.bungeesk.bungee.utils;

import fr.zorg.bungeesk.bungee.BungeeSK;
import fr.zorg.bungeesk.bungee.packets.PacketServer;
import fr.zorg.bungeesk.bungee.packets.SocketServer;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BungeeUtils {

    /** Matches the {@code &#rrggbb} short hex form. */
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    /**
     * Detects a real MiniMessage tag, e.g. {@code <red>}, {@code <#ff0000>}, {@code <gradient:#a:#b>}
     * or {@code </bold>}. The tag name must start with a letter or {@code '#'} and contain no
     * whitespace, so literal text like {@code <IP:PORT / ALL>} is not treated as MiniMessage.
     */
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("</?[a-zA-Z#][^<>\\s]*>");

    public static ProxiedPlayer getPlayer(BungeePlayer bungeePlayer) {
        final ProxiedPlayer player = BungeeSK.getInstance().getProxy().getPlayer(bungeePlayer.getName());
        return player != null && player.isConnected() ? player : null;
    }

    public static BungeePlayer getBungeePlayer(ProxiedPlayer player) {
        return new BungeePlayer(player.getName(), player.getUniqueId());
    }

    /**
     * Builds one {@link BaseComponent} array per input string, each rendered from a format string.
     * <p>
     * Colour handling follows this precedence: legacy {@code '&'} colour/format codes are translated
     * (including hex colours in both the {@code &#rrggbb} and {@code &x&r&r&g&g&b&b} forms) and the
     * result is passed through {@link TextComponent#fromLegacyText(String)}.
     * <p>
     * Supports MiniMessage tags (hex, gradients, …) as well as legacy {@code '&'} codes and hex
     * colours; see {@link #getTextComponent(String)} for the precedence.
     *
     * @param text one or more raw format strings (individual entries may be null)
     * @return an array of component arrays, one entry per input string
     */
    public static BaseComponent[][] getTextComponents(String... text) {
        final BaseComponent[][] components = new BaseComponent[text.length][];
        for (int i = 0; i < text.length; i++)
            components[i] = getTextComponent(text[i]);
        return components;
    }

    /**
     * Renders a single format string into a {@link BaseComponent} array.
     * <p>
     * Precedence: if the string contains a MiniMessage tag it is parsed as <b>MiniMessage</b> (hex,
     * gradients, etc.) and converted to BungeeCord components via {@link BungeeComponentSerializer};
     * otherwise legacy {@code '&'} codes are translated (with hex in both the {@code &#rrggbb} and
     * {@code &x&r&r&g&g&b&b} forms) and converted via {@link TextComponent#fromLegacyText(String)}.
     * Malformed MiniMessage falls back to the legacy path so a message is never dropped. Null/empty
     * input yields an empty component array.
     *
     * @param text the raw format string (may be null)
     * @return the rendered component array, never null
     */
    public static BaseComponent[] getTextComponent(String text) {
        if (text == null || text.isEmpty())
            return TextComponent.fromLegacyText("");
        // Strings already carrying legacy section-sign codes are legacy, not MiniMessage (the strict
        // MiniMessage parser rejects §), so skip straight to the legacy path.
        if (text.indexOf('§') < 0 && MINIMESSAGE_TAG.matcher(text).find()) {
            try {
                final Component component = MiniMessage.miniMessage().deserialize(text);
                final String json = GsonComponentSerializer.gson().serialize(component);
                return ComponentSerializer.parse(json);
            } catch (RuntimeException ex) {
                // Malformed MiniMessage – fall back to legacy so we never fail to send a message.
                BungeeSK.getInstance().getLogger().warning("Could not render MiniMessage \"" + text + "\": " + ex + " — using legacy formatting instead.");
            }
        }
        return TextComponent.fromLegacyText(translateColors(text));
    }

    /**
     * Translates a raw format string using {@code '&'} into a section-sign legacy string that
     * {@link TextComponent#fromLegacyText(String)} understands, including hex colours.
     * <p>
     * The {@code &#rrggbb} short form is expanded to BungeeCord's native {@code §x§r§r§g§g§b§b}
     * sequence; the already-expanded {@code &x&r&r&g&g&b&b} form and all other {@code &<code>}
     * sequences are converted by turning {@code '&'} into the section sign.
     *
     * @param text the raw format string (must not be null)
     * @return the translated section-sign string
     */
    public static String translateColors(String text) {
        final Matcher matcher = HEX_PATTERN.matcher(text);
        final StringBuilder sb = new StringBuilder(text.length() + 16);
        while (matcher.find()) {
            final String hex = matcher.group(1);
            final StringBuilder replacement = new StringBuilder("§x");
            for (int i = 0; i < hex.length(); i++)
                replacement.append('§').append(hex.charAt(i));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static BungeeServer getServerFromName(String name) {
        final ServerInfo serverInfo = BungeeSK.getInstance().getProxy().getServerInfo(name);
        return serverInfo != null ? new BungeeServer(serverInfo.getAddress().getAddress(), serverInfo.getAddress().getPort(), serverInfo.getName()) : null;
    }

    public static BungeeServer getServerFromInfo(ServerInfo serverInfo) {
        return serverInfo != null ? new BungeeServer(serverInfo.getAddress().getAddress(), serverInfo.getAddress().getPort(), serverInfo.getName()) : null;
    }

    public static BungeeServer getServerFromAddress(String address, int port) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(address);
            final boolean isLocal = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress();
            final ServerInfo serverInfo = BungeeSK
                    .getInstance()
                    .getProxy()
                    .getServers()
                    .values()
                    .stream()
                    .filter(server ->
                            (server.getAddress().getAddress().getHostAddress().equalsIgnoreCase(address) ||
                                    server.getAddress().getAddress().isAnyLocalAddress() || server.getAddress().getAddress().isLoopbackAddress() && isLocal) &&
                                    server.getAddress().getPort() == port)
                    .findFirst()
                    .orElse(null);


            return serverInfo != null ? new BungeeServer(serverInfo.getAddress().getAddress(), serverInfo.getAddress().getPort(), serverInfo.getName()) : null;
        } catch (UnknownHostException ignored) {
        }
        return null;
    }

    public static BungeeServer getServerFromSocket(SocketServer socketServer) {
        return getServerFromAddress(socketServer.getSocket().getInetAddress().getHostAddress(), socketServer.getMinecraftPort());
    }

    public static ServerInfo getServerInfo(BungeeServer bungeeServer) {
        return BungeeSK
                .getInstance()
                .getProxy()
                .getServers()
                .values()
                .stream()
                .filter(server ->
                        server.getAddress().getAddress().getHostAddress().equalsIgnoreCase(bungeeServer.getAddress().getHostAddress()) &&
                                server.getAddress().getPort() == bungeeServer.getPort())
                .findFirst()
                .orElse(null);
    }

    public static SocketServer getSocketFromBungeeServer(BungeeServer bungeeServer) {

        final boolean isLocal = bungeeServer.getAddress().isLoopbackAddress() || bungeeServer.getAddress().isAnyLocalAddress();

        return PacketServer
                .getClientSockets()
                .stream()
                .filter(clientSocket ->
                        (clientSocket.getSocket().getInetAddress().getHostAddress().equalsIgnoreCase(
                                bungeeServer
                                        .getAddress()
                                        .getHostAddress()
                        ) ||
                                (clientSocket.getSocket().getInetAddress().isAnyLocalAddress() || clientSocket.getSocket().getInetAddress().isLoopbackAddress() && isLocal)) &&
                                clientSocket.getMinecraftPort() == bungeeServer.getPort()
                )
                .findFirst().orElse(null);

    }

}