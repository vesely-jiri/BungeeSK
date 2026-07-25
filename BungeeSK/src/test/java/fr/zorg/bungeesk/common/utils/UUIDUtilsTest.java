package fr.zorg.bungeesk.common.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Player UUIDs are sent over the socket as a fixed 16-byte encoding; verify it round-trips exactly,
 * including the sign bits of both longs (a common off-by-one source in hand-rolled UUID codecs).
 */
class UUIDUtilsTest {

    @Test
    void roundTripsAUUID() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        final byte[] bytes = UUIDUtils.UUIDToBytes(uuid);
        assertEquals(16, bytes.length, "a UUID encodes to exactly 16 bytes");
        assertEquals(uuid, UUIDUtils.bytesToUUID(bytes), "decoding the bytes must recover the same UUID");
    }

    @Test
    void roundTripsBoundaryValues() {
        final UUID uuid = new UUID(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(uuid, UUIDUtils.bytesToUUID(UUIDUtils.UUIDToBytes(uuid)),
                "the sign bits of the most/least significant longs must be preserved");
    }
}
