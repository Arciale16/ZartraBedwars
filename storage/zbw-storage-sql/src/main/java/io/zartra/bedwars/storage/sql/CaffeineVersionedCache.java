package io.zartra.bedwars.storage.sql;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.VersionedCache;
import java.time.Duration;
import java.util.Optional;

/** Caffeine cache with bounded entries, per-record expiry and revision fencing. */
public final class CaffeineVersionedCache implements VersionedCache {
    private final long maximumEntries;
    private final Cache<RecordKey, Entry> cache;

    /** Creates a cache with a strict positive entry bound. */
    public CaffeineVersionedCache(final long maximumEntries) {
        if (maximumEntries < 1 || maximumEntries > 10_000_000L) {
            throw new IllegalArgumentException("maximumEntries must be between 1 and 10000000");
        }
        this.maximumEntries = maximumEntries;
        this.cache = Caffeine.newBuilder().maximumSize(maximumEntries)
                .expireAfter(new EntryExpiry()).build();
    }

    @Override public void put(final StoredRecord record, final Duration lifetime) {
        if (record == null) { throw new NullPointerException("record"); }
        if (lifetime == null || lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        final long nanos;
        try { nanos = lifetime.toNanos(); }
        catch (ArithmeticException exception) { throw new IllegalArgumentException("lifetime is too large", exception); }
        cache.put(record.key(), new Entry(record, nanos));
    }

    @Override public Optional<StoredRecord> get(final RecordKey key,
                                                final RecordRevision minimumRevision) {
        if (key == null || minimumRevision == null) { throw new NullPointerException("cache lookup argument"); }
        final Entry entry = cache.getIfPresent(key);
        if (entry == null || entry.record.revision().compareTo(minimumRevision) < 0) {
            if (entry != null) { cache.invalidate(key); }
            return Optional.empty();
        }
        return Optional.of(entry.record);
    }

    @Override public void invalidate(final RecordKey key) {
        if (key == null) { throw new NullPointerException("key"); }
        cache.invalidate(key);
    }
    @Override public void invalidateAll() { cache.invalidateAll(); }
    @Override public long maximumEntries() { return maximumEntries; }
    @Override public long estimatedEntries() {
        cache.cleanUp();
        return cache.estimatedSize();
    }
    @Override public void close() {
        invalidateAll();
        cache.cleanUp();
    }

    private static final class Entry {
        private final StoredRecord record;
        private final long lifetimeNanos;
        private Entry(final StoredRecord record, final long lifetimeNanos) {
            this.record = record;
            this.lifetimeNanos = lifetimeNanos;
        }
    }

    private static final class EntryExpiry implements Expiry<RecordKey, Entry> {
        @Override public long expireAfterCreate(final RecordKey key, final Entry value,
                                                final long currentTime) {
            return value.lifetimeNanos;
        }
        @Override public long expireAfterUpdate(final RecordKey key, final Entry value,
                                                final long currentTime, final long currentDuration) {
            return value.lifetimeNanos;
        }
        @Override public long expireAfterRead(final RecordKey key, final Entry value,
                                              final long currentTime, final long currentDuration) {
            return currentDuration;
        }
    }
}
