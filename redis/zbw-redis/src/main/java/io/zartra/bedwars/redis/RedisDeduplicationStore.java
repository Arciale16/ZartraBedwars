package io.zartra.bedwars.redis;


import io.zartra.bedwars.redis.api.DeduplicationKey;

import java.time.Clock;

import java.time.Duration;

import java.util.Iterator;

import java.util.LinkedHashMap;

import java.util.Map;

import java.util.Objects;


/** Thread-safe bounded local replay guard complementing Redis/SQL idempotency. */
public final class RedisDeduplicationStore {
    /** Required operation retention. */ public static final Duration RETENTION = Duration.ofHours(24);

    /** Required hard entry ceiling. */ public static final int MAX_ENTRIES = 250000;

    private final Clock clock;

    private final int capacity;

    private final Map<DeduplicationKey, Long> entries = new LinkedHashMap<DeduplicationKey, Long>();

    /** Creates a store capped at the normative maximum. */
    public RedisDeduplicationStore(final Clock clock, final int capacity) {
        this.clock = Objects.requireNonNull(clock, "clock");

        if (capacity < 1 || capacity > MAX_ENTRIES) {
            throw new IllegalArgumentException("dedupe capacity outside bounds");

        }
        this.capacity = capacity;

    }
    /** Records an operation, returning false for a live duplicate. */
    public synchronized boolean record(final DeduplicationKey key) {
        Objects.requireNonNull(key, "key");

        purgeExpired();

        if (entries.containsKey(key)) { return false;
 }
        if (entries.size() >= capacity) {
            final Iterator<DeduplicationKey> iterator = entries.keySet().iterator();

            iterator.next();

            iterator.remove();

        }
        entries.put(key, clock.millis() + RETENTION.toMillis());

        return true;

    }
    /** Returns current bounded size. */ public synchronized int size() { purgeExpired();
 return entries.size();
 }
    private void purgeExpired() {
        final long now = clock.millis();

        final Iterator<Map.Entry<DeduplicationKey, Long>> iterator = entries.entrySet().iterator();

        while (iterator.hasNext()) {
            if (iterator.next().getValue().longValue() <= now) { iterator.remove();
 }
        }
    }
}
