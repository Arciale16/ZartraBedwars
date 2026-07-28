package io.zartra.bedwars.redis;



import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertFalse;



import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertTrue;


import io.zartra.bedwars.api.event.EventMetadata;

import io.zartra.bedwars.api.identity.CorrelationId;

import io.zartra.bedwars.api.identity.EventId;

import io.zartra.bedwars.api.identity.EventTypeId;

import io.zartra.bedwars.api.identity.IdempotencyKey;

import io.zartra.bedwars.redis.api.DeduplicationKey;

import io.zartra.bedwars.redis.api.OperationId;

import io.zartra.bedwars.redis.api.RedisKey;

import io.zartra.bedwars.redis.api.RedisNamespace;

import io.zartra.bedwars.redis.api.SchemaVersion;

import io.zartra.bedwars.redis.api.StreamCursor;

import io.zartra.bedwars.redis.api.StreamId;

import io.zartra.bedwars.redis.api.StreamRecord;

import io.zartra.bedwars.storage.api.MessageEnvelope;

import java.net.URI;

import java.time.Clock;

import java.time.Duration;

import java.time.Instant;

import java.time.ZoneOffset;

import java.util.Arrays;

import java.util.Collections;

import java.util.Random;

import java.util.UUID;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;


class RedisAdapterTest {
    private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final RedisNamespace namespace = RedisNamespace.of("install", "test", "coord", SchemaVersion.of(1, 0));


    @Test void configurationEnforcesConnectionQueueAndSchemeBounds() throws Exception {
        RedisAdapterConfig config = RedisAdapterConfig.of(new URI("rediss://localhost:6379"), namespace, 16, 5000, Duration.ofMillis(500));

        assertEquals(16, config.connections());
 assertEquals(5000, config.queueCapacity());
 assertEquals(namespace, config.namespace());

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("http://localhost"), namespace, 1, 1, Duration.ofMillis(1)));

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("redis://localhost"), namespace, 17, 1, Duration.ofMillis(1)));

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("redis://localhost"), namespace, 0, 1, Duration.ofMillis(1)));

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("redis://localhost"), namespace, 1, 0, Duration.ofMillis(1)));

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("redis://localhost"), namespace, 1, 5001, Duration.ofMillis(1)));

        assertThrows(IllegalArgumentException.class, () -> RedisAdapterConfig.of(new URI("redis://localhost"), namespace, 1, 1, Duration.ZERO));

    }

    @Test void schemaGuardFailsClosedForForeignAndUnknownSchemas() {
        RedisSchemaGuard guard = new RedisSchemaGuard(namespace, Collections.singleton(SchemaVersion.of(1, 0)));

        RedisKey key = RedisKey.of(namespace, "stream", "events");
 guard.requireAccepted(key, SchemaVersion.of(1, 0));

        assertThrows(IllegalArgumentException.class, () -> guard.requireAccepted(key, SchemaVersion.of(2, 0)));

        RedisNamespace foreign = RedisNamespace.of("other", "test", "coord", SchemaVersion.of(1, 0));

        assertThrows(SecurityException.class, () -> guard.requireAccepted(RedisKey.of(foreign, "stream", "events"), SchemaVersion.of(1, 0)));

        assertThrows(IllegalArgumentException.class, () -> new RedisSchemaGuard(namespace, Collections.<SchemaVersion>emptySet()));

    }

    @Test void dedupeIsBoundedDuplicateSafeAndExpiresAfterTwentyFourHours() {
        MutableClock clock = new MutableClock(NOW);
 RedisDeduplicationStore store = new RedisDeduplicationStore(clock, 2);

        DeduplicationKey first = DeduplicationKey.of(namespace, OperationId.random());

        assertTrue(store.record(first));
 assertFalse(store.record(first));
 assertEquals(1, store.size());

        store.record(DeduplicationKey.of(namespace, OperationId.random()));
 store.record(DeduplicationKey.of(namespace, OperationId.random()));
 assertEquals(2, store.size());

        clock.advance(Duration.ofHours(24));
 assertEquals(0, store.size());

        assertThrows(IllegalArgumentException.class, () -> new RedisDeduplicationStore(clock, 250001));

    }

    @Test void reconnectAndCircuitBreakerHaveFiniteBudgets() {
        RedisReconnectPolicy policy = new RedisReconnectPolicy(new Random(1L));

        assertTrue(policy.delay(1).toMillis() >= 50L);
 assertTrue(policy.delay(3).toMillis() < 675L);

        assertThrows(IllegalArgumentException.class, () -> policy.delay(4));

        MutableClock clock = new MutableClock(NOW);
 RedisCircuitBreaker breaker = new RedisCircuitBreaker(clock, 2, Duration.ofSeconds(1));

        assertTrue(breaker.allowRequest());
 breaker.failure();
 breaker.failure();
 assertFalse(breaker.allowRequest());

        clock.advance(Duration.ofSeconds(1));
 assertTrue(breaker.allowRequest());
 assertEquals(RedisCircuitBreaker.State.HALF_OPEN, breaker.state());
 breaker.success();
 assertEquals(RedisCircuitBreaker.State.CLOSED, breaker.state());

    }

    @Test void streamProcessingSortsRejectsDuplicatesAndStopsOnFailure() {
        RedisDeduplicationStore store = new RedisDeduplicationStore(CLOCK, 20);
 RedisStreamProcessor processor = new RedisStreamProcessor(store, 1);

        RedisKey stream = RedisKey.of(namespace, "stream", "events");
 StreamCursor cursor = StreamCursor.after(stream, StreamId.of(0, 0));

        StreamRecord second = record("b", 2, StreamId.of(2, 0));
 StreamRecord first = record("a", 1, StreamId.of(1, 0));

        java.util.List<Long> order = new java.util.ArrayList<Long>();
        StreamCursor result = processor.process(cursor, Arrays.asList(second, first, first), value -> {
            order.add(value.id().epochMillis());
            return true;
        });

        assertEquals(Arrays.asList(1L, 2L), order);
 assertEquals(StreamId.of(2, 0), result.lastConsumed());

        StreamCursor stopped = processor.process(result, Collections.singletonList(record("c", 3, StreamId.of(3, 0))), value -> false);
 assertEquals(result, stopped);

        assertThrows(IllegalArgumentException.class, () -> new RedisStreamProcessor(store, 4));

    }

    @Test void unopenedLettuceAdapterFailsFastAndReportsDegradation() throws Exception {
        RedisAdapterConfig config = RedisAdapterConfig.of(new URI("redis://localhost:6379"), namespace, 1, 2, Duration.ofMillis(100));

        RedisCircuitBreaker breaker = new RedisCircuitBreaker(CLOCK, 1, Duration.ofSeconds(1));

        LettuceRedisAdapter adapter = new LettuceRedisAdapter(config, CLOCK, breaker);

        assertEquals("not_started", adapter.health().toCompletableFuture().join().diagnosticCode());

        assertThrows(CompletionException.class, () -> adapter.publish(RedisKey.of(namespace, "cache", "x"), new byte[] {1}).toCompletableFuture().join());

        RedisNamespace foreign = RedisNamespace.of("other", "test", "coord", SchemaVersion.of(1, 0));

        assertThrows(SecurityException.class, () -> adapter.publish(RedisKey.of(foreign, "cache", "x"), new byte[] {1}));

        assertThrows(IllegalArgumentException.class, () -> adapter.publish(RedisKey.of(namespace, "cache", "x"), new byte[256 * 1024 + 1]));

        RedisKey streamKey = RedisKey.of(namespace, "stream", "coordination");
        assertThrows(CompletionException.class, () -> adapter.createConsumerGroup(streamKey, "workers").toCompletableFuture().join());
        assertThrows(CompletionException.class, () -> adapter.readGroup(streamKey, "workers", "node-1", 1).toCompletableFuture().join());
        assertThrows(CompletionException.class, () -> adapter.readGroup(streamKey, "workers", "node-1", 0).toCompletableFuture().join());
        assertThrows(CompletionException.class, () -> adapter.acknowledge(streamKey, "workers", "1-0").toCompletableFuture().join());
        assertThrows(CompletionException.class, () -> adapter.acknowledge(streamKey, "workers").toCompletableFuture().join());
 adapter.close();

    }

    private StreamRecord record(final String id, final long sequence, final StreamId streamId) {
        UUID event = UUID.nameUUIDFromBytes(("event-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        UUID correlation = UUID.nameUUIDFromBytes(("correlation-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        MessageEnvelope envelope = MessageEnvelope.of(IdempotencyKey.of("zartra", id), EventMetadata.of(EventId.of(event), EventTypeId.of("zartra", "redis"), CorrelationId.of(correlation), NOW, sequence, 1, EventMetadata.ThreadContext.APPLICATION_WORKER), new byte[] {7}, NOW);

        return StreamRecord.of(streamId, envelope);

    }

    private static final class MutableClock extends Clock {
        private Instant value;
 MutableClock(final Instant value) { this.value = value;
 } void advance(final Duration duration) { value = value.plus(duration);
 }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC;
 }
        @Override public Clock withZone(final java.time.ZoneId zone) { return this;
 }
        @Override public Instant instant() { return value;
 }
    }
}
