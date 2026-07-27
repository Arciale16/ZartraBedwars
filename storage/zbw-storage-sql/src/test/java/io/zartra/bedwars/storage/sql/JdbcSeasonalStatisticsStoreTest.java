package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.statistics.model.SeasonalStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
import io.zartra.bedwars.statistics.runtime.SeasonWindow;
import io.zartra.bedwars.statistics.runtime.SeasonalStatisticsProjection;
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

/** SQLite evidence for immutable, deterministic M15 seasonal aggregate persistence. */
final class JdbcSeasonalStatisticsStoreTest {
    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 170));
    private static final StatisticId WINS = StatisticId.of("zartra", "wins");
    private static final StatisticScope SEASON = StatisticScope.of("zartra", "summer-2026");
    private static final SeasonWindow WINDOW = new SeasonWindow(SEASON, 1, START, END);

    @TempDir
    Path temporary;

    @Test
    void seasonCreationUpdateAndRestartRoundTripAreDeterministic() {
        final Path database = temporary.resolve("season.db");
        try (JdbcStorageEngine engine = open(database)) {
            migrate(engine);
            final JdbcSeasonalStatisticsStore store = new JdbcSeasonalStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                final SeasonalStatistic loaded = store.find(unit, PLAYER, WINS, SEASON)
                        .requireValue().get();
                assertEquals(1, loaded.value());
                store.save(unit, value(2, 2), loaded.revision()).requireValue();
                unit.commit().requireValue();
            }
        }
        try (JdbcStorageEngine engine = open(database); UnitOfWork unit = read(engine)) {
            final SeasonalStatistic loaded = new JdbcSeasonalStatisticsStore(5)
                    .find(unit, PLAYER, WINS, SEASON).requireValue().get();
            assertEquals(2, loaded.value());
            assertEquals(2, loaded.revision().value());
            unit.commit().requireValue();
        }
    }

    @Test
    void projectionSuppressesDuplicatesAndReportsRevisionConflicts() {
        try (JdbcStorageEngine engine = open(temporary.resolve("projection.db"))) {
            migrate(engine);
            final SeasonalStatisticsProjection projection = new SeasonalStatisticsProjection(
                    new JdbcSeasonalStatisticsStore(5));
            try (UnitOfWork unit = write(engine)) {
                assertEquals(SeasonalStatisticsProjection.Outcome.Status.APPLIED,
                        projection.project(unit, value(1, 1), RecordRevision.initial(),
                                key("one"), NOW, WINDOW).requireValue().status());
                assertEquals(SeasonalStatisticsProjection.Outcome.Status.DUPLICATE,
                        projection.project(unit, value(1, 1), RecordRevision.initial(),
                                key("one"), NOW, WINDOW).requireValue().status());
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertEquals(SeasonalStatisticsProjection.Outcome.Status.REVISION_CONFLICT,
                        projection.project(unit, value(3, 100), RecordRevision.of(99),
                                key("conflict"), NOW, WINDOW).requireValue().status());
                unit.rollback().requireValue();
            }
        }
    }

    @Test
    void rollbackAndSeasonBoundariesPreventHistoricalMutation() {
        try (JdbcStorageEngine engine = open(temporary.resolve("rollback.db"))) {
            migrate(engine);
            final JdbcSeasonalStatisticsStore store = new JdbcSeasonalStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.rollback().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                assertFalse(store.find(unit, PLAYER, WINS, SEASON).requireValue().isPresent());
                unit.rollback().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                final SeasonalStatisticsProjection projection = new SeasonalStatisticsProjection(store);
                assertThrows(IllegalArgumentException.class, () -> projection.project(unit,
                        value(1, 1), RecordRevision.initial(), key("outside"), END, WINDOW));
                unit.rollback().requireValue();
            }
        }
        assertTrue(WINDOW.contains(START));
        assertFalse(WINDOW.contains(END));
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonWindow(SEASON, 0, START, END));
        assertThrows(IllegalArgumentException.class,
                () -> new SeasonWindow(SEASON, 1, END, START));
    }

    @Test
    void invalidValuesAndInvalidRevisionAreRejectedBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> value(-1, 1));
        try (JdbcStorageEngine engine = open(temporary.resolve("invalid.db"))) {
            migrate(engine);
            final JdbcSeasonalStatisticsStore store = new JdbcSeasonalStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                assertThrows(IllegalArgumentException.class, () -> store.save(unit,
                        value(1, 1), RecordRevision.of(7)));
                unit.rollback().requireValue();
            }
        }
    }

    private static SeasonalStatistic value(final long value, final long revision) {
        return new SeasonalStatistic(PLAYER, WINS, SEASON, value, RecordRevision.of(revision),
                new StatisticAudit("test", CorrelationId.of(new UUID(0, 171)), NOW));
    }

    private static IdempotencyKey key(final String value) {
        return IdempotencyKey.of("m15-season", value);
    }

    private static void migrate(final JdbcStorageEngine engine) {
        try (UnitOfWork unit = write(engine)) {
            final java.sql.Connection connection = ((JdbcUnitOfWork) unit).connection();
            new StatisticsSchemaMigrator(5).migrate(connection).requireValue();
            new TeamStatisticsSchemaMigrator(5).migrate(connection).requireValue();
            new SeasonalStatisticsSchemaMigrator(5).migrate(connection).requireValue();
            unit.commit().requireValue();
        }
    }

    private static UnitOfWork write(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE,
                Duration.ofSeconds(5), 2)).requireValue();
    }

    private static UnitOfWork read(final JdbcStorageEngine engine) {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_ONLY,
                Duration.ofSeconds(5), 2)).requireValue();
    }

    private static JdbcStorageEngine open(final Path database) {
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(StorageEngine.EngineKind.SQLITE,
                "jdbc:sqlite:" + database.toAbsolutePath(), "", new char[0], 1,
                Duration.ofSeconds(5), Duration.ofSeconds(5))).requireValue();
    }
}
