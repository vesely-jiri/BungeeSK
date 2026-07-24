package com.example;

import java.io.Serializable;

/**
 * A serializable class in a package that is NOT on the BungeeSK deserialization whitelist
 * (i.e. neither {@code fr.zorg.bungeesk.*} nor {@code java.*}). Stands in for a third-party
 * "gadget" class in {@code SafeSerializationTest} — the filter must reject it.
 */
public class UntrustedPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    public String data = "untrusted";

}
