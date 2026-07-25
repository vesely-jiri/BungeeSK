package fr.zorg.bungeesk.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pair carries two-value packet payloads (and is on the deserialization whitelist), so its factory,
 * accessors, fluent setters and value equality must behave.
 */
class PairTest {

    @Test
    void fromExposesBothValues() {
        final Pair<String, Integer> pair = Pair.from("port", 25565);
        assertEquals("port", pair.getFirstValue());
        assertEquals(25565, pair.getSecondValue().intValue());
    }

    @Test
    void settersMutateAndChain() {
        final Pair<String, String> pair = new Pair<>();
        final Pair<String, String> returned = pair.setFirstValue("a").setSecondValue("b");
        assertSame(pair, returned, "fluent setters return the same instance");
        assertEquals("a", pair.getFirstValue());
        assertEquals("b", pair.getSecondValue());
    }

    @Test
    void valueEquality() {
        assertEquals(Pair.from("a", "b"), Pair.from("a", "b"));
        assertNotEquals(Pair.from("a", "b"), Pair.from("a", "c"));
        assertNotEquals(Pair.from("a", "b"), null);
        assertNotEquals(Pair.from("a", "b"), "not a pair");
    }
}
