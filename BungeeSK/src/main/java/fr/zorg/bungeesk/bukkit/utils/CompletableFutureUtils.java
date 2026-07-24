package fr.zorg.bungeesk.bukkit.utils;

import fr.zorg.bungeesk.bukkit.BungeeSK;
import fr.zorg.bungeesk.bukkit.packets.PacketClient;
import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.CompletableFuturePacket;
import fr.zorg.bungeesk.common.packets.CompletableFutureResponsePacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletableFutureUtils {

    /**
     * Timeout for a synchronous request/response round-trip to the proxy. Kept generous because the
     * proxy may be remote (WAN latency + its SQLite lookup + encryption on both ends).
     */
    private static final long RESPONSE_TIMEOUT_SECONDS = 5L;

    // Concurrent: generateFuture() runs on the Skript thread while completeFuture() runs on the async
    // socket-reader thread. A plain HashMap could lose a completion under concurrent access.
    private static final Map<UUID, CompletableFuture<Object>> futures = new ConcurrentHashMap<>();

    public static Object generateFuture(BungeeSKPacket packet) {

        if (!PacketClient.isConnected())
            return null;

        final UUID randomUUID = UUID.randomUUID(); // Using a random UUID here to prevent from mixing between 2 actions at the same time
        final CompletableFuture<Object> future = new CompletableFuture<>();
        futures.put(randomUUID, future);

        final CompletableFuturePacket completableFuturePacket = new CompletableFuturePacket(packet, randomUUID);
        PacketClient.sendPacket(completableFuturePacket);

        Object response = null;
        try {
            response = future.get(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
        } finally {
            futures.remove(randomUUID); // never leak the entry, even on timeout
        }

        return response;
    }

    public static void completeFuture(UUID uuid, Object response) {
        final CompletableFuture<Object> future = futures.remove(uuid);
        if (future != null)
            future.complete(response);
    }

    public static void initFuture(UUID uuid, BungeeSKPacket input) {
        BungeeSK.getApi().getListeners().forEach(listener -> {
            try {
                listener.getClass().getMethod("onFutureRequest", UUID.class, BungeeSKPacket.class);
                final Object response = listener.onFutureRequest(uuid, input);
                if (response != null) {
                    PacketClient.sendPacket(
                            new CompletableFutureResponsePacket(
                                    uuid,
                                    response instanceof EmptyFutureResponse ? null : response
                            ));
                }
            } catch (NoSuchMethodException ignored) {
            }
        });
    }

}