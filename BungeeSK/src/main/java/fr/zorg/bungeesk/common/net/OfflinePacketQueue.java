package fr.zorg.bungeesk.common.net;

import fr.zorg.bungeesk.common.packets.BungeeSKPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A small bounded, time-to-live buffer for fire-and-forget packets produced while the connection to
 * the proxy is down. When the link comes back the queue is drained in FIFO order and anything older
 * than the TTL is discarded — so a "send player to server" that has been sitting for half a minute
 * does not fire long after it stopped making sense.
 *
 * <p>The buffer is bounded so a permanently-offline proxy cannot grow it without limit: once full,
 * the oldest entry is evicted to make room for the newest. All methods are thread-safe — entries are
 * enqueued from the async send threads and drained from the reconnect path.
 */
public final class OfflinePacketQueue {

    /** One buffered packet together with the wall-clock time it was queued (ms). */
    private static final class Entry {
        final BungeeSKPacket packet;
        final long queuedAtMs;

        Entry(BungeeSKPacket packet, long queuedAtMs) {
            this.packet = packet;
            this.queuedAtMs = queuedAtMs;
        }
    }

    /** Result of a drain: the packets to replay (fresh, in FIFO order) and how many were too old. */
    public static final class Drain {
        public final List<BungeeSKPacket> fresh;
        public final int staleDropped;

        Drain(List<BungeeSKPacket> fresh, int staleDropped) {
            this.fresh = fresh;
            this.staleDropped = staleDropped;
        }
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final int maxSize;
    private final long ttlMs;
    private long droppedOverflow = 0L;
    private long droppedStale = 0L;

    /**
     * @param maxSize maximum buffered packets before the oldest is evicted (floored at 1)
     * @param ttlMs   how long (ms) a packet may sit before it is dropped on drain; 0 disables the TTL
     */
    public OfflinePacketQueue(int maxSize, long ttlMs) {
        this.maxSize = Math.max(1, maxSize);
        this.ttlMs = Math.max(0L, ttlMs);
    }

    /**
     * Buffers a packet. If the queue is already at capacity the oldest entry is evicted first (and
     * counted), so the newest effects win a saturated buffer.
     *
     * @return true if an older entry had to be evicted to make room
     */
    public synchronized boolean offer(BungeeSKPacket packet, long nowMs) {
        boolean evicted = false;
        while (entries.size() >= maxSize) {
            entries.pollFirst();
            droppedOverflow++;
            evicted = true;
        }
        entries.addLast(new Entry(packet, nowMs));
        return evicted;
    }

    /**
     * Removes every buffered packet, returning those still within the TTL in FIFO order and
     * discarding (counting) the stale ones. The queue is empty afterwards.
     */
    public synchronized Drain drainFresh(long nowMs) {
        final List<BungeeSKPacket> fresh = new ArrayList<>(entries.size());
        int stale = 0;
        Entry e;
        while ((e = entries.pollFirst()) != null) {
            if (ttlMs == 0L || nowMs - e.queuedAtMs <= ttlMs) {
                fresh.add(e.packet);
            } else {
                stale++;
                droppedStale++;
            }
        }
        return new Drain(fresh, stale);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized long getDroppedOverflow() {
        return droppedOverflow;
    }

    public synchronized long getDroppedStale() {
        return droppedStale;
    }

    public synchronized void clear() {
        entries.clear();
    }
}
