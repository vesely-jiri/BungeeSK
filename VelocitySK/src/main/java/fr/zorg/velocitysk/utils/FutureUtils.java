package fr.zorg.velocitysk.utils;

import fr.zorg.bungeesk.common.entities.EmptyFutureResponse;
import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.CompletableFuturePacket;
import fr.zorg.bungeesk.common.packets.CompletableFutureResponsePacket;
import fr.zorg.velocitysk.BungeeSK;
import fr.zorg.velocitysk.packets.PacketServer;
import fr.zorg.velocitysk.packets.SocketServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureUtils {

    public static void completeFuture(SocketServer socketServer, UUID uuid, BungeeSKPacket input) {
        BungeeSK.getApi().getListeners().forEach(listener -> {
            try {
                listener.getClass().getMethod("onFutureRequest", UUID.class, SocketServer.class, BungeeSKPacket.class);
                final Object response = listener.onFutureRequest(uuid, socketServer, input);
                if (response != null) {
                    socketServer.sendPacket(
                            new CompletableFutureResponsePacket(
                                    uuid,
                                    response instanceof EmptyFutureResponse ? null : response
                            ));
                }
            } catch (NoSuchMethodException ignored) {
            }
        });
    }

    // Concurrent: generateFuture() runs on a scheduler thread while completeFuture() runs on the
    // socket-reader thread. A plain HashMap could corrupt or lose a completion under that access.
    private static final Map<UUID, CompletableFuture<Object>> futures = new ConcurrentHashMap<>();

    public static Object generateFuture(SocketServer server, BungeeSKPacket packet) {

        // Fast-fail on THIS socket too (PacketServer.isConnected() is only the proxy's own listener):
        // no point allocating a future and blocking the full timeout on a client that's already gone.
        if (!PacketServer.isConnected() || !server.isConnected())
            return null;

        final UUID randomUUID = UUID.randomUUID(); // Using a random UUID here to prevent from mixing between 2 actions at the same time
        final CompletableFuture<Object> future = new CompletableFuture<>();
        futures.put(randomUUID, future);

        final CompletableFuturePacket completableFuturePacket = new CompletableFuturePacket(packet, randomUUID);
        server.sendPacket(completableFuturePacket);

        Object response = null;
        try {
            response = future.get(1, TimeUnit.SECONDS);
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


}