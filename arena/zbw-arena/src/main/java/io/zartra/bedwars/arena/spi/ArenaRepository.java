package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.arena.model.ArenaBundle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Durable atomic arena aggregate repository.
 *
 * <p>Implementations may block and therefore must only be called from an M05 bounded worker. A
 * save atomically stores the arena, map, revision and optional last-known-good image. Expected
 * revision zero creates a new record. Implementations must reject stale writers.</p>
 */
public interface ArenaRepository {
    /** @return record when present */ Result<Optional<Record>> find(ArenaId id);
    /** @return stable snapshot of all records */ Result<List<Record>> listRecords();
    /** @return stored record at its new revision */ Result<Record> save(SaveRequest request);
    /** @return whether the exact-revision record was deleted */ Result<Boolean> delete(ArenaId id, long expectedRevision);
    /** @return restored last-known-good record at a new revision */ Result<Record> restoreLastKnownGood(ArenaId id, long expectedRevision);

    /** Immutable durable record. */
    final class Record {
        private final ArenaBundle bundle;
        private final long revision;
        private final Optional<ArenaBundle> lastKnownGood;
        /** Creates a record snapshot. */
        public Record(final ArenaBundle bundle, final long revision,
                      final Optional<ArenaBundle> lastKnownGood) {
            this.bundle = Objects.requireNonNull(bundle, "bundle");
            if (revision < 1L) { throw new IllegalArgumentException("revision must be positive"); }
            this.revision = revision;
            this.lastKnownGood = Objects.requireNonNull(lastKnownGood, "lastKnownGood");
        }
        /** @return current atomic aggregate */ public ArenaBundle bundle() { return bundle; }
        /** @return durable optimistic revision */ public long revision() { return revision; }
        /** @return optional last-known-good image */ public Optional<ArenaBundle> lastKnownGood() { return lastKnownGood; }
    }

    /** Immutable atomic save request. */
    final class SaveRequest {
        private final ArenaBundle bundle;
        private final long expectedRevision;
        private final boolean promoteLastKnownGood;
        /** Creates a revision-fenced save request. */
        public SaveRequest(final ArenaBundle bundle, final long expectedRevision,
                           final boolean promoteLastKnownGood) {
            this.bundle = Objects.requireNonNull(bundle, "bundle");
            if (expectedRevision < 0L) { throw new IllegalArgumentException("expectedRevision is negative"); }
            this.expectedRevision = expectedRevision;
            this.promoteLastKnownGood = promoteLastKnownGood;
        }
        /** @return bundle to store */ public ArenaBundle bundle() { return bundle; }
        /** @return zero for create or current revision for update */ public long expectedRevision() { return expectedRevision; }
        /** @return whether this valid snapshot replaces last-known-good */ public boolean promoteLastKnownGood() { return promoteLastKnownGood; }
    }
}
