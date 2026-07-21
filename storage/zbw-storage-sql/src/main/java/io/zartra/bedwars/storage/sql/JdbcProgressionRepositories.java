package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
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
import io.zartra.bedwars.progression.repository.CurrencyAccountRepository;
import io.zartra.bedwars.progression.repository.EconomicTransactionRepository;
import io.zartra.bedwars.progression.repository.EntitlementRepository;
import io.zartra.bedwars.progression.repository.ExperienceLedgerRepository;
import io.zartra.bedwars.progression.repository.LevelHistoryRepository;
import io.zartra.bedwars.progression.repository.PrestigeHistoryRepository;
import io.zartra.bedwars.progression.repository.ProgressionAccountRepository;
import io.zartra.bedwars.progression.repository.RewardRepository;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Factory for bounded JDBC implementations of every M12 repository port. */
public final class JdbcProgressionRepositories {
    private final Core core;
    /** Creates stateless adapters using the caller-owned M04 transaction. */
    public JdbcProgressionRepositories(final int queryTimeoutSeconds) { core = new Core(queryTimeoutSeconds);}
    /** @return progression aggregate adapter */ public ProgressionAccountRepository progressionAccounts() { return new Accounts(core);}
    /** @return append-only XP adapter */ public ExperienceLedgerRepository experienceLedger() { return new Xp(core);}
    /** @return level history adapter */ public LevelHistoryRepository levelHistory() { return new Levels(core);}
    /** @return prestige history adapter */ public PrestigeHistoryRepository prestigeHistory() { return new Prestiges(core);}
    /** @return persistent currency adapter */ public CurrencyAccountRepository currencyAccounts() { return new Currencies(core);}
    /** @return immutable economic ledger adapter */ public EconomicTransactionRepository economicTransactions() { return new Economy(core);}
    /** @return reward-registration adapter; delivery is deliberately absent in Phase 2 */ public RewardRepository rewards() { return new Rewards(core);}
    /** @return entitlement adapter */ public EntitlementRepository entitlements() { return new Entitlements(core);}

    private static final class Accounts implements ProgressionAccountRepository {
        private final Core core;
        Accounts(final Core core) { this.core = core;}
        @Override public Result<Optional<ProgressionAccount>> find(final UnitOfWork unit, final PlayerProgressionId id) {
            try (PreparedStatement sql = core.prepare(unit, "SELECT payload,revision FROM progression_accounts WHERE player_id=?")) {
                sql.setString(1, id.toString());
                try (ResultSet row = sql.executeQuery()) {
                    return Result.success(row.next() ? Optional.of(Codec.account(row.getBytes(1), row.getLong(2))) : Optional.empty());
                }
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<ProgressionAccount> save(final UnitOfWork unit, final ProgressionAccount value, final RecordRevision expected) {
            final long next = expected.next().value();
            try {
                final int changed;
                if (expected.value() == 0 && !core.exists(unit, "progression_accounts", "player_id", value.id().toString())) {
                    try (PreparedStatement sql = core.prepare(unit, "INSERT INTO progression_accounts(player_id,payload,revision) VALUES(?,?,?)")) {
                        sql.setString(1, value.id().toString());
                        sql.setBytes(2, Codec.account(value));
                        sql.setLong(3, next);
                        changed = sql.executeUpdate();
                    }
                } else {
                    try (PreparedStatement sql = core.prepare(unit, "UPDATE progression_accounts SET payload=?,revision=? WHERE player_id=? AND revision=?")) {
                        sql.setBytes(1, Codec.account(value));
                        sql.setLong(2, next);
                        sql.setString(3, value.id().toString());
                        sql.setLong(4, expected.value());
                        changed = sql.executeUpdate();
                    }
                }
                return changed == 1 ? Result.success(Codec.account(Codec.account(value), next)) : Result.failure(SqlErrors.CONFLICT);
            } catch (SQLException failure) { return core.failure(failure);}
        }
    }

    private static final class Xp implements ExperienceLedgerRepository {
        private final Core core;
        Xp(final Core core) { this.core = core;}
        @Override public Result<ExperienceLedgerEntry> append(final UnitOfWork unit, final ExperienceLedgerEntry value) {
            try (PreparedStatement sql = core.prepare(unit, "INSERT INTO progression_xp_ledger(transaction_id,player_id,idempotency_key,payload,created_at) VALUES(?,?,?,?,?)")) {
                sql.setString(1, value.transactionId().toString());
                sql.setString(2, value.owner().toString());
                sql.setString(3, value.idempotencyKey().toString());
                sql.setBytes(4, Codec.xp(value));
                sql.setLong(5, value.audit().createdAt().toEpochMilli());
                sql.executeUpdate();
                return Result.success(value);
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<Optional<ExperienceLedgerEntry>> findByIdempotencyKey(final UnitOfWork unit, final IdempotencyKey key) {
            return core.payload(unit, "SELECT payload FROM progression_xp_ledger WHERE idempotency_key=?", key.toString(), Codec::xp);
        }
        @Override public Result<List<ExperienceLedgerEntry>> history(final UnitOfWork unit, final PlayerProgressionId owner, final int limit) {
            return core.payloads(unit, "SELECT payload FROM progression_xp_ledger WHERE player_id=? ORDER BY created_at,transaction_id LIMIT ?", owner.toString(), limit, Codec::xp);
        }
    }

    private static final class Levels implements LevelHistoryRepository {
        private final Core core;
        Levels(final Core core) { this.core = core;}
        @Override public Result<LevelState> append(final UnitOfWork unit, final PlayerProgressionId owner, final LevelState value) {
            return core.appendHistory(unit, "progression_level_history", owner, value.attainedAt(), value.level(), Codec.level(value), value);
        }
        @Override public Result<List<LevelState>> history(final UnitOfWork unit, final PlayerProgressionId owner, final int limit) {
            return core.payloads(unit, "SELECT payload FROM progression_level_history WHERE player_id=? ORDER BY attained_at,history_id LIMIT ?", owner.toString(), limit, Codec::level);
        }
    }

    private static final class Prestiges implements PrestigeHistoryRepository {
        private final Core core;
        Prestiges(final Core core) { this.core = core;}
        @Override public Result<PrestigeState> append(final UnitOfWork unit, final PlayerProgressionId owner, final PrestigeState value) {
            return core.appendHistory(unit, "progression_prestige_history", owner, value.attainedAt(), value.prestige(), Codec.prestige(value), value);
        }
        @Override public Result<List<PrestigeState>> history(final UnitOfWork unit, final PlayerProgressionId owner, final int limit) {
            return core.payloads(unit, "SELECT payload FROM progression_prestige_history WHERE player_id=? ORDER BY attained_at,history_id LIMIT ?", owner.toString(), limit, Codec::prestige);
        }
    }

    private static final class Currencies implements CurrencyAccountRepository {
        private final Core core;
        Currencies(final Core core) { this.core = core;}
        @Override public Result<Optional<CurrencyAccount>> find(final UnitOfWork unit, final PlayerProgressionId owner, final CurrencyId currency) {
            try (PreparedStatement sql = core.prepare(unit, "SELECT payload,revision FROM currency_accounts WHERE player_id=? AND currency_id=?")) {
                sql.setString(1, owner.toString());
                sql.setString(2, currency.toString());
                try (ResultSet row = sql.executeQuery()) {
                    return Result.success(row.next() ? Optional.of(Codec.currency(row.getBytes(1), row.getLong(2))) : Optional.empty());
                }
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<CurrencyAccount> save(final UnitOfWork unit, final CurrencyAccount value, final RecordRevision expected) {
            final long next = expected.next().value();
            try {
                final int changed;
                if (expected.value() == 0 && !core.currencyExists(unit, value)) {
                    try (PreparedStatement sql = core.prepare(unit, "INSERT INTO currency_accounts(player_id,currency_id,payload,revision) VALUES(?,?,?,?)")) {
                        sql.setString(1, value.owner().toString());
                        sql.setString(2, value.currencyId().toString());
                        sql.setBytes(3, Codec.currency(value));
                        sql.setLong(4, next);
                        changed = sql.executeUpdate();
                    }
                } else {
                    try (PreparedStatement sql = core.prepare(unit, "UPDATE currency_accounts SET payload=?,revision=? WHERE player_id=? AND currency_id=? AND revision=?")) {
                        sql.setBytes(1, Codec.currency(value));
                        sql.setLong(2, next);
                        sql.setString(3, value.owner().toString());
                        sql.setString(4, value.currencyId().toString());
                        sql.setLong(5, expected.value());
                        changed = sql.executeUpdate();
                    }
                }
                return changed == 1 ? Result.success(Codec.currency(Codec.currency(value), next)) : Result.failure(SqlErrors.CONFLICT);
            } catch (SQLException failure) { return core.failure(failure);}
        }
    }

    private static final class Economy implements EconomicTransactionRepository {
        private final Core core;
        Economy(final Core core) { this.core = core;}
        @Override public Result<LedgerEntry> append(final UnitOfWork unit, final LedgerEntry value) {
            try {
                try (PreparedStatement sql = core.prepare(unit, "INSERT INTO economic_transactions(transaction_id,idempotency_key,payload,created_at) VALUES(?,?,?,?)")) {
                    sql.setString(1, value.transactionId().toString());
                    sql.setString(2, value.idempotencyKey().toString());
                    sql.setBytes(3, Codec.ledger(value));
                    sql.setLong(4, value.audit().createdAt().toEpochMilli());
                    sql.executeUpdate();
                }
                try (PreparedStatement sql = core.prepare(unit, "INSERT INTO economic_transaction_entries(transaction_id,player_id,currency_id,delta,resulting_balance) VALUES(?,?,?,?,?)")) {
                    sql.setString(1, value.transactionId().toString());
                    sql.setString(2, value.owner().toString());
                    sql.setString(3, value.currencyId().toString());
                    sql.setLong(4, value.delta());
                    sql.setLong(5, value.resultingBalance());
                    sql.executeUpdate();
                }
                return Result.success(value);
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<Optional<LedgerEntry>> find(final UnitOfWork unit, final TransactionId id) { return core.payload(unit, "SELECT payload FROM economic_transactions WHERE transaction_id=?", id.toString(), Codec::ledger);}
        @Override public Result<Optional<LedgerEntry>> findByIdempotencyKey(final UnitOfWork unit, final IdempotencyKey key) { return core.payload(unit, "SELECT payload FROM economic_transactions WHERE idempotency_key=?", key.toString(), Codec::ledger);}
    }

    private static final class Rewards implements RewardRepository {
        private final Core core;
        Rewards(final Core core) { this.core = core;}
        @Override public Result<RewardRecord> register(final UnitOfWork unit, final RewardRecord value) {
            try (PreparedStatement sql = core.prepare(unit, "INSERT INTO reward_grants(idempotency_key,reward_id,player_id,payload) VALUES(?,?,?,?)")) {
                sql.setString(1, value.idempotencyKey().toString());
                sql.setString(2, value.rewardId().toString());
                sql.setString(3, value.recipient().toString());
                sql.setBytes(4, Codec.reward(value));
                sql.executeUpdate();
                return Result.success(value);
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<Optional<RewardRecord>> findByIdempotencyKey(final UnitOfWork unit, final IdempotencyKey key) { return core.payload(unit, "SELECT payload FROM reward_grants WHERE idempotency_key=?", key.toString(), Codec::reward);}
    }

    private static final class Entitlements implements EntitlementRepository {
        private final Core core;
        Entitlements(final Core core) { this.core = core;}
        @Override public Result<EntitlementGrant> grant(final UnitOfWork unit, final EntitlementGrant value) {
            try (PreparedStatement sql = core.prepare(unit, "INSERT INTO progression_unlocks(player_id,entitlement_id,idempotency_key,payload) VALUES(?,?,?,?)")) {
                sql.setString(1, value.owner().toString());
                sql.setString(2, value.entitlementId().toString());
                sql.setString(3, value.idempotencyKey().toString());
                sql.setBytes(4, Codec.entitlement(value));
                sql.executeUpdate();
                return Result.success(value);
            } catch (SQLException failure) { return core.failure(failure);}
        }
        @Override public Result<Optional<EntitlementGrant>> findByIdempotencyKey(final UnitOfWork unit, final IdempotencyKey key) { return core.payload(unit, "SELECT payload FROM progression_unlocks WHERE idempotency_key=?", key.toString(), Codec::entitlement);}
        @Override public Result<Set<EntitlementId>> findAll(final UnitOfWork unit, final PlayerProgressionId owner) {
            final Set<EntitlementId> values = new LinkedHashSet<EntitlementId>();
            try (PreparedStatement sql = core.prepare(unit, "SELECT entitlement_id FROM progression_unlocks WHERE player_id=? ORDER BY entitlement_id")) {
                sql.setString(1, owner.toString());
                try (ResultSet row = sql.executeQuery()) { while (row.next()) { values.add(EntitlementId.parse(row.getString(1)));} }
                return Result.success(Collections.unmodifiableSet(values));
            } catch (SQLException failure) { return core.failure(failure);}
        }
    }

    private static final class Core {
        private final int timeout;
        Core(final int timeout) {
            if (timeout < 1) {
                throw new IllegalArgumentException("queryTimeoutSeconds must be positive");
            }
            this.timeout = timeout;
        }
        PreparedStatement prepare(final UnitOfWork unit, final String sql) throws SQLException { final PreparedStatement value = connection(unit).prepareStatement(sql);
        value.setQueryTimeout(timeout);
        return value;}
        <T> Result<T> failure(final SQLException failure) { return Result.failure(SqlErrors.classify(failure));}
        boolean exists(final UnitOfWork unit, final String table, final String column, final String value) throws SQLException {
            try (PreparedStatement sql = prepare(unit, "SELECT 1 FROM " + table + " WHERE " + column + "=?")) { sql.setString(1, value);
            try (ResultSet row = sql.executeQuery()) { return row.next();} }
        }
        boolean currencyExists(final UnitOfWork unit, final CurrencyAccount value) throws SQLException {
            try (PreparedStatement sql = prepare(unit, "SELECT 1 FROM currency_accounts WHERE player_id=? AND currency_id=?")) { sql.setString(1, value.owner().toString());
            sql.setString(2, value.currencyId().toString());
            try (ResultSet row = sql.executeQuery()) { return row.next();} }
        }
        <T> Result<T> appendHistory(final UnitOfWork unit, final String table, final PlayerProgressionId owner, final Instant time, final int discriminator, final byte[] payload, final T value) {
            try (PreparedStatement sql = prepare(unit, "INSERT INTO " + table + "(history_id,player_id,payload,attained_at) VALUES(?,?,?,?)")) { sql.setString(1, owner + ":" + time.toEpochMilli() + ":" + discriminator);
            sql.setString(2, owner.toString());
            sql.setBytes(3, payload);
            sql.setLong(4, time.toEpochMilli());
            sql.executeUpdate();
            return Result.success(value);
            } catch (SQLException failure) { return failure(failure);}
        }
        <T> Result<Optional<T>> payload(final UnitOfWork unit, final String query, final String key, final Decoder<T> decoder) {
            try (PreparedStatement sql = prepare(unit, query)) { sql.setString(1, key);
            try (ResultSet row = sql.executeQuery()) {
                return Result.success(row.next()
                        ? Optional.of(decoder.read(row.getBytes(1))) : Optional.empty());
            }
            } catch (SQLException failure) { return failure(failure);}
        }
        <T> Result<List<T>> payloads(final UnitOfWork unit, final String query, final String key, final int limit, final Decoder<T> decoder) {
            if (limit < 1 || limit > 1000) { throw new IllegalArgumentException("limit must be between 1 and 1000");}
            final List<T> values = new ArrayList<T>();
            try (PreparedStatement sql = prepare(unit, query)) { sql.setString(1, key);
            sql.setInt(2, limit);
            try (ResultSet row = sql.executeQuery()) {
                while (row.next()) { values.add(decoder.read(row.getBytes(1)));}
            }
            return Result.success(Collections.unmodifiableList(values));
            } catch (SQLException failure) { return failure(failure);}
        }
        private static Connection connection(final UnitOfWork unit) {
            if (!(unit instanceof JdbcUnitOfWork)) {
                throw new IllegalArgumentException("unitOfWork must be JDBC-backed");
            }
            return ((JdbcUnitOfWork) unit).connection();
        }
    }

    private interface Decoder<T> { T read(byte[] bytes);}
    private interface Writer { void write(DataOutputStream output) throws IOException;}
    private interface Reader<T> { T read(DataInputStream input) throws IOException;}
    private static final class Codec {
        private Codec() { }
        static byte[] account(final ProgressionAccount value) { return write(out -> { player(out, value.id());
        out.writeLong(value.experience().value());
        level(out, value.level());
        prestige(out, value.prestige());
        out.writeInt(value.entitlements().size());
        for (EntitlementId id : value.entitlements()) { out.writeUTF(id.toString());}
        audit(out, value.audit());});}
        static ProgressionAccount account(final byte[] bytes, final long revision) { return read(bytes, in -> { final PlayerProgressionId id = player(in);
        final ExperienceAmount xp = ExperienceAmount.of(in.readLong());
        final LevelState level = level(in);
        final PrestigeState prestige = prestige(in);
        final int count = bounded(in.readInt(), 0, 4096);
        final Set<EntitlementId> unlocks = new LinkedHashSet<EntitlementId>();
        for (int index = 0; index < count; index++) { unlocks.add(EntitlementId.parse(in.readUTF()));}
        return new ProgressionAccount(id, xp, level, prestige, unlocks,
                RecordRevision.of(revision), audit(in));});}
        static byte[] currency(final CurrencyAccount value) { return write(out -> { player(out, value.owner());
        out.writeUTF(value.currencyId().toString());
        out.writeLong(value.balance());
        audit(out, value.audit());});}
        static CurrencyAccount currency(final byte[] bytes, final long revision) { return read(bytes, in -> new CurrencyAccount(player(in), CurrencyId.parse(in.readUTF()), in.readLong(), RecordRevision.of(revision), audit(in)));}
        static byte[] xp(final ExperienceLedgerEntry value) { return write(out -> { out.writeUTF(value.transactionId().toString());
        player(out, value.owner());
        out.writeLong(value.delta());
        out.writeLong(value.resultingExperience().value());
        out.writeUTF(value.idempotencyKey().toString());
        audit(out, value.audit());});}
        static ExperienceLedgerEntry xp(final byte[] bytes) { return read(bytes, in -> new ExperienceLedgerEntry(TransactionId.parse(in.readUTF()), player(in), in.readLong(), ExperienceAmount.of(in.readLong()), IdempotencyKey.parse(in.readUTF()), audit(in)));}
        static byte[] ledger(final LedgerEntry value) { return write(out -> { out.writeUTF(value.transactionId().toString());
        player(out, value.owner());
        out.writeUTF(value.currencyId().toString());
        out.writeLong(value.delta());
        out.writeLong(value.resultingBalance());
        out.writeUTF(value.idempotencyKey().toString());
        audit(out, value.audit());});}
        static LedgerEntry ledger(final byte[] bytes) { return read(bytes, in -> new LedgerEntry(TransactionId.parse(in.readUTF()), player(in), CurrencyId.parse(in.readUTF()), in.readLong(), in.readLong(), IdempotencyKey.parse(in.readUTF()), audit(in)));}
        static byte[] reward(final RewardRecord value) { return write(out -> { out.writeUTF(value.rewardId().toString());
        player(out, value.recipient());
        out.writeUTF(value.idempotencyKey().toString());
        audit(out, value.audit());});}
        static RewardRecord reward(final byte[] bytes) { return read(bytes, in -> new RewardRecord(RewardId.parse(in.readUTF()), player(in), IdempotencyKey.parse(in.readUTF()), audit(in)));}
        static byte[] entitlement(final EntitlementGrant value) { return write(out -> { player(out, value.owner());
        out.writeUTF(value.entitlementId().toString());
        out.writeUTF(value.idempotencyKey().toString());
        audit(out, value.audit());});}
        static EntitlementGrant entitlement(final byte[] bytes) { return read(bytes, in -> new EntitlementGrant(player(in), EntitlementId.parse(in.readUTF()), IdempotencyKey.parse(in.readUTF()), audit(in)));}
        static byte[] level(final LevelState value) { return write(out -> level(out, value));}
        static LevelState level(final byte[] bytes) { return read(bytes, Codec::level);}
        static byte[] prestige(final PrestigeState value) { return write(out -> prestige(out, value));}
        static PrestigeState prestige(final byte[] bytes) { return read(bytes, Codec::prestige);}
        private static void player(final DataOutputStream out, final PlayerProgressionId value) throws IOException { out.writeUTF(value.toString());}
        private static PlayerProgressionId player(final DataInputStream in) throws IOException { return PlayerProgressionId.of(PlayerId.parse(in.readUTF()));}
        private static void level(final DataOutputStream out, final LevelState value) throws IOException { out.writeInt(value.level());
        out.writeLong(value.experience().value());
        out.writeLong(value.attainedAt().toEpochMilli());}
        private static LevelState level(final DataInputStream in) throws IOException { return new LevelState(in.readInt(), ExperienceAmount.of(in.readLong()), Instant.ofEpochMilli(in.readLong()));}
        private static void prestige(final DataOutputStream out, final PrestigeState value) throws IOException { out.writeInt(value.prestige());
        out.writeLong(value.attainedAt().toEpochMilli());}
        private static PrestigeState prestige(final DataInputStream in) throws IOException { return new PrestigeState(in.readInt(), Instant.ofEpochMilli(in.readLong()));}
        private static void audit(final DataOutputStream out, final AuditMetadata value) throws IOException { out.writeUTF(value.actor());
        out.writeUTF(value.correlationId().toString());
        out.writeLong(value.createdAt().toEpochMilli());
        out.writeLong(value.updatedAt().toEpochMilli());}
        private static AuditMetadata audit(final DataInputStream in) throws IOException { return new AuditMetadata(in.readUTF(), CorrelationId.parse(in.readUTF()), Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong()));}
        private static byte[] write(final Writer writer) { try { final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) { writer.write(out);}
        return bytes.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("progression encoding failed", failure);
        } }
        private static <T> T read(final byte[] bytes, final Reader<T> reader) { try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) { final T value = reader.read(in);
        if (in.available() != 0) { throw new IllegalArgumentException("trailing progression bytes");}
        return value;
        } catch (IOException | RuntimeException failure) {
            throw new IllegalArgumentException("invalid progression payload", failure);
        } }
        private static int bounded(final int value, final int minimum, final int maximum) {
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException("collection size out of range");
            }
            return value;
        }
    }
}
