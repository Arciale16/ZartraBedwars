package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import io.zartra.bedwars.arena.archive.CanonicalArenaArchiveCodec;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import io.zartra.bedwars.storage.sql.JdbcStorageEngine;
import io.zartra.bedwars.storage.sql.SqlStorageConfiguration;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M04SqliteArenaContractTest {
    @TempDir Path temporary;

    @Test void arenaArchivePersistsAcrossRealSqliteRestart() {
        final Path database = temporary.resolve("arena-restart.db");
        final CanonicalArenaArchiveCodec codec = new CanonicalArenaArchiveCodec();
        final ArenaArchive archive = codec.encode(ArenaTestFixture.id("archive/sqlite"),
                ArenaTestFixture.complete(), ArenaTestFixture.NOW).requireValue();
        final RecordKey key = arenaKey();
        try (JdbcStorageEngine engine = open(database)) {
            try (UnitOfWork unit = begin(engine)) {
                assertTrue(engine.records().save(unit, StoredRecord.of(key,
                        RecordRevision.initial(), 1, archive.payload(), ArenaTestFixture.NOW),
                        RecordRevision.initial()).isSuccess());
                assertTrue(unit.commit().isSuccess());
            }
        }
        try (JdbcStorageEngine engine = open(database); UnitOfWork unit = beginRead(engine)) {
            final Optional<StoredRecord> found = engine.records().find(unit, key)
                    .requireValue();
            assertTrue(found.isPresent());
            final ArenaArchive recovered = ArenaArchive.create(archive.archiveId(),
                    archive.arenaId(), archive.mapId(), 1, archive.createdAt(),
                    found.get().payload());
            assertEquals(ArenaTestFixture.complete(), codec.decode(recovered).requireValue());
            assertTrue(unit.commit().isSuccess());
        }
    }

    @Test void arenaAndSetupRecordsCommitOrRollbackAsOneTransaction() {
        final Path database = temporary.resolve("arena-atomic.db");
        final RecordKey arena = arenaKey();
        final RecordKey setup = RecordKey.of(ArenaTestFixture.id("aggregate/setup_session"),
                ArenaTestFixture.id("setup/session-one"));
        try (JdbcStorageEngine engine = open(database)) {
            try (UnitOfWork unit = begin(engine)) {
                save(engine, unit, arena, new byte[] {1});
                save(engine, unit, setup, new byte[] {2});
                assertTrue(unit.commit().isSuccess());
            }
            try (UnitOfWork unit = begin(engine)) {
                final StoredRecord current = engine.records().find(unit, arena).requireValue().get();
                assertTrue(engine.records().save(unit, StoredRecord.of(arena, current.revision(),
                        1, new byte[] {3}, ArenaTestFixture.NOW), current.revision()).isSuccess());
                assertTrue(unit.rollback().isSuccess());
            }
            try (UnitOfWork unit = beginRead(engine)) {
                assertEquals(1, engine.records().find(unit, arena).requireValue().get().payload()[0]);
                assertEquals(2, engine.records().find(unit, setup).requireValue().get().payload()[0]);
                unit.commit();
            }
        }
    }

    @Test void staleRevisionAndImplicitCloseCannotPublishPartialArenaState() {
        final Path database = temporary.resolve("arena-stale.db");
        final RecordKey key = arenaKey();
        try (JdbcStorageEngine engine = open(database)) {
            try (UnitOfWork unit = begin(engine)) {
                save(engine, unit, key, new byte[] {7});
                unit.commit();
            }
            try (UnitOfWork unit = begin(engine)) {
                final StoredRecord changed = StoredRecord.of(key, RecordRevision.initial(), 1,
                        new byte[] {8}, ArenaTestFixture.NOW);
                assertFalse(engine.records().save(unit, changed,
                        RecordRevision.initial()).isSuccess());
            }
            try (UnitOfWork unit = beginRead(engine)) {
                assertEquals(7, engine.records().find(unit, key).requireValue().get().payload()[0]);
                unit.commit();
            }
        }
    }

    private static JdbcStorageEngine open(final Path database) {
        final SqlStorageConfiguration configuration = SqlStorageConfiguration.of(
                StorageEngine.EngineKind.SQLITE, "jdbc:sqlite:" + database.toAbsolutePath(),
                "", new char[0], 1, Duration.ofSeconds(5), Duration.ofSeconds(5));
        return JdbcStorageEngine.open(configuration).requireValue();
    }
    private static UnitOfWork begin(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE,
                Duration.ofSeconds(5), 1)).requireValue();
    }
    private static UnitOfWork beginRead(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_ONLY,
                Duration.ofSeconds(5), 0)).requireValue();
    }
    private static void save(final JdbcStorageEngine engine, final UnitOfWork unit,
                             final RecordKey key, final byte[] payload) {
        assertTrue(engine.records().save(unit, StoredRecord.of(key, RecordRevision.initial(), 1,
                payload, ArenaTestFixture.NOW), RecordRevision.initial()).isSuccess());
    }
    private static RecordKey arenaKey() {
        return RecordKey.of(ArenaTestFixture.id("aggregate/arena"), DefinitionId.of("zartra",
                "arena/" + ArenaTestFixture.ARENA_ID));
    }
}
