package io.zartra.bedwars.storage.api;

import java.time.Duration;
import java.util.Optional;

/** Bounded version-aware local cache; SQL remains authoritative. */
public interface VersionedCache extends AutoCloseable {
    /** Stores a defensive record copy for a positive bounded lifetime. */
    void put(StoredRecord record, Duration lifetime);
    /** @return a cached record only when its revision is at least {@code minimumRevision} */
    Optional<StoredRecord> get(RecordKey key, RecordRevision minimumRevision);
    /** Invalidates one key after an authoritative mutation. */ void invalidate(RecordKey key);
    /** Invalidates every entry; intended for bounded lifecycle/config transitions. */ void invalidateAll();
    /** @return configured maximum entry count */ long maximumEntries();
    /** @return approximate current entry count */ long estimatedEntries();
    /** Releases cache resources. */ @Override void close();
}
