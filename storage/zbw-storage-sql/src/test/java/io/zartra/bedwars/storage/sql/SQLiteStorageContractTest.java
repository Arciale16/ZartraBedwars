package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CaseId;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.RetentionPolicy;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real SQLite contract, crash, idempotency, transaction and privacy-storage suite. */
final class SQLiteStorageContractTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

    @TempDir Path temporary;

    @Test void recordTransactionsCommitConflictDeleteAndRollback() {
        final JdbcStorageEngine engine = open("records.db");
        final RecordKey key = key("player-one");
        final StoredRecord initial = record(key, 0, 1);
        try (UnitOfWork transaction = write(engine)) {
            final StoredRecord saved = engine.records().save(
                    transaction, initial, RecordRevision.initial()).requireValue();
            assertEquals(1L, saved.revision().value());
            assertEquals(UnitOfWork.State.COMMITTED, transaction.commit().requireValue());
        }
        try (UnitOfWork read = read(engine)) {
            assertEquals(1L, engine.records().find(read, key).requireValue().get().revision().value());
            read.rollback();
        }
        try (UnitOfWork conflict = write(engine)) {
            assertTrue(engine.records().save(conflict, initial, RecordRevision.initial()).isFailure());
            conflict.rollback();
        }
        try (UnitOfWork update = write(engine)) {
            final StoredRecord changed = engine.records().save(
                    update, record(key, 1, 2), RecordRevision.of(1)).requireValue();
            assertEquals(2L, changed.revision().value());
            update.commit();
        }
        try (UnitOfWork rollback = write(engine)) {
            assertTrue(engine.records().delete(rollback, key, RecordRevision.of(2)).requireValue());
        }
        try (UnitOfWork verify = read(engine)) {
            assertTrue(engine.records().find(verify, key).requireValue().isPresent());
            verify.rollback();
        }
        try (UnitOfWork delete = write(engine)) {
            assertTrue(engine.records().delete(delete, key, RecordRevision.of(2)).requireValue());
            delete.commit();
        }
        try (UnitOfWork verify = read(engine)) {
            assertFalse(engine.records().find(verify, key).requireValue().isPresent());
            verify.rollback();
        }
        engine.close();
        assertTrue(engine.begin(options(TransactionOptions.AccessMode.READ_ONLY)).isFailure());
    }

    @Test void outboxAndInboxProvideOrderedIdempotentOutcomes() {
        final JdbcStorageEngine engine = open("messages.db");
        final MessageEnvelope later = envelope("later", 2, NOW.plusSeconds(2));
        final MessageEnvelope first = envelope("first", 1, NOW);
        try (UnitOfWork transaction = write(engine)) {
            assertTrue(engine.messages().enqueue(transaction, later).requireValue());
            assertTrue(engine.messages().enqueue(transaction, first).requireValue());
            assertFalse(engine.messages().enqueue(transaction, first).requireValue());
            assertTrue(engine.messages().receive(transaction, first).requireValue());
            assertFalse(engine.messages().receive(transaction, first).requireValue());
            transaction.commit();
        }
        try (UnitOfWork claim = write(engine)) {
            final List<MessageEnvelope> claimed = engine.messages().claim(
                    claim, NOW.plusSeconds(3), 10, Duration.ofSeconds(30)).requireValue();
            assertEquals(2, claimed.size());
            assertEquals(first.operationId(), claimed.get(0).operationId());
            claim.commit();
        }
        try (UnitOfWork duplicateClaim = write(engine)) {
            assertTrue(engine.messages().claim(duplicateClaim, NOW.plusSeconds(4), 10,
                    Duration.ofSeconds(30)).requireValue().isEmpty());
            duplicateClaim.rollback();
        }
        try (UnitOfWork acknowledge = write(engine)) {
            assertTrue(engine.messages().acknowledge(acknowledge, first.operationId()).requireValue());
            assertFalse(engine.messages().acknowledge(acknowledge, first.operationId()).requireValue());
            acknowledge.commit();
        }
        engine.close();
    }

    @Test void retentionHoldReleaseAndTombstoneAreDuplicateSafe() {
        final JdbcStorageEngine engine = open("privacy.db");
        final RecordKey key = key("evidence");
        final CaseId caseId = CaseId.of(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        final PlayerId operator = PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        final RetentionPolicy policy = RetentionPolicy.of(DefinitionId.of("zartra", "reported_replay"),
                Duration.ofDays(90), Duration.ofDays(30));
        try (UnitOfWork transaction = write(engine)) {
            assertTrue(engine.retention().retain(transaction, key, policy, NOW.plus(Duration.ofDays(90))).requireValue());
            assertTrue(engine.retention().hold(transaction, caseId, key, operator, NOW).requireValue());
            assertFalse(engine.retention().hold(transaction, caseId, key, operator, NOW).requireValue());
            assertTrue(engine.retention().release(transaction, caseId, operator, NOW.plusSeconds(1)).requireValue());
            assertFalse(engine.retention().release(transaction, caseId, operator, NOW.plusSeconds(2)).requireValue());
            assertTrue(engine.retention().tombstone(transaction, key, NOW.plusSeconds(3)).requireValue());
            assertFalse(engine.retention().tombstone(transaction, key, NOW.plusSeconds(4)).requireValue());
            transaction.commit();
        }
        engine.close();
    }

    @Test void abandonedUnitRollsBackAndSchemaSurvivesRestart() {
        final JdbcStorageEngine first = open("restart.db");
        final RecordKey key = key("crash");
        try (UnitOfWork abandoned = write(first)) {
            first.records().save(abandoned, record(key, 0, 1), RecordRevision.initial()).requireValue();
        }
        first.close();
        final JdbcStorageEngine restarted = open("restart.db");
        try (UnitOfWork verify = read(restarted)) {
            assertFalse(restarted.records().find(verify, key).requireValue().isPresent());
            verify.rollback();
        }
        assertEquals(EngineKind.SQLITE, restarted.kind());
        assertTrue(restarted.poolHealth().total() <= 1);
        restarted.close();
    }

    @Test void unitOfWorkRejectsCrossThreadAndReadOnlyMutation() throws Exception {
        final JdbcStorageEngine engine = open("threading.db");
        final UnitOfWork unit = read(engine);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<UnitOfWork.State> future = executor.submit(unit::state);
            final ExecutionException exception = assertThrows(ExecutionException.class, future::get);
            assertTrue(exception.getCause() instanceof IllegalStateException);
            assertThrows(IllegalStateException.class, () -> engine.records().save(unit,
                    record(key("bad"), 0, 1), RecordRevision.initial()));
        } finally {
            unit.close();
            executor.shutdownNow();
            engine.close();
        }
    }

    private JdbcStorageEngine open(final String file) {
        final String url = "jdbc:sqlite:" + temporary.resolve(file).toAbsolutePath().toString();
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(EngineKind.SQLITE, url,
                "", new char[0], 1, Duration.ofSeconds(2), Duration.ofSeconds(2))).requireValue();
    }
    private static UnitOfWork write(final JdbcStorageEngine engine) {
        return engine.begin(options(TransactionOptions.AccessMode.READ_WRITE)).requireValue();
    }
    private static UnitOfWork read(final JdbcStorageEngine engine) {
        return engine.begin(options(TransactionOptions.AccessMode.READ_ONLY)).requireValue();
    }
    private static TransactionOptions options(final TransactionOptions.AccessMode mode) {
        return TransactionOptions.of(mode, Duration.ofSeconds(2), 2);
    }
    private static RecordKey key(final String id) {
        return RecordKey.of(DefinitionId.of("zartra", "test"), DefinitionId.of("zartra", id));
    }
    private static StoredRecord record(final RecordKey key, final long revision, final int value) {
        return StoredRecord.of(key, RecordRevision.of(revision), 1, new byte[] {(byte) value}, NOW);
    }
    private static MessageEnvelope envelope(final String id, final long sequence,
                                            final Instant availableAt) {
        return MessageEnvelope.of(IdempotencyKey.of("zartra", id), EventMetadata.of(
                EventId.of(UUID.nameUUIDFromBytes(("event-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                EventTypeId.of("zartra", "test"),
                CorrelationId.of(UUID.nameUUIDFromBytes("correlation".getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                NOW, sequence, 1, EventMetadata.ThreadContext.APPLICATION_WORKER),
                new byte[] {5}, availableAt);
    }
}
