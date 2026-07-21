package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.CurrencyAccount;
import io.zartra.bedwars.progression.model.CurrencyId;
import io.zartra.bedwars.progression.model.EntitlementGrant;
import io.zartra.bedwars.progression.model.EntitlementId;
import io.zartra.bedwars.progression.model.ExperienceAmount;
import io.zartra.bedwars.progression.model.ExperienceLedgerEntry;
import io.zartra.bedwars.progression.model.LedgerEntry;
import io.zartra.bedwars.progression.model.LevelState;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.model.PrestigeState;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.progression.model.RewardId;
import io.zartra.bedwars.progression.model.RewardRecord;
import io.zartra.bedwars.progression.model.TransactionId;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** SQLite contract evidence for all eight M12 repository ports. */
final class JdbcProgressionRepositoriesTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");
    private static final PlayerProgressionId PLAYER = PlayerProgressionId.of(PlayerId.of(new UUID(0, 120)));
    private static final CurrencyId COINS = CurrencyId.of("zartra", "coins");
    @TempDir Path temporary;

    @Test void migrationIsChecksumLockedAndRepositoryStateSurvivesRestart() {
        final Path database = temporary.resolve("m12.db");
        final String checksum;
        try (JdbcStorageEngine engine = open(database)) {
            checksum = migrate(engine);
            final JdbcProgressionRepositories stores = new JdbcProgressionRepositories(5);
            try (UnitOfWork unit = write(engine)) {
                final ProgressionAccount account = account(ExperienceAmount.of(25), 0);
                assertEquals(1, stores.progressionAccounts().save(unit, account, RecordRevision.initial())
                        .requireValue().revision().value());
                assertEquals(1, stores.currencyAccounts().save(unit, currency(100, 0),
                        RecordRevision.initial()).requireValue().revision().value());
                assertTrue(stores.experienceLedger().append(unit, xp("xp-one")).isSuccess());
                assertTrue(stores.levelHistory().append(unit, PLAYER,
                        new LevelState(2, ExperienceAmount.of(25), NOW)).isSuccess());
                assertTrue(stores.prestigeHistory().append(unit, PLAYER,
                        new PrestigeState(1, NOW)).isSuccess());
                assertTrue(stores.economicTransactions().append(unit, ledger("credit-one", 100)).isSuccess());
                assertTrue(stores.rewards().register(unit, reward("reward-one")).isSuccess());
                assertTrue(stores.entitlements().grant(unit, entitlement("unlock-one")).isSuccess());
                unit.commit().requireValue();
            }
        }
        try (JdbcStorageEngine engine = open(database)) {
            assertEquals(checksum, migrate(engine));
            final JdbcProgressionRepositories stores = new JdbcProgressionRepositories(5);
            try (UnitOfWork unit = read(engine)) {
                assertEquals(25, stores.progressionAccounts().find(unit, PLAYER).requireValue().get().experience().value());
                assertEquals(100, stores.currencyAccounts().find(unit, PLAYER, COINS).requireValue().get().balance());
                assertEquals(1, stores.experienceLedger().history(unit, PLAYER, 10).requireValue().size());
                assertEquals(1, stores.levelHistory().history(unit, PLAYER, 10).requireValue().size());
                assertEquals(1, stores.prestigeHistory().history(unit, PLAYER, 10).requireValue().size());
                assertTrue(stores.economicTransactions().findByIdempotencyKey(unit, key("credit-one")).requireValue().isPresent());
                assertTrue(stores.rewards().findByIdempotencyKey(unit, key("reward-one")).requireValue().isPresent());
                assertEquals(Collections.singleton(EntitlementId.of("zartra", "builder")), stores.entitlements().findAll(unit, PLAYER).requireValue());
                unit.commit().requireValue();
            }
        }
    }

    @Test void rollbackOptimisticConflictAndDuplicateKeysFailClosed() {
        try (JdbcStorageEngine engine = open(temporary.resolve("safety.db"))) {
            migrate(engine);
            final JdbcProgressionRepositories stores = new JdbcProgressionRepositories(5);
            try (UnitOfWork unit = write(engine)) {
                stores.currencyAccounts().save(unit, currency(50, 0), RecordRevision.initial()).requireValue();
                stores.economicTransactions().append(unit, ledger("same", 50)).requireValue();
                unit.rollback();
            }
            try (UnitOfWork unit = read(engine)) {
                assertFalse(stores.currencyAccounts().find(unit, PLAYER, COINS).requireValue().isPresent());
                unit.commit();
            }
            try (UnitOfWork unit = write(engine)) {
                stores.currencyAccounts().save(unit, currency(50, 0), RecordRevision.initial()).requireValue();
                unit.commit();
            }
            try (UnitOfWork unit = write(engine)) {
                assertTrue(stores.currencyAccounts().save(unit, currency(60, 0), RecordRevision.initial()).isFailure());
                unit.rollback();
            }
            try (UnitOfWork unit = write(engine)) {
                stores.experienceLedger().append(unit, xp("duplicate")).requireValue();
                assertTrue(stores.experienceLedger().append(unit, xp("duplicate")).isFailure());
                unit.rollback();
            }
        }
    }

    @Test void transactionCoordinatorIsIdempotent() {
        try (JdbcStorageEngine engine = open(temporary.resolve("transactions.db"))) {
            migrate(engine);
            final JdbcProgressionTransactions transactions =
                    new JdbcProgressionTransactions(engine, 5);
            final MessageEnvelope source = envelope("source");
            final MessageEnvelope outbox = envelope("projection");
            assertTrue(transactions.projectExperience(source,
                    account(ExperienceAmount.of(25), 0), RecordRevision.initial(),
                    xp("project-xp"), java.util.Optional.of(reward("project-reward")),
                    outbox).requireValue());
            assertFalse(transactions.projectExperience(source,
                    account(ExperienceAmount.of(25), 0), RecordRevision.initial(),
                    xp("project-xp"), java.util.Optional.of(reward("project-reward")),
                    outbox).requireValue());
            final LedgerEntry credit = ledger("currency-credit", 100);
            assertEquals(credit.transactionId(), transactions.transactCurrency(currency(100, 0),
                    RecordRevision.initial(), credit).requireValue().transactionId());
            assertEquals(credit.transactionId(), transactions.transactCurrency(currency(999, 0),
                    RecordRevision.initial(), credit).requireValue().transactionId());
            final LedgerEntry purchase = ledger("purchase-ledger", 80);
            assertTrue(transactions.recordSettlement(key("settlement"), key("purchase"),
                    purchase).requireValue());
            assertFalse(transactions.recordSettlement(key("settlement-two"), key("purchase"),
                    purchase).requireValue());
        }
    }

    private static String migrate(final JdbcStorageEngine engine) {
        try (UnitOfWork unit = write(engine)) {
            final ProgressionSchemaMigrator.Report report = new ProgressionSchemaMigrator(5)
                    .migrate(((JdbcUnitOfWork) unit).connection()).requireValue();
            unit.commit().requireValue();
            return report.checksum();
        }
    }
    private static ProgressionAccount account(final ExperienceAmount xp, final long revision) {
        return new ProgressionAccount(PLAYER, xp, new LevelState(2, xp, NOW),
                new PrestigeState(1, NOW), Collections.emptySet(), RecordRevision.of(revision), audit());
    }
    private static CurrencyAccount currency(final long balance, final long revision) { return new CurrencyAccount(PLAYER, COINS, balance, RecordRevision.of(revision), audit());}
    private static ExperienceLedgerEntry xp(final String id) { return new ExperienceLedgerEntry(TransactionId.of("zartra", id), PLAYER, 25, ExperienceAmount.of(25), key(id), audit());}
    private static LedgerEntry ledger(final String id, final long balance) { return new LedgerEntry(TransactionId.of("zartra", id), PLAYER, COINS, balance, balance, key(id), audit());}
    private static RewardRecord reward(final String id) { return new RewardRecord(RewardId.of("zartra", "match"), PLAYER, key(id), audit());}
    private static EntitlementGrant entitlement(final String id) { return new EntitlementGrant(PLAYER, EntitlementId.of("zartra", "builder"), key(id), audit());}
    private static IdempotencyKey key(final String value) { return IdempotencyKey.of("test", value);}
    private static AuditMetadata audit() { return new AuditMetadata("test", CorrelationId.of(new UUID(0, 121)), NOW, NOW);}
    private static MessageEnvelope envelope(final String id) {
        return MessageEnvelope.of(key(id), EventMetadata.of(
                EventId.of(UUID.nameUUIDFromBytes(id.getBytes())),
                EventTypeId.of("test", "progression"), CorrelationId.of(new UUID(0, 122)),
                NOW, 1, 1, EventMetadata.ThreadContext.APPLICATION_WORKER),
                new byte[] {1}, NOW);
    }
    private static UnitOfWork write(final JdbcStorageEngine engine) { return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE, Duration.ofSeconds(5), 2)).requireValue();}
    private static UnitOfWork read(final JdbcStorageEngine engine) { return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_ONLY, Duration.ofSeconds(5), 2)).requireValue();}
    private static JdbcStorageEngine open(final Path database) { return JdbcStorageEngine.open(SqlStorageConfiguration.of(StorageEngine.EngineKind.SQLITE, "jdbc:sqlite:" + database.toAbsolutePath(), "", new char[0], 1, Duration.ofSeconds(5), Duration.ofSeconds(5))).requireValue();}
}
