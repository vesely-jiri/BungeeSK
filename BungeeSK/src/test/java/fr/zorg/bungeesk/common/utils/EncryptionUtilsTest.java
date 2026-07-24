package fr.zorg.bungeesk.common.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the AES packet encryption used on the socket: a correct password round-trips, and a wrong
 * password fails closed (returns null) instead of leaking plaintext.
 */
class EncryptionUtilsTest {

    private static final byte[] PLAINTEXT = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

    @Test
    void roundTripsWithCorrectPassword() {
        final char[] password = "correct horse battery staple".toCharArray();

        final byte[] encrypted = EncryptionUtils.encryptPacket(PLAINTEXT, password);
        assertNotNull(encrypted, "encryption should succeed");
        assertFalse(java.util.Arrays.equals(PLAINTEXT, encrypted), "ciphertext must differ from plaintext");

        final byte[] decrypted = EncryptionUtils.decryptPacket(encrypted, password);
        assertArrayEquals(PLAINTEXT, decrypted, "correct password should recover the plaintext");
    }

    @Test
    void failsClosedWithWrongPassword() {
        final byte[] encrypted = EncryptionUtils.encryptPacket(PLAINTEXT, "right-password".toCharArray());
        assertNotNull(encrypted);

        final byte[] decrypted = EncryptionUtils.decryptPacket(encrypted, "wrong-password".toCharArray());
        assertNull(decrypted, "a wrong password must fail (null), never return data");
    }
}
