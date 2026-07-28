package io.zartra.bedwars.redis.security;


import java.util.Arrays;

import java.util.Collections;

import java.util.HashMap;

import java.util.Map;

import java.util.Objects;


/** Immutable HMAC key ring with one signing key and multiple verification slots. */
public final class RedisKeyRing {
    private final String signingKeyId;

    private final Map<String, byte[]> keys;

    /** Creates a defensive key ring;
 every HMAC key must be exactly 256 bits. */
    public RedisKeyRing(final String signingKeyId, final Map<String, byte[]> keys) {
        this.signingKeyId = requireId(signingKeyId);

        if (keys == null || keys.isEmpty() || !keys.containsKey(signingKeyId)) { throw new IllegalArgumentException("signing key missing");
 }
        final Map<String, byte[]> copy = new HashMap<String, byte[]>();

        for (Map.Entry<String, byte[]> entry : keys.entrySet()) {
            final byte[] material = Objects.requireNonNull(entry.getValue(), "key material");

            if (material.length != 32) { throw new IllegalArgumentException("HMAC key must be 256 bits");
 }
            copy.put(requireId(entry.getKey()), Arrays.copyOf(material, material.length));

        }
        this.keys = Collections.unmodifiableMap(copy);

    }
    /** Returns current signing key ID. */ public String signingKeyId() { return signingKeyId;
 }
    /** Returns a defensive key copy, or null when revoked/unknown. */
    public byte[] verificationKey(final String keyId) {
        final byte[] material = keys.get(requireId(keyId));

        return material == null ? null : Arrays.copyOf(material, material.length);

    }
    private static String requireId(final String value) {
        if (value == null || !value.matches("[A-Za-z0-9._-]{1,64}")) { throw new IllegalArgumentException("invalid key ID");
 }
        return value;

    }
}
