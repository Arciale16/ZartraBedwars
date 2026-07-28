package io.zartra.bedwars.redis.security;


import java.time.Clock;

import java.time.Duration;

import java.util.Arrays;

import java.util.Iterator;

import java.util.LinkedHashMap;

import java.util.Map;

import java.util.Objects;

import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;


/** HMAC-SHA-256 signer/verifier with deadline, skew, nonce replay and rate checks. */
public final class RedisEnvelopeAuthenticator {
    private static final Duration SKEW = Duration.ofSeconds(30);

    private static final Duration NONCE_RETENTION = Duration.ofMinutes(5);

    private final RedisKeyRing keys;
 private final Clock clock;
 private final RedisRateLimiter limiter;
 private final Map<String, Long> nonces = new LinkedHashMap<String, Long>();

    /** Creates an authenticator. */ public RedisEnvelopeAuthenticator(final RedisKeyRing keys, final Clock clock, final RedisRateLimiter limiter) { this.keys = Objects.requireNonNull(keys, "keys");
 this.clock = Objects.requireNonNull(clock, "clock");
 this.limiter = Objects.requireNonNull(limiter, "limiter");
 }
    /** Signs with the active key. */ public AuthenticatedRedisEnvelope sign(final AuthenticatedRedisEnvelope envelope) { return envelope.signed(mac(keys.verificationKey(keys.signingKeyId()), envelope.canonicalBytes()));
 }
    /** Authenticates before payload parsing and records nonce atomically. */
    public synchronized void verify(final AuthenticatedRedisEnvelope envelope, final String expectedInstallation, final String expectedEnvironment, final String expectedAudience, final String peer) {
        Objects.requireNonNull(envelope, "envelope");
 purgeNonces();
 final long now = clock.millis();

        if (!expectedInstallation.equals(envelope.installation()) || !expectedEnvironment.equals(envelope.environment()) || !expectedAudience.equals(envelope.audience())) { throw new SecurityException("wrong envelope destination");
 }
        if (Math.abs(now - envelope.issuedAt().toEpochMilli()) > SKEW.toMillis() || now > envelope.deadline().toEpochMilli()) { throw new SecurityException("expired or skewed envelope");
 }
        if (!limiter.allow(peer)) { throw new SecurityException("rate limit exceeded");
 }
        final byte[] key = keys.verificationKey(envelope.keyId());
 if (key == null || !constantTime(mac(key, envelope.canonicalBytes()), envelope.signature())) { throw new SecurityException("invalid envelope signature");
 }
        final String nonce = java.util.Base64.getEncoder().encodeToString(envelope.nonce());
 if (nonces.containsKey(nonce)) { throw new SecurityException("replayed nonce");
 }
        nonces.put(nonce, now + NONCE_RETENTION.toMillis());

    }
    private void purgeNonces() { final long now = clock.millis();
 final Iterator<Map.Entry<String, Long>> iterator = nonces.entrySet().iterator();
 while (iterator.hasNext()) { if (iterator.next().getValue().longValue() <= now) { iterator.remove();
 } } }
    private static byte[] mac(final byte[] key, final byte[] value) { try { final Mac mac = Mac.getInstance("HmacSHA256");
 mac.init(new SecretKeySpec(key, "HmacSHA256"));
 return mac.doFinal(value);
 } catch (java.security.GeneralSecurityException failure) { throw new IllegalStateException("HMAC unavailable", failure);
 } }
    private static boolean constantTime(final byte[] left, final byte[] right) { return java.security.MessageDigest.isEqual(left, right == null ? new byte[0] : Arrays.copyOf(right, right.length));
 }
}
