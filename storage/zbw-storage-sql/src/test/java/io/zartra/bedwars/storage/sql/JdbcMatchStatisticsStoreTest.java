package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.statistics.model.MatchStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.runtime.MatchStatisticsProjection;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SQLite round-trip, duplicate, conflict, rollback and restart evidence for M15 match state. */
final class JdbcMatchStatisticsStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final MatchId MATCH = MatchId.of(new UUID(0, 150));
    private static final StatisticId WINS = StatisticId.of("zartra", "wins");
    @TempDir Path temporary;

    @Test void insertUpdateAndRestartRoundTripAreDeterministic() {
        final Path database = temporary.resolve("match.db");
        try (JdbcStorageEngine engine = open(database)) {
            migrate(engine);
            final JdbcMatchStatisticsStore store = new JdbcMatchStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                final MatchStatistic loaded = store.find(unit, MATCH, WINS).requireValue().get();
                assertEquals(1, loaded.value());
                assertEquals(1, loaded.revision().value());
                store.save(unit, value(2, 2), loaded.revision()).requireValue();
                unit.commit().requireValue();
            }
        }
        try (JdbcStorageEngine engine = open(database); UnitOfWork unit = read(engine)) {
            final MatchStatistic loaded = new JdbcMatchStatisticsStore(5)
                    .find(unit, MATCH, WINS).requireValue().get();
            assertEquals(2, loaded.value());
            assertEquals(2, loaded.revision().value());
            unit.commit().requireValue();
        }
    }

    @Test void projectionSuppressesDuplicatesAndReportsRevisionConflicts() {
        try (JdbcStorageEngine engine = open(temporary.resolve("projection.db"))) {
            migrate(engine);
            final JdbcMatchStatisticsStore store = new JdbcMatchStatisticsStore(5);
            final MatchStatisticsProjection projection = new MatchStatisticsProjection(store);
            try (UnitOfWork unit = write(engine)) {
                assertEquals(MatchStatisticsProjection.Outcome.Status.APPLIED,
                        projection.project(unit, value(1, 1), RecordRevision.initial(),
                                key("one"), NOW).requireValue().status());
                assertEquals(MatchStatisticsProjection.Outcome.Status.DUPLICATE,
                        projection.project(unit, value(1, 1), RecordRevision.initial(),
                                key("one"), NOW).requireValue().status());
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertEquals(MatchStatisticsProjection.Outcome.Status.REVISION_CONFLICT,
                        projection.project(unit, value(3, 100), RecordRevision.of(99),
                                key("conflict"), NOW).requireValue().status());
                unit.rollback().requireValue();
            }
        }
    }

    @Test void rollbackRemovesClaimAndAggregateAndMissingReadsAreEmpty() {
        try (JdbcStorageEngine engine = open(temporary.resolve("rollback.db"))) {
            migrate(engine);
            final JdbcMatchStatisticsStore store = new JdbcMatchStatisticsStore(5);
            try (UnitOfWork unit = read(engine)) {
                assertFalse(store.find(unit, MATCH, WINS).requireValue().isPresent());
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.rollback().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                assertFalse(store.find(unit, MATCH, WINS).requireValue().isPresent());
                unit.rollback().requireValue();
            }
        }
    }

    @Test void migrationIsChecksumStableAndInvalidRevisionIsRejected() {
        try (JdbcStorageEngine engine = open(temporary.resolve("migration.db"))) {
            final String first = migrate(engine);
            assertEquals(first, migrate(engine));
            final JdbcMatchStatisticsStore store = new JdbcMatchStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                assertThrows(IllegalArgumentException.class, () -> store.save(unit,
                        value(1, 1), RecordRevision.of(7)));
                unit.rollback().requireValue();
            }
        }
    }

    private static MatchStatistic value(final long value, final long revision) {
        return new MatchStatistic(MATCH, WINS, value, RecordRevision.of(revision),
                new StatisticAudit("test", CorrelationId.of(new UUID(0, 151)), NOW));
    }
    private static IdempotencyKey key(final String value) { return IdempotencyKey.of("m15", value); }
    private static String migrate(final JdbcStorageEngine engine) {
        try (UnitOfWork unit = write(engine)) {
            final StatisticsSchemaMigrator migrator = new StatisticsSchemaMigrator(5);
            final String checksum = migrator.checksum();
            migrator.migrate(((JdbcUnitOfWork) unit).connection()).requireValue();
            unit.commit().requireValue();
            return checksum;
        }
    }
    private static UnitOfWork write(final JdbcStorageEngine engine) { return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE, Duration.ofSeconds(5), 2)).requireValue(); }
    private static UnitOfWork read(final JdbcStorageEngine engine) { return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(5), 2)).requireValue(); }
    private static JdbcStorageEngine open(final Path database) { return JdbcStorageEngine.open(SqlStorageConfiguration.of(StorageEngine.EngineKind.SQLITE, "jdbc:sqlite:" + database.toAbsolutePath(), "", new char[0], 1, Duration.ofSeconds(5), Duration.ofSeconds(5))).requireValue(); }
}
