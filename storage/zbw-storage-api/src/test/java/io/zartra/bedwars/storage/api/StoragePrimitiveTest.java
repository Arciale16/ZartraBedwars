package io.zartra.bedwars.storage.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit coverage for every exported M04 storage primitive. */
final class StoragePrimitiveTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

    @Test void recordKeysAndRevisionsAreTypedAndStable() {
        final RecordKey key = key("one");
        assertEquals(key, key("one"));
        assertNotEquals(key, key("two"));
        assertEquals(key.hashCode(), key("one").hashCode());
        assertEquals("zartra:test/zartra:one", key.toString());
        assertThrows(NullPointerException.class, () -> RecordKey.of(null, DefinitionId.of("zartra", "x")));

        final RecordRevision initial = RecordRevision.initial();
        assertEquals(0L, initial.value());
        assertEquals(1L, initial.next().value());
        assertTrue(initial.compareTo(initial.next()) < 0);
        assertEquals("0", initial.toString());
        assertThrows(IllegalArgumentException.class, () -> RecordRevision.of(-1));
        assertThrows(IllegalStateException.class, () -> RecordRevision.of(Long.MAX_VALUE).next());
    }

    @Test void storedRecordDefensivelyCopiesAndValidatesPayload() {
        final byte[] payload = {1, 2, 3};
        final StoredRecord record = StoredRecord.of(key("one"), RecordRevision.of(2), 3, payload, NOW);
        payload[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, record.payload());
        final byte[] copy = record.payload();
        copy[1] = 8;
        assertArrayEquals(new byte[] {1, 2, 3}, record.payload());
        assertEquals(record, StoredRecord.of(key("one"), RecordRevision.of(2), 3,
                new byte[] {1, 2, 3}, NOW));
        assertEquals(record.hashCode(), StoredRecord.of(key("one"), RecordRevision.of(2), 3,
                new byte[] {1, 2, 3}, NOW).hashCode());
        assertThrows(IllegalArgumentException.class, () -> StoredRecord.of(key("one"),
                RecordRevision.initial(), 0, new byte[] {1}, NOW));
        assertThrows(IllegalArgumentException.class, () -> StoredRecord.of(key("one"),
                RecordRevision.initial(), 1, new byte[0], NOW));
    }

    @Test void transactionOptionsBoundTimeoutAndRetries() {
        final TransactionOptions options = TransactionOptions.of(
                TransactionOptions.AccessMode.READ_WRITE, Duration.ofSeconds(3), 4);
        assertEquals(TransactionOptions.AccessMode.READ_WRITE, options.accessMode());
        assertEquals(Duration.ofSeconds(3), options.timeout());
        assertEquals(4, options.deadlockRetries());
        assertThrows(IllegalArgumentException.class, () -> TransactionOptions.of(
                TransactionOptions.AccessMode.READ_ONLY, Duration.ZERO, 0));
        assertThrows(IllegalArgumentException.class, () -> TransactionOptions.of(
                TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(1), 17));
    }

    @Test void messageEnvelopeCopiesPayloadAndBatchIsBounded() {
        final MessageEnvelope envelope = envelope("one", 4);
        assertEquals(IdempotencyKey.of("zartra", "one"), envelope.operationId());
        assertArrayEquals(new byte[] {7, 8}, envelope.payload());
        assertEquals(NOW.plusSeconds(2), envelope.availableAt());
        assertEquals(envelope, envelope("one", 4));
        assertEquals(envelope.hashCode(), envelope("one", 4).hashCode());
        assertEquals(1, MessageRepository.Batches.bounded(Collections.singletonList(envelope), 1).size());
        assertThrows(IllegalArgumentException.class, () -> MessageRepository.Batches.bounded(
                Collections.singletonList(envelope), 0));
        assertThrows(IllegalArgumentException.class, () -> MessageEnvelope.of(
                envelope.operationId(), envelope.metadata(), new byte[0], NOW));
    }

    @Test void migrationPlanEnforcesOrderChecksumAndBackupFlag() {
        final String checksum = repeat('a', 64);
        final Migration first = Migration.of(1, "create_records", checksum, false);
        final Migration second = Migration.of(2, "add_index", repeat('b', 64), true);
        final MigrationPlan plan = MigrationPlan.of(Arrays.asList(first, second));
        assertEquals(2, plan.targetVersion());
        assertTrue(plan.requiresBackup());
        assertTrue(first.compareTo(second) < 0);
        assertEquals(first, Migration.of(1, "create_records", checksum, false));
        assertEquals(first.hashCode(), Migration.of(1, "create_records", checksum, false).hashCode());
        assertThrows(IllegalArgumentException.class, () -> Migration.of(0, "bad", checksum, false));
        assertThrows(IllegalArgumentException.class, () -> Migration.of(1, "Bad", checksum, false));
        assertThrows(IllegalArgumentException.class, () -> Migration.of(1, "bad", "no", false));
        assertThrows(IllegalArgumentException.class, () -> MigrationPlan.of(Collections.singletonList(second)));
    }

    @Test void retentionAndRecoveryValuesEnforceObjectives() {
        final RetentionPolicy policy = RetentionPolicy.of(DefinitionId.of("zartra", "replay"),
                Duration.ofDays(30), Duration.ofDays(30));
        assertEquals(Duration.ofDays(30), policy.retention());
        assertEquals("zartra:replay", policy.retentionClass().toString());
        assertThrows(IllegalArgumentException.class, () -> RetentionPolicy.of(
                DefinitionId.of("zartra", "bad"), Duration.ZERO, Duration.ofDays(1)));

        final RecoveryService.RecoveryObjectives objectives = RecoveryService.RecoveryObjectives.of(
                Duration.ZERO, Duration.ofMinutes(15));
        assertEquals(Duration.ZERO, objectives.maximumDataLoss());
        assertEquals(Duration.ofMinutes(15), objectives.maximumServiceRestore());
        assertThrows(IllegalArgumentException.class, () -> RecoveryService.RecoveryObjectives.of(
                Duration.ofSeconds(-1), Duration.ofMinutes(1)));
        final RecoveryService.BackupEvidence evidence = RecoveryService.BackupEvidence.of(
                DefinitionId.of("zartra", "nightly"), NOW, repeat('c', 64));
        assertEquals(NOW, evidence.completedAt());
        assertEquals("zartra:nightly", evidence.backupId().toString());
        assertFalse(evidence.checksum().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> RecoveryService.BackupEvidence.of(
                DefinitionId.of("zartra", "bad"), NOW, "bad"));
    }

    @Test void nullMalformedAndEqualityBranchesAreExplicit() {
        final RecordKey one = key("one");
        assertEquals(one, one);
        assertNotEquals(one, "one");
        assertNotEquals(one, key("two"));
        assertThrows(NullPointerException.class, () -> RecordKey.of(
                DefinitionId.of("zartra", "test"), null));

        final StoredRecord record = StoredRecord.of(one, RecordRevision.of(1), 1,
                new byte[] {1}, NOW);
        assertEquals(record, record);
        assertNotEquals(record, "record");
        assertNotEquals(record, StoredRecord.of(key("two"), RecordRevision.of(1), 1,
                new byte[] {1}, NOW));
        assertNotEquals(record, StoredRecord.of(one, RecordRevision.of(2), 1,
                new byte[] {1}, NOW));
        assertNotEquals(record, StoredRecord.of(one, RecordRevision.of(1), 2,
                new byte[] {1}, NOW));
        assertNotEquals(record, StoredRecord.of(one, RecordRevision.of(1), 1,
                new byte[] {2}, NOW));
        assertThrows(IllegalArgumentException.class, () -> StoredRecord.of(one,
                RecordRevision.initial(), 1, null, NOW));
        assertThrows(NullPointerException.class, () -> StoredRecord.of(null,
                RecordRevision.initial(), 1, new byte[] {1}, NOW));
        assertThrows(NullPointerException.class, () -> StoredRecord.of(one,
                null, 1, new byte[] {1}, NOW));

        assertThrows(NullPointerException.class, () -> TransactionOptions.of(
                null, Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> TransactionOptions.of(
                TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(1), -1));

        final MessageEnvelope envelope = envelope("one", 1);
        assertEquals(envelope, envelope);
        assertNotEquals(envelope, "envelope");
        assertNotEquals(envelope, envelope("two", 1));
        assertNotEquals(envelope, envelope("one", 2));
        assertThrows(IllegalArgumentException.class, () -> MessageEnvelope.of(
                envelope.operationId(), envelope.metadata(), null, NOW));
        assertThrows(NullPointerException.class, () -> MessageEnvelope.of(
                null, envelope.metadata(), new byte[] {1}, NOW));

        final Migration migration = Migration.of(1, "one", repeat('a', 64), false);
        assertEquals(migration, migration);
        assertNotEquals(migration, "migration");
        assertNotEquals(migration, Migration.of(1, "two", repeat('a', 64), false));
        assertNotEquals(migration, Migration.of(1, "one", repeat('b', 64), false));
        assertNotEquals(migration, Migration.of(1, "one", repeat('a', 64), true));
        assertFalse(migration.unsafeDdl());
        assertEquals("one", migration.description());
        assertThrows(IllegalArgumentException.class, () -> Migration.of(1, null,
                repeat('a', 64), false));
        assertThrows(IllegalArgumentException.class, () -> Migration.of(1, "one", null, false));

        final MigrationPlan plan = MigrationPlan.of(Collections.singletonList(migration));
        assertFalse(plan.requiresBackup());
        assertEquals(migration, plan.migrations().get(0));
        assertThrows(IllegalArgumentException.class, () -> MigrationPlan.of(null));
        assertThrows(IllegalArgumentException.class, () -> MigrationPlan.of(
                Collections.<Migration>emptyList()));
        assertThrows(NullPointerException.class, () -> MigrationPlan.of(
                Collections.<Migration>singletonList(null)));

        assertThrows(IllegalArgumentException.class, () -> RetentionPolicy.of(
                DefinitionId.of("zartra", "bad"), Duration.ofDays(1), Duration.ZERO));
        assertThrows(NullPointerException.class, () -> RetentionPolicy.of(
                null, Duration.ofDays(1), Duration.ofDays(1)));
        assertThrows(IllegalArgumentException.class, () -> RecoveryService.RecoveryObjectives.of(
                Duration.ZERO, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> RecoveryService.RecoveryObjectives.of(
                null, Duration.ofSeconds(1)));
        assertThrows(NullPointerException.class, () -> RecoveryService.BackupEvidence.of(
                null, NOW, repeat('c', 64)));
        assertFalse(RecordRevision.initial().equals("0"));
    }

    private static RecordKey key(final String id) {
        return RecordKey.of(DefinitionId.of("zartra", "test"), DefinitionId.of("zartra", id));
    }
    private static MessageEnvelope envelope(final String id, final long sequence) {
        final UUID event = UUID.nameUUIDFromBytes(("event-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        final UUID correlation = UUID.nameUUIDFromBytes(("correlation-" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return MessageEnvelope.of(IdempotencyKey.of("zartra", id), EventMetadata.of(
                EventId.of(event), EventTypeId.of("zartra", "test"), CorrelationId.of(correlation),
                NOW, sequence, 1, EventMetadata.ThreadContext.APPLICATION_WORKER),
                new byte[] {7, 8}, NOW.plusSeconds(2));
    }
    private static String repeat(final char value, final int count) {
        final char[] chars = new char[count];
        Arrays.fill(chars, value);
        return new String(chars);
    }
}
