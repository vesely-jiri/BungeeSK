package fr.zorg.bungeesk.bukkit.packets;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.utils.ClientBuilder;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Owns the (single) connection from this game server to the proxy and, when enabled, keeps it alive
 * with an exponential-backoff auto-reconnect. All mutating operations are synchronized on the class
 * monitor so the read thread, the reconnect scheduler and Skript effects can't race on the state.
 */
public class PacketClient {

    public enum State {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    private static ClientBuilder builder;
    private static Socket socket;
    private static SocketClient client;

    private static volatile State state = State.DISCONNECTED;

    // Reconnect configuration + runtime state.
    private static boolean autoReconnect = false;
    private static boolean reconnectEnabled = true;
    private static long initialDelaySeconds = 5L;
    private static long maxDelaySeconds = 60L;
    private static int attempt = 0;
    private static BukkitTask reconnectTask;

    /**
     * Applies reconnect settings (usually from config.yml). Call once on enable; safe to call again.
     */
    public static synchronized void configureReconnect(boolean enabled, long initialDelaySeconds, long maxDelaySeconds) {
        PacketClient.reconnectEnabled = enabled;
        PacketClient.initialDelaySeconds = Math.max(1L, initialDelaySeconds);
        PacketClient.maxDelaySeconds = Math.max(PacketClient.initialDelaySeconds, maxDelaySeconds);
    }

    /**
     * Opens (or replaces) the connection described by {@code builder} and enables auto-reconnect.
     */
    public static synchronized void start(ClientBuilder builder) {
        if (builder == null)
            return;
        cancelReconnectTask();
        // Tear down any previous connection without triggering a reconnect for it.
        final SocketClient previous = client;
        client = null;
        socket = null;
        if (previous != null)
            previous.disconnect();

        PacketClient.builder = builder;
        autoReconnect = true;
        attempt = 0;
        connect();
    }

    private static synchronized void connect() {
        if (isConnected())
            return;
        if (state != State.RECONNECTING)
            state = State.CONNECTING;
        final ClientBuilder current = builder;
        if (current == null) {
            state = State.DISCONNECTED;
            return;
        }
        BungeeSK.runAsync(() -> {
            Socket newSocket = null;
            try {
                newSocket = new Socket();
                newSocket.connect(new InetSocketAddress(current.getAddress(), current.getPort()), CONNECT_TIMEOUT_MS);
            } catch (IOException ex) {
                BungeeSK.getInstance().getLogger().warning("BungeeSK could not reach the proxy at "
                        + current.getAddress() + ":" + current.getPort()
                        + " (is the proxy online and the port open?).");
                handleDrop();
                return;
            }
            synchronized (PacketClient.class) {
                if (!autoReconnect) {
                    // A stop()/start() happened while we were connecting; drop this socket.
                    closeQuietly(newSocket);
                    return;
                }
                socket = newSocket;
                client = new SocketClient(newSocket);
            }
        });
    }

    /**
     * Called by {@link fr.zorg.bungeesk.bukkit.packets.listeners.AuthCompleteListener} once the
     * handshake fully succeeds. Marks the link as live and clears the backoff.
     */
    public static synchronized void onConnected() {
        state = State.CONNECTED;
        attempt = 0;
        cancelReconnectTask();
    }

    /**
     * Called by the active {@link SocketClient} when its socket closes. Ignores notifications from a
     * stale client (e.g. one replaced by a newer {@link #start(ClientBuilder)}).
     */
    public static synchronized void notifyDisconnected(SocketClient who) {
        if (who != client)
            return;
        handleDrop();
    }

    private static synchronized void handleDrop() {
        socket = null;
        client = null;
        if (autoReconnect && reconnectEnabled)
            scheduleReconnect();
        else
            state = State.DISCONNECTED;
    }

    private static synchronized void scheduleReconnect() {
        if (reconnectTask != null)
            return; // already pending
        state = State.RECONNECTING;
        final long factor = (long) Math.pow(2, Math.min(attempt, 16));
        final long delay = Math.min(maxDelaySeconds, initialDelaySeconds * factor);
        attempt++;
        BungeeSK.getInstance().getLogger().info("BungeeSK will try to reconnect to the proxy in "
                + delay + "s (attempt " + attempt + ").");
        try {
            reconnectTask = BungeeSK.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(
                    BungeeSK.getInstance(),
                    () -> {
                        synchronized (PacketClient.class) {
                            reconnectTask = null;
                            if (autoReconnect && !isConnected())
                                connect();
                        }
                    },
                    delay * 20L);
        } catch (IllegalStateException ex) {
            // Scheduler unavailable (e.g. plugin disabling) – give up quietly.
            state = State.DISCONNECTED;
        }
    }

    private static synchronized void cancelReconnectTask() {
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
    }

    /**
     * Disconnects and disables auto-reconnect. This is what user-facing "disconnect" / shutdown use.
     */
    public static synchronized void stop() {
        autoReconnect = false;
        cancelReconnectTask();
        state = State.DISCONNECTED;
        final SocketClient current = client;
        client = null;
        socket = null;
        if (current != null)
            current.disconnect();
    }

    /**
     * Forces an immediate (re)connection using the last-known connection settings, re-enabling
     * auto-reconnect. Does nothing if no connection was ever configured.
     */
    public static synchronized void reconnectNow() {
        if (builder != null)
            start(builder);
    }

    public static boolean isConnected() {
        final SocketClient current = client;
        return socket != null && current != null && current.isConnected();
    }

    public static State getState() {
        // Socket is up but the handshake hasn't completed yet.
        if (state == State.CONNECTED && !isConnected())
            return State.DISCONNECTED;
        if (isConnected() && state != State.CONNECTED)
            return State.CONNECTING;
        return state;
    }

    public static SocketClient getClient() {
        return client;
    }

    public static ClientBuilder getBuilder() {
        return builder;
    }

    public static void sendPacket(BungeeSKPacket packet) {
        final SocketClient current = client;
        if (current != null && current.isConnected())
            current.sendPacket(packet);
    }

    private static void closeQuietly(Socket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }
}
