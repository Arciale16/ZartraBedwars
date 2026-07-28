package io.zartra.bedwars.redis.security;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;


import io.zartra.bedwars.redis.api.OperationId;

import io.zartra.bedwars.redis.api.SchemaVersion;

import java.time.Clock;

import java.time.Instant;

import java.time.ZoneOffset;

import java.util.Arrays;

import java.util.HashMap;

import java.util.Map;

import java.util.UUID;

import org.junit.jupiter.api.Test;


class RedisSecurityTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Test void hmacAuthenticatesAndNonceReplayFails() {
        RedisKeyRing ring = ring();
 RedisEnvelopeAuthenticator authenticator = new RedisEnvelopeAuthenticator(ring, Clock.fixed(NOW, ZoneOffset.UTC), new RedisRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), 100, 200));

        AuthenticatedRedisEnvelope unsigned = envelope(NOW, NOW.plusSeconds(5), "key-new", new byte[] {1, 2});
 AuthenticatedRedisEnvelope signed = authenticator.sign(unsigned);

        authenticator.verify(signed, "install", "prod", "backend", "peer-1");

        assertThrows(SecurityException.class, () -> authenticator.verify(signed, "install", "prod", "backend", "peer-1"));

        assertEquals("key-new", signed.keyId());
 assertArrayEquals(new byte[] {1, 2}, signed.payload());

    }
    @Test void rotationOldKeyVerifiesButNewKeySigns() {
        RedisKeyRing ring = ring();
 assertEquals("key-new", ring.signingKeyId());
 assertArrayEquals(bytes(2), ring.verificationKey("key-old"));

        byte[] copy = ring.verificationKey("key-old");
 copy[0] = 99;
 assertArrayEquals(bytes(2), ring.verificationKey("key-old"));

        assertThrows(IllegalArgumentException.class, () -> new RedisKeyRing("missing", CollectionsMap.one("key", bytes(1))));

        assertThrows(IllegalArgumentException.class, () -> new RedisKeyRing("key", CollectionsMap.one("key", new byte[31])));

        assertThrows(IllegalArgumentException.class, () -> ring.verificationKey("bad key"));

    }
    @Test void authenticationRejectsTamperingDestinationExpirySizeAndUnknownKey() {
        RedisEnvelopeAuthenticator authenticator = new RedisEnvelopeAuthenticator(ring(), Clock.fixed(NOW, ZoneOffset.UTC), new RedisRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), 100, 200));

        AuthenticatedRedisEnvelope signed = authenticator.sign(envelope(NOW, NOW.plusSeconds(2), "key-new", new byte[] {1}));

        assertThrows(SecurityException.class, () -> authenticator.verify(signed, "other", "prod", "backend", "p"));

        AuthenticatedRedisEnvelope expired = authenticator.sign(envelope(NOW.minusSeconds(40), NOW.minusSeconds(1), "key-new", new byte[] {1}));

        assertThrows(SecurityException.class, () -> authenticator.verify(expired, "install", "prod", "backend", "p"));

        AuthenticatedRedisEnvelope tampered = signed.signed(new byte[32]);

        assertThrows(SecurityException.class, () -> authenticator.verify(tampered, "install", "prod", "backend", "p"));

        assertThrows(IllegalArgumentException.class, () -> envelope(NOW, NOW.plusSeconds(1), "key-new", new byte[AuthenticatedRedisEnvelope.MAX_PAYLOAD_BYTES + 1]));

    }
    @Test void rateLimiterEnforcesBurst() {
        RedisRateLimiter limiter = new RedisRateLimiter(Clock.fixed(NOW, ZoneOffset.UTC), 1, 2);

        limiter.allow("peer");
 limiter.allow("peer");

        org.junit.jupiter.api.Assertions.assertFalse(limiter.allow("peer"));

        assertThrows(IllegalArgumentException.class, () -> new RedisRateLimiter(Clock.systemUTC(), 2, 1));

    }
    private RedisKeyRing ring() { Map<String, byte[]> keys = new HashMap<String, byte[]>();
 keys.put("key-new", bytes(1));
 keys.put("key-old", bytes(2));
 return new RedisKeyRing("key-new", keys);
 }
    private static byte[] bytes(final int value) { byte[] result = new byte[32];
 Arrays.fill(result, (byte) value);
 return result;
 }
    private AuthenticatedRedisEnvelope envelope(final Instant issued, final Instant deadline, final String keyId, final byte[] payload) {
        return AuthenticatedRedisEnvelope.builder().messageId(UUID.randomUUID()).operationId(OperationId.random()).installation("install").environment("prod").audience("backend").schema(SchemaVersion.of(1, 0)).issuedAt(issued).deadline(deadline).nonce(new byte[16]).keyId(keyId).payload(payload).build();

    }
    private static final class CollectionsMap { static Map<String, byte[]> one(final String key, final byte[] value) { Map<String, byte[]> map = new HashMap<String, byte[]>();
 map.put(key, value);
 return map;
 } }
}
