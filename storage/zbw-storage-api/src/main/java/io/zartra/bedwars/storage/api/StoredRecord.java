package io.zartra.bedwars.storage.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** Immutable versioned payload at a persistence serialization boundary. */
public final class StoredRecord {
    private final RecordKey key;
    private final RecordRevision revision;
    private final int schemaVersion;
    private final byte[] payload;
    private final Instant updatedAt;

    private StoredRecord(final RecordKey key, final RecordRevision revision,
                         final int schemaVersion, final byte[] payload, final Instant updatedAt) {
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        if (payload == null || payload.length == 0) { throw new IllegalArgumentException("payload must not be empty"); }
        this.key = Objects.requireNonNull(key, "key");
        this.revision = Objects.requireNonNull(revision, "revision");
        this.schemaVersion = schemaVersion;
        this.payload = Arrays.copyOf(payload, payload.length);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /** @return validated immutable record */
    public static StoredRecord of(final RecordKey key, final RecordRevision revision,
                                  final int schemaVersion, final byte[] payload, final Instant updatedAt) {
        return new StoredRecord(key, revision, schemaVersion, payload, updatedAt);
    }

    /** @return typed key */ public RecordKey key() { return key; }
    /** @return optimistic revision */ public RecordRevision revision() { return revision; }
    /** @return positive serialization schema */ public int schemaVersion() { return schemaVersion; }
    /** @return defensive payload copy */ public byte[] payload() { return Arrays.copyOf(payload, payload.length); }
    /** @return durable update timestamp */ public Instant updatedAt() { return updatedAt; }

    @Override public int hashCode() {
        return 31 * Objects.hash(key, revision, schemaVersion, updatedAt) + Arrays.hashCode(payload);
    }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof StoredRecord)) { return false; }
        final StoredRecord that = (StoredRecord) other;
        return schemaVersion == that.schemaVersion && key.equals(that.key)
                && revision.equals(that.revision) && updatedAt.equals(that.updatedAt)
                && Arrays.equals(payload, that.payload);
    }
}
