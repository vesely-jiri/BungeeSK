package fr.zorg.bungeesk.common.utils;

import com.example.UntrustedPayload;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the deserialization whitelist that closes the pre-authentication RCE vector: only
 * {@code fr.zorg.bungeesk.*} and {@code java.*} (plus primitives) may be deserialized from a socket.
 */
class SafeSerializationTest {

    private static byte[] serialize(Object o) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    private static Object readFiltered(byte[] data) throws Exception {
        return SafeSerialization.createFilteredStream(new ByteArrayInputStream(data)).readObject();
    }

    @Test
    void allowsJdkValueTypes() throws Exception {
        assertEquals("hello", readFiltered(serialize("hello")));
        assertEquals(42, readFiltered(serialize(42)));
    }

    @Test
    void allowsBungeeSkTypes() throws Exception {
        final Pair<String, String> pair = Pair.from("a", "b");
        assertEquals(pair, readFiltered(serialize(pair)));
    }

    @Test
    void rejectsUntrustedThirdPartyClass() throws Exception {
        final byte[] data = serialize(new UntrustedPayload());
        // The filter returns REJECTED, which ObjectInputStream surfaces as an InvalidClassException.
        assertThrows(InvalidClassException.class, () -> readFiltered(data));
    }
}
