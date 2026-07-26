package fr.zorg.bungeesk.common.net;

import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflinePacketQueueTest {

    /** Minimal identifiable packet so we can assert on order/contents. */
    private static final class TestPacket implements BungeeSKPacket {
        final int id;

        TestPacket(int id) {
            this.id = id;
        }
    }

    private static List<Integer> ids(List<BungeeSKPacket> packets) {
        return packets.stream().map(p -> ((TestPacket) p).id).collect(Collectors.toList());
    }

    @Test
    void drainsFreshInFifoOrder() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(10, 10_000L);
        assertFalse(queue.offer(new TestPacket(1), 0L));
        assertFalse(queue.offer(new TestPacket(2), 100L));
        assertFalse(queue.offer(new TestPacket(3), 200L));

        final OfflinePacketQueue.Drain drain = queue.drainFresh(300L);
        assertEquals(List.of(1, 2, 3), ids(drain.fresh));
        assertEquals(0, drain.staleDropped);
        assertEquals(0, queue.size(), "queue must be empty after draining");
    }

    @Test
    void dropsEntriesOlderThanTtl() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(10, 5_000L);
        queue.offer(new TestPacket(1), 0L);      // will be 6s old at drain -> stale
        queue.offer(new TestPacket(2), 4_000L);  // 2s old at drain -> fresh

        final OfflinePacketQueue.Drain drain = queue.drainFresh(6_000L);
        assertEquals(List.of(2), ids(drain.fresh));
        assertEquals(1, drain.staleDropped);
        assertEquals(1, queue.getDroppedStale());
    }

    @Test
    void evictsOldestWhenFull() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(3, 10_000L);
        assertFalse(queue.offer(new TestPacket(1), 0L));
        assertFalse(queue.offer(new TestPacket(2), 0L));
        assertFalse(queue.offer(new TestPacket(3), 0L));
        assertTrue(queue.offer(new TestPacket(4), 0L), "offer must report eviction when full");

        assertEquals(3, queue.size());
        assertEquals(1, queue.getDroppedOverflow());

        final OfflinePacketQueue.Drain drain = queue.drainFresh(0L);
        assertEquals(List.of(2, 3, 4), ids(drain.fresh), "oldest (1) must have been evicted");
    }

    @Test
    void ttlZeroKeepsEverything() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(10, 0L);
        queue.offer(new TestPacket(1), 0L);
        queue.offer(new TestPacket(2), 1_000_000L);

        final OfflinePacketQueue.Drain drain = queue.drainFresh(9_999_999L);
        assertEquals(List.of(1, 2), ids(drain.fresh));
        assertEquals(0, drain.staleDropped);
    }

    @Test
    void drainOnEmptyReturnsEmpty() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(10, 10_000L);
        final OfflinePacketQueue.Drain drain = queue.drainFresh(0L);
        assertTrue(drain.fresh.isEmpty());
        assertEquals(0, drain.staleDropped);
    }

    @Test
    void clearEmptiesQueue() {
        final OfflinePacketQueue queue = new OfflinePacketQueue(10, 10_000L);
        queue.offer(new TestPacket(1), 0L);
        queue.offer(new TestPacket(2), 0L);
        queue.clear();
        assertEquals(0, queue.size());
        assertTrue(queue.drainFresh(0L).fresh.isEmpty());
    }
}
