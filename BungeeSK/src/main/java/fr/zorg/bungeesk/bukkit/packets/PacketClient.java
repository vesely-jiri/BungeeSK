package fr.zorg.bungeesk.bukkit.packets;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.utils.ClientBuilder;
import fr.zorg.bungeesk.common.net.OfflinePacketQueue;
import fr.zorg.bungeesk.common.packets.AuthCompletePacket;
import fr.zorg.bungeesk.common.packets.AuthResponsePacket;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.CompletableFuturePacket;
import fr.zorg.bungeesk.common.packets.CompletableFutureResponsePacket;
import fr.zorg.bungeesk.common.packets.GlobalScriptsRequestPacket;
import fr.zorg.bungeesk.common.packets.HandshakePacket;
import fr.zorg.bungeesk.common.packets.KeepAlivePacket;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns the (single) connection from this game server to the proxy and, when enabled, keeps it alive
 * with an auto-reconnect. All mutating operations are synchronized on the class monitor so the read
 * thread, the reconnect scheduler and Skript effects can't race on the state.
 *
 * <p>The delay before each attempt is driven by {@code reconnect.delays-seconds}: a list that is
 * stepped through one entry per attempt, the last entry repeating for every later attempt (so a
 * single-element list is a fixed delay). When that list is empty it falls back to exponential
 * backoff between {@code initial-delay-seconds} and {@code max-delay-seconds}.
 */
public class PacketClient {

    public enum State {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    // volatile: written under the class lock but read lock-free on the Skript/effect and reconnect paths
    // (sendPacket/isConnected/flushOfflineQueue), so readers must see the latest published reference.
    private static volatile ClientBuilder builder;
    private static volatile Socket socket;
    private static volatile SocketClient client;

    private static volatile State state = State.DISCONNECTED;

    // Reconnect configuration + runtime state.
    private static boolean autoReconnect = false;
    private static boolean reconnectEnabled = true;
    private static List<Integer> delaysSeconds = Collections.emptyList();
    private static long initialDelaySeconds = 5L;
    private static long maxDelaySeconds = 60L;
    private static boolean logAttempts = false;
    private static int attempt = 0;
    // Whether we've already logged the current outage, so the "can't reach proxy" warning is shown
    // once (not once per attempt) when per-attempt logging is off.
    private static boolean announcedProblem = false;
    private static BukkitTask reconnectTask;

    // While the proxy link is down, fire-and-forget effect packets are buffered here (bounded, with a
    // short TTL) and replayed once we reconnect, so a brief outage doesn't silently swallow a broadcast
    // or a cross-server command. Requests (answered synchronously and already failing fast when
    // offline) and connection-control packets are never queued — see isQueueable().
    private static final int OFFLINE_QUEUE_MAX = 512;
    private static final long OFFLINE_QUEUE_TTL_MS = 10_000L;
    // Delay before replaying the buffer on reconnect: lets the encryption handshake settle on both
    // ends first, so replayed packets are encrypted consistently with what the proxy now expects.
    private static final long OFFLINE_QUEUE_FLUSH_DELAY_TICKS = 20L; // ~1s
    private static final OfflinePacketQueue offlineQueue = new OfflinePacketQueue(OFFLINE_QUEUE_MAX, OFFLINE_QUEUE_TTL_MS);

    /**
     * Applies reconnect settings (usually from config.yml). Call once on enable; safe to call again.
     *
     * @param enabled              whether to reconnect at all
     * @param delaysSeconds        per-attempt delays; last entry repeats. Empty = exponential backoff
     * @param initialDelaySeconds  exponential-backoff first delay (used only when {@code delaysSeconds} is empty)
     * @param maxDelaySeconds      exponential-backoff cap (used only when {@code delaysSeconds} is empty)
     * @param logAttempts          log a line before every attempt (otherwise only lost/reconnected)
     */
    public static synchronized void configureReconnect(boolean enabled, List<Integer> delaysSeconds,
                                                       long initialDelaySeconds, long maxDelaySeconds,
                                                       boolean logAttempts) {
        PacketClient.reconnectEnabled = enabled;
        PacketClient.delaysSeconds = (delaysSeconds == null) ? Collections.emptyList() : new ArrayList<>(delaysSeconds);
        PacketClient.initialDelaySeconds = Math.max(1L, initialDelaySeconds);
        PacketClient.maxDelaySeconds = Math.max(PacketClient.initialDelaySeconds, maxDelaySeconds);
        PacketClient.logAttempts = logAttempts;
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
        announcedProblem = false;
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
            final Socket newSocket;
            try {
                newSocket = new Socket();
                newSocket.connect(new InetSocketAddress(current.getAddress(), current.getPort()), CONNECT_TIMEOUT_MS);
                // OS-level keep-alive so an idle link isn't silently reaped by a NAT/conntrack table.
                try {
                    newSocket.setKeepAlive(true);
                } catch (SocketException ignored) {
                }
            } catch (IOException ex) {
                announceProblem(current.getAddress(), current.getPort(), "is the proxy online and the port open?");
                handleDrop();
                return;
            }

            // Perform the (blocking) handshake stream setup OUTSIDE the class lock. If the proxy
            // resets the connection mid-handshake this throws instead of dumping a raw stack trace,
            // and we schedule the next reconnect attempt (previously this path stalled the loop).
            final SocketClient newClient;
            try {
                newClient = new SocketClient(newSocket);
            } catch (IOException ex) {
                closeQuietly(newSocket);
                announceProblem(current.getAddress(), current.getPort(), "connection reset during the handshake");
                handleDrop();
                return;
            }

            synchronized (PacketClient.class) {
                if (!autoReconnect) {
                    // A stop()/start() happened while we were connecting; drop this socket.
                    newClient.disconnect();
                    return;
                }
                socket = newSocket;
                client = newClient;
                // Start reads only now that this client is published, so a fast read failure's
                // notifyDisconnected(this) matches and correctly schedules a reconnect.
                newClient.startReading();
            }
        });
    }

    /**
     * Called by {@link fr.zorg.bungeesk.bukkit.packets.listeners.AuthCompleteListener} once the
     * handshake fully succeeds. Marks the link as live, clears the backoff and logs the outcome.
     */
    public static synchronized void onConnected() {
        final boolean wasReconnecting = attempt > 0 || announcedProblem;
        state = State.CONNECTED;
        attempt = 0;
        announcedProblem = false;
        cancelReconnectTask();
        if (wasReconnecting)
            BungeeSK.getInstance().getLogger().info("Reconnected to the proxy.");
        else
            BungeeSK.getInstance().getLogger().info("Connected to the proxy.");
        flushOfflineQueueLater();
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
        final boolean wasConnected = state == State.CONNECTED;
        socket = null;
        client = null;
        if (!(autoReconnect && reconnectEnabled)) {
            state = State.DISCONNECTED;
            return;
        }
        // A live connection just dropped (as opposed to a failed reconnect attempt): announce it once.
        if (wasConnected) {
            BungeeSK.getInstance().getLogger().warning("Lost the connection to the proxy — attempting to reconnect.");
            announcedProblem = true;
        }
        scheduleReconnect();
    }

    private static synchronized void scheduleReconnect() {
        if (reconnectTask != null)
            return; // already pending
        state = State.RECONNECTING;
        final long delay = computeDelaySeconds(attempt);
        attempt++;
        if (logAttempts)
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

    /**
     * The delay (seconds) before the given zero-based attempt. Uses the configured delay list
     * (last entry repeating) when set, otherwise exponential backoff capped at {@link #maxDelaySeconds}.
     */
    private static long computeDelaySeconds(int attemptIndex) {
        if (delaysSeconds != null && !delaysSeconds.isEmpty()) {
            final int index = Math.min(attemptIndex, delaysSeconds.size() - 1);
            return Math.max(1L, delaysSeconds.get(index));
        }
        final long factor = (long) Math.pow(2, Math.min(attemptIndex, 16));
        return Math.min(maxDelaySeconds, initialDelaySeconds * factor);
    }

    /**
     * Logs a "can't reach the proxy" warning: every attempt when {@link #logAttempts} is on,
     * otherwise just once per outage so the console isn't spammed.
     */
    private static synchronized void announceProblem(String address, int port, String detail) {
        if (logAttempts) {
            BungeeSK.getInstance().getLogger().warning("BungeeSK could not reach the proxy at "
                    + address + ":" + port + " — " + detail + " (attempt " + attempt + ").");
        } else if (!announcedProblem) {
            announcedProblem = true;
            BungeeSK.getInstance().getLogger().warning("BungeeSK could not reach the proxy at "
                    + address + ":" + port + " — will keep retrying quietly"
                    + " (set reconnect.log-attempts: true in config.yml for per-attempt logs).");
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
        announcedProblem = false;
        // Explicit disconnect: drop anything buffered so it isn't replayed on a later manual connect.
        offlineQueue.clear();
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
        if (current != null && current.isConnected()) {
            current.sendPacket(packet);
            return;
        }
        // Offline: buffer replayable effects so a short outage doesn't silently lose them.
        if (isQueueable(packet)) {
            final boolean evicted = offlineQueue.offer(packet, System.currentTimeMillis());
            if (evicted)
                BungeeSK.getInstance().getLogger().warning(
                        "Proxy link down: offline buffer full (" + OFFLINE_QUEUE_MAX
                                + ") — dropping the oldest buffered packet.");
        }
    }

    /**
     * Whether a packet may be buffered while offline and replayed on reconnect. Excludes
     * connection-control packets (handshake / auth / keep-alive) and request/response packets — a
     * request is answered synchronously and already fails fast when offline, so replaying a stale one
     * makes no sense.
     */
    private static boolean isQueueable(BungeeSKPacket packet) {
        return !(packet instanceof CompletableFuturePacket
                || packet instanceof CompletableFutureResponsePacket
                || packet instanceof KeepAlivePacket
                || packet instanceof HandshakePacket
                || packet instanceof AuthResponsePacket
                || packet instanceof AuthCompletePacket
                || packet instanceof GlobalScriptsRequestPacket);
    }

    /**
     * Schedules a delayed replay of the offline buffer after (re)connecting. The delay lets the
     * encryption handshake finish on both ends first (see {@link #OFFLINE_QUEUE_FLUSH_DELAY_TICKS}).
     */
    private static void flushOfflineQueueLater() {
        if (offlineQueue.size() == 0)
            return;
        try {
            BungeeSK.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(
                    BungeeSK.getInstance(),
                    PacketClient::flushOfflineQueue,
                    OFFLINE_QUEUE_FLUSH_DELAY_TICKS);
        } catch (IllegalStateException ex) {
            // Scheduler unavailable (plugin disabling) — leave the buffer for next time.
        }
    }

    private static void flushOfflineQueue() {
        final SocketClient current = client;
        if (current == null || !current.isConnected())
            return; // dropped again before we could flush; keep the buffer for the next connect
        final OfflinePacketQueue.Drain drain = offlineQueue.drainFresh(System.currentTimeMillis());
        for (BungeeSKPacket packet : drain.fresh)
            current.sendPacket(packet);
        if (!drain.fresh.isEmpty())
            BungeeSK.getInstance().getLogger().info(
                    "Replayed " + drain.fresh.size() + " buffered packet(s) after reconnecting"
                            + (drain.staleDropped > 0
                            ? " (" + drain.staleDropped + " dropped as too old)." : "."));
    }

    private static void closeQuietly(Socket socket) {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) {
        }
    }
}
