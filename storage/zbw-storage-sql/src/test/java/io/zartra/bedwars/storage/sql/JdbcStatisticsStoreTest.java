package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.statistics.model.PlayerStatistic;
import io.zartra.bedwars.statistics.model.StatisticAudit;
import io.zartra.bedwars.statistics.model.StatisticId;
import io.zartra.bedwars.statistics.model.StatisticScope;
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

/** SQLite evidence for player-statistic aggregate persistence branches in M15. */
final class JdbcStatisticsStoreTest {
    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final PlayerId PLAYER = PlayerId.of(new UUID(0, 160));
    private static final StatisticId WINS = StatisticId.of("zartra", "wins");
    private static final StatisticScope SCOPE = StatisticScope.of("zartra", "lobby");

    @TempDir Path temporary;

    @Test
    void insertUpdateRoundTripAndRestartAreDeterministic() {
        final Path database = temporary.resolve("player.db");
        try (JdbcStorageEngine engine = open(database)) {
            migrate(engine);
            final JdbcStatisticsStore store = new JdbcStatisticsStore(5);
            try (UnitOfWork unit = write(engine)) {
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.commit().requireValue();
            }
            try (UnitOfWork unit = write(engine)) {
                final PlayerStatistic loaded = store.find(unit, PLAYER, WINS, SCOPE)
                        .requireValue().get();
                assertEquals(1, loaded.value());
                assertEquals(1, loaded.revision().value());
                store.save(unit, value(3, 2), loaded.revision()).requireValue();
                unit.commit().requireValue();
            }
        }

        try (JdbcStorageEngine engine = open(database); UnitOfWork unit = read(engine)) {
            final PlayerStatistic loaded = new JdbcStatisticsStore(5)
                    .find(unit, PLAYER, WINS, SCOPE).requireValue().get();
            assertEquals(3, loaded.value());
            assertEquals(2, loaded.revision().value());
            unit.commit().requireValue();
        }
    }

    @Test
    void claimDuplicateAndRollbackSafetyAreDeterministic() {
        try (JdbcStorageEngine engine = open(temporary.resolve("claim.db"))) {
            migrate(engine);
            final JdbcStatisticsStore store = new JdbcStatisticsStore(5);

            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("dup"), NOW).requireValue());
                assertFalse(store.claim(unit, key("dup"), NOW).requireValue());
                unit.commit().requireValue();
            }

            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                unit.rollback().requireValue();
            }

            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.claim(unit, key("rollback"), NOW).requireValue());
                unit.commit().requireValue();
            }
        }
    }

    @Test
    void missingAggregateAndRevisionConflictPathsAreHandled() {
        try (JdbcStorageEngine engine = open(temporary.resolve("revision.db"))) {
            migrate(engine);
            final JdbcStatisticsStore store = new JdbcStatisticsStore(5);

            try (UnitOfWork unit = read(engine)) {
                assertFalse(store.find(unit, PLAYER, WINS, SCOPE).requireValue().isPresent());
                unit.commit().requireValue();
            }

            try (UnitOfWork unit = write(engine)) {
                store.save(unit, value(1, 1), RecordRevision.initial()).requireValue();
                unit.commit().requireValue();
            }

            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.save(unit, value(2, 2), RecordRevision.initial()).isFailure());
                unit.rollback().requireValue();
            }

            try (UnitOfWork unit = write(engine)) {
                assertTrue(store.save(unit, value(3, 3), RecordRevision.of(99)).isFailure());
                unit.rollback().requireValue();
            }
        }
    }

    @Test
    void readOnlyTransactionAndMalformedInputPathsAreRejected() {
        try (JdbcStorageEngine engine = open(temporary.resolve("invalid.db"))) {
            migrate(engine);
            final JdbcStatisticsStore store = new JdbcStatisticsStore(5);

            try (UnitOfWork read = read(engine)) {
                assertThrows(IllegalStateException.class,
                        () -> store.save(read, value(1, 1), RecordRevision.initial()));
            }

            try (UnitOfWork unit = write(engine)) {
                assertThrows(NullPointerException.class,
                        () -> store.find(null, PLAYER, WINS, SCOPE));
                assertThrows(NullPointerException.class,
                        () -> store.find(unit, null, WINS, SCOPE));
                assertThrows(NullPointerException.class,
                        () -> store.claim(unit, null, NOW));
                assertThrows(IllegalArgumentException.class,
                        () -> new PlayerStatistic(PLAYER, WINS, SCOPE, -1,
                                RecordRevision.of(1),
                                new StatisticAudit("test", CorrelationId.of(new UUID(0, 163)), NOW)));
                assertThrows(NullPointerException.class,
                        () -> new PlayerStatistic(null, WINS, SCOPE, 1,
                                RecordRevision.of(1),
                                new StatisticAudit("test", CorrelationId.of(new UUID(0, 164)), NOW)));
            }
        }
    }

    private static PlayerStatistic value(final long value, final long revision) {
        return new PlayerStatistic(PLAYER, WINS, SCOPE, value, RecordRevision.of(revision),
                new StatisticAudit("test", CorrelationId.of(new UUID(0, 162)), NOW));
    }

    private static IdempotencyKey key(final String value) {
        return IdempotencyKey.of("m15-player", value);
    }

    private static void migrate(final JdbcStorageEngine engine) {
        try (UnitOfWork unit = write(engine)) {
            new StatisticsSchemaMigrator(5).migrate(((JdbcUnitOfWork) unit).connection())
                    .requireValue();
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
        return JdbcStorageEngine.open(SqlStorageConfiguration.of(
                StorageEngine.EngineKind.SQLITE,
                "jdbc:sqlite:" + database.toAbsolutePath(),
                "",
                new char[0],
                1,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5))).requireValue();
    }
}
