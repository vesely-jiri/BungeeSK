package fr.zorg.bungeesk.common.utils;

import fr.zorg.bungeesk.common.packets.BungeeSKPacket;
import fr.zorg.bungeesk.common.packets.HandshakePacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Round-trips a packet through the wire codec: {@link PacketUtils#packetToBytes} then
 * {@link PacketUtils#packetFromBytes} (which reads through the {@link SafeSerialization} whitelist),
 * and checks that malformed bytes fail safe to {@code null} instead of throwing.
 */
class PacketUtilsTest {

    @Test
    void roundTripsAWhitelistedPacket() {
        final byte[] bytes = PacketUtils.packetToBytes(new HandshakePacket(25565));
        assertNotNull(bytes, "serialization should produce bytes");

        final BungeeSKPacket decoded = PacketUtils.packetFromBytes(bytes);
        assertInstanceOf(HandshakePacket.class, decoded);
        assertEquals(25565, ((HandshakePacket) decoded).getMinecraftPort(), "port must survive the round-trip");
    }

    @Test
    void malformedBytesFailToNull() {
        assertNull(PacketUtils.packetFromBytes(new byte[]{0, 1, 2, 3}),
                "garbage input must not throw, just return null");
    }
}
