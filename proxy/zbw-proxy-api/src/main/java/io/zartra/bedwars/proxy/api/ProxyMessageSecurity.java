package io.zartra.bedwars.proxy.api;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA-256 authentication, deadline, environment, audience and replay protection. */
public final class ProxyMessageSecurity {
    /** SHA-256 HMAC size. */ public static final int SIGNATURE_BYTES = 32;
    /** Maximum retained nonce records. */ public static final int MAX_NONCES = 10000;
    /** Sustained authenticated message rate per peer. */ public static final int RATE_PER_SECOND = 100;
    /** Short authenticated-message burst per peer. */ public static final int BURST = 200;
    private final ProtocolVersion protocol;
    private final String environment;
    private final String audience;
    private final Map<String, byte[]> keys;
    private final Map<String, Instant> nonces = new LinkedHashMap<String, Instant>();
    private double permits = BURST;
    private Instant refillAt;

    /** Creates a verifier with rotation-capable key IDs. */
    public ProxyMessageSecurity(final ProtocolVersion protocol, final String environment,
            final String audience, final Map<String, byte[]> keys) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.environment = ProxyContractValidation.token(environment, "environment");
        this.audience = ProxyContractValidation.token(audience, "audience");
        if (keys == null || keys.isEmpty()) { throw new IllegalArgumentException("keys required"); }
        this.keys = new LinkedHashMap<String, byte[]>();
        for (Map.Entry<String, byte[]> entry : keys.entrySet()) {
            String id = ProxyContractValidation.token(entry.getKey(), "keyId");
            byte[] key = Objects.requireNonNull(entry.getValue(), "key").clone();
            if (key.length < SIGNATURE_BYTES) { throw new IllegalArgumentException("key too short"); }
            this.keys.put(id, key);
        }
    }

    /** Signs an opaque payload. */
    public SignedProxyMessage sign(final String keyId, final String nonce,
            final Instant issuedAt, final Instant deadline, final byte[] payload) {
        byte[] key = key(keyId);
        byte[] signature = mac(key, canonical(protocol, environment, audience, keyId,
                nonce, issuedAt, deadline, payload));
        return SignedProxyMessage.of(protocol, environment, audience, keyId, nonce,
                issuedAt, deadline, payload, signature);
    }

    /** Authenticates metadata and bytes before returning payload for deserialization. */
    public synchronized byte[] authenticate(final SignedProxyMessage message, final Instant now) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(now, "now");
        if (!protocol.compatibleWith(message.protocol()) || !environment.equals(message.environment())
                || !audience.equals(message.audience()) || !now.isBefore(message.deadline())) {
            throw new SecurityException("message context rejected");
        }
        rate(now);
        cleanup(now);
        if (nonces.containsKey(message.nonce())) { throw new SecurityException("message replay rejected"); }
        byte[] expected = mac(key(message.keyId()), canonical(message.protocol(),
                message.environment(), message.audience(), message.keyId(), message.nonce(),
                message.issuedAt(), message.deadline(), message.payload()));
        if (!MessageDigest.isEqual(expected, message.signature())) {
            throw new SecurityException("message authentication failed");
        }
        if (nonces.size() >= MAX_NONCES) { throw new SecurityException("nonce capacity exceeded"); }
        nonces.put(message.nonce(), message.deadline());
        return message.payload();
    }

    private void rate(final Instant now) {
        if (refillAt == null) {
            refillAt = now;
        } else if (now.isAfter(refillAt)) {
            long elapsed = now.toEpochMilli() - refillAt.toEpochMilli();
            permits = Math.min(BURST, permits + elapsed * RATE_PER_SECOND / 1000.0);
            refillAt = now;
        }
        if (permits < 1.0) {
            throw new SecurityException("message rate exceeded");
        }
        permits -= 1.0;
    }

    private byte[] key(final String keyId) {
        byte[] value = keys.get(ProxyContractValidation.token(keyId, "keyId"));
        if (value == null) { throw new SecurityException("unknown key id"); }
        return value;
    }
    private void cleanup(final Instant now) {
        for (Map.Entry<String, Instant> entry : new LinkedHashMap<String, Instant>(nonces).entrySet()) {
            if (!now.isBefore(entry.getValue())) { nonces.remove(entry.getKey()); }
        }
    }
    private static byte[] canonical(final ProtocolVersion protocol, final String environment,
            final String audience, final String keyId, final String nonce,
            final Instant issuedAt, final Instant deadline, final byte[] payload) {
        String header = protocol + "\n" + environment + "\n" + audience + "\n" + keyId
                + "\n" + nonce + "\n" + issuedAt.toEpochMilli() + "\n"
                + deadline.toEpochMilli() + "\n" + payload.length + "\n";
        byte[] prefix = header.getBytes(StandardCharsets.UTF_8);
        byte[] value = new byte[prefix.length + payload.length];
        System.arraycopy(prefix, 0, value, 0, prefix.length);
        System.arraycopy(payload, 0, value, prefix.length, payload.length);
        return value;
    }
    private static byte[] mac(final byte[] key, final byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value);
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("HMAC-SHA-256 unavailable", failure);
        }
    }
}
