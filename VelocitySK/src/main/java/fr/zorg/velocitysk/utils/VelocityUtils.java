package fr.zorg.velocitysk.utils;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import fr.zorg.bungeesk.common.entities.BungeePlayer;
import fr.zorg.bungeesk.common.entities.BungeeServer;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.packets.PacketServer;
import fr.zorg.velocitysk.packets.SocketServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VelocityUtils {

    /**
     * Legacy serializer used to render '&' colour/format codes, including hex colours in
     * both the {@code &#rrggbb} and the {@code &x&r&r&g&g&b&b} forms.
     */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Detects a real MiniMessage tag such as {@code <red>}, {@code <#ff0000>},
     * {@code <gradient:#00ffcc:#0066ff>} or {@code </bold>}. The tag name must start with a letter
     * or {@code '#'} and contain no whitespace, so literal help text like {@code <IP:PORT / ALL>}
     * is NOT mistaken for MiniMessage (which previously routed it to the strict parser and crashed).
     */
    private static final Pattern MINIMESSAGE_TAG = Pattern.compile("</?[a-zA-Z#][^<>\\s]*>");

    /** The legacy section sign; strings already containing it are treated as legacy, never MiniMessage. */
    private static final char SECTION = '§';

    /**
     * Converts a raw format string into an Adventure {@link Component} following this precedence:
     * <ol>
     *     <li>If the string contains a MiniMessage tag (a {@code '<'} followed later by a {@code '>'}),
     *     it is parsed as <b>MiniMessage</b>. This is only supported on Velocity, which ships MiniMessage
     *     natively via {@code velocity-api}. On BungeeCord MiniMessage is unavailable (no extra deps), so
     *     there such strings fall back to legacy parsing.</li>
     *     <li>Otherwise the string is parsed as <b>legacy</b> {@code '&'} colour/format codes, with hex
     *     colours supported in both the {@code &#rrggbb} and {@code &x&r&r&g&g&b&b} forms.</li>
     * </ol>
     * Null or empty input yields an empty component.
     *
     * @param text the raw format string (may be null)
     * @return the rendered component, never null
     */
    public static Component format(String text) {
        if (text == null || text.isEmpty())
            return Component.empty();
        if (hasMiniMessageTag(text)) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (RuntimeException ex) {
                // Malformed (or legacy-tainted) MiniMessage — fall back to legacy so we never fail
                // to send a message, mirroring the BungeeCord path.
                BungeeSK.getLogger().warn("Could not render MiniMessage \"{}\": {} — using legacy formatting instead.", text, ex.toString());
            }
        }
        return LEGACY.deserialize(text);
    }

    /**
     * Renders a format string down to a plain (unformatted) String, useful for console/log output.
     * Applies the same precedence as {@link #format(String)} and then strips all styling.
     *
     * @param text the raw format string (may be null)
     * @return the plain text, never null
     */
    public static String formatPlain(String text) {
        if (text == null || text.isEmpty())
            return "";
        return PlainTextComponentSerializer.plainText().serialize(format(text));
    }

    /**
     * Whether this string should be parsed as MiniMessage. It must contain a real MiniMessage tag
     * (see {@link #MINIMESSAGE_TAG}) and must NOT already contain legacy section-sign codes — the
     * MiniMessage parser is strict and throws on legacy {@code §} codes, so those go to legacy.
     */
    private static boolean hasMiniMessageTag(String text) {
        if (text.indexOf(SECTION) >= 0)
            return false;
        return MINIMESSAGE_TAG.matcher(text).find();
    }

    public static Player getPlayer(BungeePlayer bungeePlayer) {
        final Optional<Player> player = BungeeSK.getServer().getPlayer(bungeePlayer.getName());
        return (player.isPresent() && player.get().isActive()) ? player.get() : null;
    }

    public static Player getPlayer(String name) {
        final Optional<Player> player = BungeeSK.getServer().getPlayer(name);
        return (player.isPresent() && player.get().isActive()) ? player.get() : null;
    }

    public static Player getPlayer(UUID uuid) {
        final Optional<Player> player = BungeeSK.getServer().getPlayer(uuid);
        return (player.isPresent() && player.get().isActive()) ? player.get() : null;
    }

    public static BungeePlayer getBungeePlayer(Player player) {
        return new BungeePlayer(player.getUsername(), player.getUniqueId());
    }

    /** Names of the servers currently authenticated through BungeeSK. */
    public static Set<String> getBungeeSkServerNames() {
        return PacketServer.getClientSockets().stream()
                .filter(SocketServer::isAuthenticated)
                .map(VelocityUtils::getServerFromSocket)
                .filter(Objects::nonNull)
                .map(BungeeServer::getName)
                .collect(Collectors.toSet());
    }

    /** Whether the player's current server is connected through BungeeSK. */
    public static boolean isOnBungeeSkServer(Player player) {
        if (player == null)
            return false;
        final Optional<ServerConnection> connection = player.getCurrentServer();
        return connection.isPresent()
                && getBungeeSkServerNames().contains(connection.get().getServerInfo().getName());
    }

    /**
     * Resolves the target of a manipulation effect (send, kick, title, ...), honouring the
     * {@code affect_all_servers} config: when it is false, players on non-BungeeSK servers are
     * treated as unreachable (returns null, so callers no-op).
     */
    public static Player getManipulablePlayer(BungeePlayer bungeePlayer) {
        final Player player = getPlayer(bungeePlayer);
        if (player == null)
            return null;
        if (BungeeConfig.AFFECT_ALL_SERVERS.get())
            return player;
        return isOnBungeeSkServer(player) ? player : null;
    }

    /**
     * Builds a single component from one or more format strings, joining them with newlines.
     * Each string is rendered through {@link #format(String)} (MiniMessage if it contains a tag,
     * otherwise legacy {@code '&'} codes with hex support).
     */
    public static Component getTextComponent(String... text) {
        final TextComponent.Builder builder = Component.text();
        for (int i = 0; i < text.length; i++) {
            builder.append(format(text[i]));
            if (i != text.length - 1)
                builder.appendNewline();
        }
        return builder.build();
    }

    public static BungeeServer getServerFromName(String name) {
        final Optional<RegisteredServer> registeredServer = BungeeSK.getServer().getServer(name);
        return registeredServer.map(server -> new BungeeServer(
                server.getServerInfo().getAddress().getAddress(),
                server.getServerInfo().getAddress().getPort(),
                server.getServerInfo().getName())).orElse(null);
    }

    public static BungeeServer getServerFromInfo(ServerInfo serverInfo) {
        return serverInfo != null ? new BungeeServer(serverInfo.getAddress().getAddress(), serverInfo.getAddress().getPort(), serverInfo.getName()) : null;
    }

    public static BungeeServer getServerFromAddress(String address, int port) {
        try {
            final InetAddress inetAddress = InetAddress.getByName(address);
            final boolean isLocal = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress();
            final RegisteredServer registeredServer = BungeeSK
                    .getServer()
                    .getAllServers()
                    .stream()
                    .filter(server ->
                            (server.getServerInfo().getAddress().getAddress().getHostAddress().equalsIgnoreCase(address) ||
                                    server.getServerInfo().getAddress().getAddress().isAnyLocalAddress() ||
                                    server.getServerInfo().getAddress().getAddress().isLoopbackAddress() && isLocal) &&
                                    server.getServerInfo().getAddress().getPort() == port)
                    .findFirst()
                    .orElse(null);

            final ServerInfo serverInfo = registeredServer != null ? registeredServer.getServerInfo() : null;

            return VelocityUtils.getServerFromInfo(serverInfo);

        } catch (UnknownHostException ignored) {
        }
        return null;
    }

    public static BungeeServer getServerFromSocket(SocketServer socketServer) {
        return getServerFromAddress(socketServer.getSocket().getInetAddress().getHostAddress(), socketServer.getMinecraftPort());
    }

    public static RegisteredServer getRegisteredServer(BungeeServer bungeeServer) {
        final Optional<RegisteredServer> registeredServer = BungeeSK
                .getServer()
                .getAllServers()
                .stream()
                .filter(server ->
                        server.getServerInfo().getAddress().getAddress().getHostAddress().equalsIgnoreCase(bungeeServer.getAddress().getHostAddress()) &&
                                server.getServerInfo().getAddress().getPort() == bungeeServer.getPort())
                .findFirst();

        return registeredServer.orElse(null);
    }

    public static ServerInfo getServerInfo(BungeeServer bungeeServer) {
        final RegisteredServer registeredServer = getRegisteredServer(bungeeServer);
        return registeredServer != null ? registeredServer.getServerInfo() : null;
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