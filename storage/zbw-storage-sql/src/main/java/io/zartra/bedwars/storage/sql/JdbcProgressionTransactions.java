package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.model.CurrencyAccount;
import io.zartra.bedwars.progression.model.ExperienceLedgerEntry;
import io.zartra.bedwars.progression.model.LedgerEntry;
import io.zartra.bedwars.progression.model.ProgressionAccount;
import io.zartra.bedwars.progression.model.RewardRecord;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Explicit M12 transaction boundary for exactly-once projection, currency and settlement writes. */
public final class JdbcProgressionTransactions {
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final StorageEngine engine;
    private final JdbcProgressionRepositories repositories;

    /** Creates a transaction coordinator over one authoritative M04 engine. */
    public JdbcProgressionTransactions(final StorageEngine engine, final int queryTimeoutSeconds) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.repositories = new JdbcProgressionRepositories(queryTimeoutSeconds);
    }

    /**
     * Claims an M08 event and atomically updates progression, appends XP, registers an optional
     * reward intent, and enqueues its outbox projection. A duplicate returns {@code false}.
     */
    public Result<Boolean> projectExperience(final MessageEnvelope source,
            final ProgressionAccount account, final RecordRevision expectedRevision,
            final ExperienceLedgerEntry ledger, final Optional<RewardRecord> reward,
            final MessageEnvelope outbox) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reward, "reward");
        try (UnitOfWork unit = begin()) {
            if (!engine.messages().receive(unit, source).requireValue()) {
                unit.rollback();
                return Result.success(false);
            }
            repositories.progressionAccounts().save(unit, account, expectedRevision).requireValue();
            repositories.experienceLedger().append(unit, ledger).requireValue();
            if (reward.isPresent()) { repositories.rewards().register(unit, reward.get()).requireValue();}
            engine.messages().enqueue(unit, outbox).requireValue();
            unit.commit().requireValue();
            return Result.success(true);
        } catch (RuntimeException failure) { return Result.failure(SqlErrors.CONFLICT);}
    }

    /** Atomically mutates a persistent balance and appends its immutable ledger evidence. */
    public Result<LedgerEntry> transactCurrency(final CurrencyAccount account,
            final RecordRevision expectedRevision, final LedgerEntry ledger) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(ledger, "ledger");
        try (UnitOfWork unit = begin()) {
            final Optional<LedgerEntry> prior = repositories.economicTransactions()
                    .findByIdempotencyKey(unit, ledger.idempotencyKey()).requireValue();
            if (prior.isPresent()) { unit.rollback();
            return Result.success(prior.get());}
            repositories.currencyAccounts().save(unit, account, expectedRevision).requireValue();
            repositories.economicTransactions().append(unit, ledger).requireValue();
            unit.commit().requireValue();
            return Result.success(ledger);
        } catch (RuntimeException failure) { return Result.failure(SqlErrors.CONFLICT);}
    }

    /** Records an M11 purchase reference exactly once without recreating shop validation. */
    public Result<Boolean> recordSettlement(final IdempotencyKey settlementId,
            final IdempotencyKey purchaseReference, final LedgerEntry ledger) {
        Objects.requireNonNull(settlementId, "settlementId");
        Objects.requireNonNull(purchaseReference, "purchaseReference");
        Objects.requireNonNull(ledger, "ledger");
        try (UnitOfWork unit = begin()) {
            final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) unit;
            try (PreparedStatement find = jdbc.connection().prepareStatement(
                    "SELECT transaction_id FROM purchase_settlements WHERE purchase_reference=?")) {
                find.setQueryTimeout(5);
                find.setString(1, purchaseReference.toString());
                try (ResultSet row = find.executeQuery()) {
                    if (row.next()) { unit.rollback();
                    return Result.success(false);}
                }
            }
            repositories.economicTransactions().append(unit, ledger).requireValue();
            try (PreparedStatement insert = jdbc.connection().prepareStatement(
                    "INSERT INTO purchase_settlements(settlement_id,purchase_reference,transaction_id,state,created_at) VALUES(?,?,?,?,?)")) {
                insert.setQueryTimeout(5);
                insert.setString(1, settlementId.toString());
                insert.setString(2, purchaseReference.toString());
                insert.setString(3, ledger.transactionId().toString());
                insert.setString(4, "SETTLED");
                insert.setLong(5, Instant.now().toEpochMilli());
                insert.executeUpdate();
            }
            unit.commit().requireValue();
            return Result.success(true);
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure));}
        catch (RuntimeException failure) { return Result.failure(SqlErrors.CONFLICT);}
    }

    private UnitOfWork begin() {
        return engine.begin(TransactionOptions.of(TransactionOptions.AccessMode.READ_WRITE,
                TIMEOUT, 3)).requireValue();
    }
}
