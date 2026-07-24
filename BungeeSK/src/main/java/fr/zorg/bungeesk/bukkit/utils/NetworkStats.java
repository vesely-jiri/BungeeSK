package fr.zorg.bungeesk.bukkit.utils;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.packets.GetNetworkPlayerCountPacket;
import org.bukkit.scheduler.BukkitTask;

/**
 * A tiny async-refreshed cache of network-wide stats, so PlaceholderAPI placeholders can read them
 * instantly instead of doing a blocking proxy round-trip on the (possibly main) thread that resolves
 * the placeholder. Refreshed on an async timer; readers just get the last known value.
 */
public final class NetworkStats {

    private static volatile long networkPlayerCount = 0L;
    private static BukkitTask task;

    private NetworkStats() {
    }

    /** Starts the async refresh loop (safe to call again; restarts it). */
    public static void start() {
        stop();
        // Every 10s on an async thread. generateFuture blocks up to 5s, well under the period.
        task = BungeeSK.getInstance().getServer().getScheduler().runTaskTimerAsynchronously(
                BungeeSK.getInstance(), NetworkStats::refresh, 40L, 200L);
    }

    private static void refresh() {
        if (!PacketClient.isConnected())
            return;
        final Object response = CompletableFutureUtils.generateFuture(new GetNetworkPlayerCountPacket());
        if (response instanceof Long)
            networkPlayerCount = (Long) response;
    }

    public static long getNetworkPlayerCount() {
        return networkPlayerCount;
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
