package fr.zorg.bungeesk.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Hardening for Java object deserialization. BungeeSK exchanges Java-serialized packets over a raw
 * socket, and the read on the receiving side happens BEFORE the shared-password authentication
 * check. Without a filter, anyone who can reach the port could deliver a malicious serialized
 * "gadget" object graph (a classic deserialization RCE). We restrict deserialization to BungeeSK's
 * own packet/entity classes plus JDK value types, and bound the object graph size/depth. Everything
 * else (i.e. every third-party class, which is where gadget chains live) is rejected.
 */
public final class SafeSerialization {

    private static final long MAX_REFS = 4096L;
    private static final long MAX_BYTES = 20_000_000L;
    private static final long MAX_DEPTH = 32L;

    private static final ObjectInputFilter FILTER = info -> {
        if (info.references() > MAX_REFS || info.streamBytes() > MAX_BYTES || info.depth() > MAX_DEPTH)
            return ObjectInputFilter.Status.REJECTED;
        final Class<?> serialClass = info.serialClass();
        if (serialClass == null)
            return ObjectInputFilter.Status.UNDECIDED;
        Class<?> type = serialClass;
        while (type.isArray())
            type = type.getComponentType();
        if (type.isPrimitive())
            return ObjectInputFilter.Status.ALLOWED;
        final String name = type.getName();
        if (name.startsWith("fr.zorg.bungeesk.") || name.startsWith("java."))
            return ObjectInputFilter.Status.ALLOWED;
        return ObjectInputFilter.Status.REJECTED;
    };

    private SafeSerialization() {
    }

    /**
     * Creates an {@link ObjectInputStream} with the BungeeSK deserialization whitelist installed.
     */
    public static ObjectInputStream createFilteredStream(InputStream in) throws IOException {
        final ObjectInputStream stream = new ObjectInputStream(in);
        stream.setObjectInputFilter(FILTER);
        return stream;
    }
}
