package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.RedisDeduplicationStore;
import io.zartra.bedwars.redis.api.DeduplicationKey;
import io.zartra.bedwars.redis.api.InvalidationVersion;
import io.zartra.bedwars.redis.api.RedisNamespace;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded duplicate-safe version tracker shared by statistics, rotation and neutral notifications.
 *
 * <p>The tracker contains metadata only. Cache contents and durable state stay in their owner.
 */
public final class VersionedCoordinationBridge {
    /** Consumption result used to decide whether an owner must invalidate or rebuild. */
    public enum Result {
        APPLIED,
        DUPLICATE,
        STALE
    }

    private final RedisNamespace namespace;
    private final RedisDeduplicationStore deduplication;
    private final int capacity;
    private final Map<String, InvalidationVersion> versions =
            new LinkedHashMap<String, InvalidationVersion>();

    /** Creates a bounded bridge with an explicit metadata ceiling. */
    public VersionedCoordinationBridge(final RedisNamespace namespace,
                                       final RedisDeduplicationStore deduplication,
                                       final int capacity) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.deduplication = Objects.requireNonNull(deduplication, "deduplication");
        if (capacity < 1 || capacity > 100000) {
            throw new IllegalArgumentException("coordination metadata capacity outside bounds");
        }
        this.capacity = capacity;
    }

    /** Applies only a first-seen event whose version is newer than local metadata. */
    public synchronized Result accept(final CoordinationEvent event) {
        final CoordinationEvent checked = Objects.requireNonNull(event, "event");
        final DeduplicationKey key = DeduplicationKey.of(namespace, checked.operationId());
        if (!deduplication.record(key)) {
            return Result.DUPLICATE;
        }
        final String subject = checked.type().name() + ':' + checked.subject();
        final InvalidationVersion current = versions.get(subject);
        if (current != null && checked.version().compareTo(current) <= 0) {
            return Result.STALE;
        }
        if (current == null && versions.size() >= capacity) {
            final Iterator<String> iterator = versions.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        versions.put(subject, checked.version());
        return Result.APPLIED;
    }

    /** Detects absent or stale local metadata so the owner can rebuild from SQL. */
    public synchronized boolean requiresRebuild(final CoordinationEvent.Type type,
                                                final String subject,
                                                final InvalidationVersion authorityVersion) {
        final InvalidationVersion local = versions.get(
                Objects.requireNonNull(type, "type").name() + ':'
                        + Objects.requireNonNull(subject, "subject"));
        return local == null || authorityVersion.isNewerThan(local);
    }

    /** @return bounded metadata entry count */ public synchronized int size() {
        return versions.size();
    }
}
