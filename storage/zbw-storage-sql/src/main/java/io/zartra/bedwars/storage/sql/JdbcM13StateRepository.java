package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.achievement.AchievementProgress;
import io.zartra.bedwars.progression.challenge.ChallengeId;
import io.zartra.bedwars.progression.challenge.ChallengeProgress;
import io.zartra.bedwars.progression.model.AuditMetadata;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.objective.ObjectiveRuntimeState;
import io.zartra.bedwars.progression.pass.SeasonId;
import io.zartra.bedwars.progression.pass.SeasonProgress;
import io.zartra.bedwars.progression.quest.QuestAssignment;
import io.zartra.bedwars.progression.quest.QuestId;
import io.zartra.bedwars.progression.runtime.M13StateRepository;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Bounded prepared-statement implementation of the M13 transactional state port. */
public final class JdbcM13StateRepository implements M13StateRepository {
    private final int timeoutSeconds;

    /** Creates a repository with a positive query timeout. */
    public JdbcM13StateRepository(final int timeoutSeconds) {
        if (timeoutSeconds < 1) { throw new IllegalArgumentException("timeoutSeconds must be positive"); }
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override public Result<Boolean> claimEvent(final UnitOfWork unitOfWork,
                                                final IdempotencyKey key,
                                                final Instant occurredAt) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        try (PreparedStatement query = prepare(unit,
                "SELECT 1 FROM m13_event_claims WHERE idempotency_key=?")) {
            query.setString(1, key.toString());
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) { return Result.success(false); }
            }
            try (PreparedStatement insert = prepare(unit,
                    "INSERT INTO m13_event_claims(idempotency_key,occurred_at) VALUES(?,?)")) {
                insert.setString(1, key.toString());
                insert.setLong(2, occurredAt.toEpochMilli());
                insert.executeUpdate();
                return Result.success(true);
            }
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }

    @Override public Result<Optional<ObjectiveRuntimeState>> findObjective(final UnitOfWork unit,
            final PlayerProgressionId player, final ObjectiveId id) {
        return find(unit, "objective_progress", player, id.toString(), Codec::objective);
    }
    @Override public Result<ObjectiveRuntimeState> saveObjective(final UnitOfWork unit,
            final ObjectiveRuntimeState state, final long expected) {
        return save(unit, "objective_progress", state.playerId(), state.objectiveId().toString(),
                Codec.objective(state), state.revision(), state.audit().updatedAt(), expected, state);
    }
    @Override public Result<Optional<QuestAssignment>> findQuest(final UnitOfWork unit,
            final PlayerProgressionId player, final QuestId id) {
        return find(unit, "quest_state", player, id.toString(), Codec::quest);
    }
    @Override public Result<QuestAssignment> saveQuest(final UnitOfWork unit,
            final QuestAssignment state, final long expected) {
        return save(unit, "quest_state", state.playerId(), state.questId().toString(),
                Codec.quest(state), state.revision(), state.assignedAt(), expected, state);
    }
    @Override public Result<Optional<AchievementProgress>> findAchievement(final UnitOfWork unit,
            final PlayerProgressionId player, final AchievementId id) {
        return find(unit, "achievement_state", player, id.toString(), Codec::achievement);
    }
    @Override public Result<AchievementProgress> saveAchievement(final UnitOfWork unit,
            final AchievementProgress state, final long expected) {
        return save(unit, "achievement_state", state.playerId(), state.achievementId().toString(),
                Codec.achievement(state), state.revision(), state.updatedAt(), expected, state);
    }
    @Override public Result<Optional<ChallengeProgress>> findChallenge(final UnitOfWork unit,
            final PlayerProgressionId player, final ChallengeId id) {
        return find(unit, "challenge_state", player, id.toString(), Codec::challenge);
    }
    @Override public Result<ChallengeProgress> saveChallenge(final UnitOfWork unit,
            final ChallengeProgress state, final long expected) {
        return save(unit, "challenge_state", state.playerId(), state.challengeId().toString(),
                Codec.challenge(state), state.revision(), state.activatedAt(), expected, state);
    }
    @Override public Result<Optional<SeasonProgress>> findSeason(final UnitOfWork unit,
            final PlayerProgressionId player, final SeasonId id) {
        return find(unit, "season_progress", player, id.toString(), Codec::season);
    }
    @Override public Result<SeasonProgress> saveSeason(final UnitOfWork unit,
            final SeasonProgress state, final long expected) {
        return save(unit, "season_progress", state.playerId(), state.seasonId().toString(),
                Codec.season(state), state.revision(), state.updatedAt(), expected, state);
    }

    private <T> Result<Optional<T>> find(final UnitOfWork unitOfWork, final String table,
                                        final PlayerProgressionId player, final String definition,
                                        final Decoder<T> decoder) {
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, false);
        try (PreparedStatement statement = prepare(unit,
                "SELECT payload FROM " + table + " WHERE player_id=? AND definition_id=?")) {
            statement.setString(1, player.toString());
            statement.setString(2, definition);
            try (ResultSet result = statement.executeQuery()) {
                return Result.success(result.next()
                        ? Optional.of(decoder.decode(result.getBytes(1))) : Optional.empty());
            }
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }

    private <T> Result<T> save(final UnitOfWork unitOfWork, final String table,
                               final PlayerProgressionId player, final String definition,
                               final byte[] payload, final long revision, final Instant updatedAt,
                               final long expected, final T state) {
        if (expected < 0) { throw new IllegalArgumentException("expected revision must not be negative"); }
        final JdbcUnitOfWork unit = JdbcSupport.require(unitOfWork, true);
        try {
            final boolean exists = exists(unit, table, player, definition);
            final int changed;
            if (!exists) {
                if (expected != 0) { return Result.failure(SqlErrors.CONFLICT); }
                try (PreparedStatement statement = prepare(unit, "INSERT INTO " + table
                        + "(player_id,definition_id,payload,revision,updated_at) VALUES(?,?,?,?,?)")) {
                    bind(statement, player, definition, payload, revision, updatedAt);
                    changed = statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = prepare(unit, "UPDATE " + table
                        + " SET payload=?,revision=?,updated_at=?"
                        + " WHERE player_id=? AND definition_id=? AND revision=?")) {
                    statement.setBytes(1, payload);
                    statement.setLong(2, revision);
                    statement.setLong(3, updatedAt.toEpochMilli());
                    statement.setString(4, player.toString());
                    statement.setString(5, definition);
                    statement.setLong(6, expected);
                    changed = statement.executeUpdate();
                }
            }
            return changed == 1 ? Result.success(state) : Result.failure(SqlErrors.CONFLICT);
        } catch (SQLException failure) { return Result.failure(SqlErrors.classify(failure)); }
    }

    private boolean exists(final JdbcUnitOfWork unit, final String table,
                           final PlayerProgressionId player, final String definition)
            throws SQLException {
        try (PreparedStatement statement = prepare(unit,
                "SELECT 1 FROM " + table + " WHERE player_id=? AND definition_id=?")) {
            statement.setString(1, player.toString());
            statement.setString(2, definition);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }
    private PreparedStatement prepare(final JdbcUnitOfWork unit, final String sql)
            throws SQLException {
        final PreparedStatement statement = unit.connection().prepareStatement(sql);
        statement.setQueryTimeout(timeoutSeconds);
        return statement;
    }
    private static void bind(final PreparedStatement statement, final PlayerProgressionId player,
                             final String definition, final byte[] payload, final long revision,
                             final Instant updatedAt) throws SQLException {
        statement.setString(1, player.toString());
        statement.setString(2, definition);
        statement.setBytes(3, payload);
        statement.setLong(4, revision);
        statement.setLong(5, updatedAt.toEpochMilli());
    }

    private interface Decoder<T> { T decode(byte[] payload); }

    private static final class Codec {
        private Codec() { }
        static byte[] objective(final ObjectiveRuntimeState value) { return write(out -> {
            out.writeUTF(value.objectiveId().toString());
            player(out, value.playerId());
            out.writeInt(value.definitionVersion());
            out.writeLong(value.value());
            out.writeLong(value.completionCount());
            out.writeUTF(value.status().name());
            out.writeLong(value.revision());
            optionalKey(out, value.lastEvent());
            optionalInstant(out, value.expiresAt());
            audit(out, value.audit());
        }); }
        static ObjectiveRuntimeState objective(final byte[] payload) { return read(payload, in ->
                new ObjectiveRuntimeState(ObjectiveId.parse(in.readUTF()), player(in), in.readInt(),
                        in.readLong(), in.readLong(), ObjectiveRuntimeState.Status.valueOf(in.readUTF()),
                        in.readLong(), optionalKey(in), optionalInstant(in), audit(in))); }
        static byte[] quest(final QuestAssignment value) { return write(out -> {
            out.writeUTF(value.questId().toString());
            player(out, value.playerId());
            out.writeUTF(value.status().name());
            out.writeLong(value.assignedAt().toEpochMilli());
            out.writeLong(value.expiresAt().toEpochMilli());
            out.writeLong(value.revision());
        }); }
        static QuestAssignment quest(final byte[] payload) { return read(payload, in ->
                new QuestAssignment(QuestId.parse(in.readUTF()), player(in),
                        QuestAssignment.Status.valueOf(in.readUTF()),
                        Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong()),
                        in.readLong())); }
        static byte[] achievement(final AchievementProgress value) { return write(out -> {
            out.writeUTF(value.achievementId().toString());
            player(out, value.playerId());
            out.writeInt(value.definitionVersion());
            out.writeInt(value.tier());
            out.writeLong(value.value());
            out.writeBoolean(value.discovered());
            out.writeLong(value.revision());
            optionalKey(out, value.lastEvent());
            out.writeLong(value.updatedAt().toEpochMilli());
        }); }
        static AchievementProgress achievement(final byte[] payload) { return read(payload, in ->
                new AchievementProgress(AchievementId.parse(in.readUTF()), player(in), in.readInt(),
                        in.readInt(), in.readLong(), in.readBoolean(), in.readLong(), optionalKey(in),
                        Instant.ofEpochMilli(in.readLong()))); }
        static byte[] challenge(final ChallengeProgress value) { return write(out -> {
            out.writeUTF(value.challengeId().toString());
            player(out, value.playerId());
            out.writeInt(value.definitionVersion());
            out.writeUTF(value.status().name());
            out.writeLong(value.activatedAt().toEpochMilli());
            out.writeLong(value.expiresAt().toEpochMilli());
            out.writeLong(value.revision());
            optionalKey(out, value.lastEvent());
        }); }
        static ChallengeProgress challenge(final byte[] payload) { return read(payload, in ->
                new ChallengeProgress(ChallengeId.parse(in.readUTF()), player(in), in.readInt(),
                        ChallengeProgress.Status.valueOf(in.readUTF()),
                        Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong()),
                        in.readLong(), optionalKey(in))); }
        static byte[] season(final SeasonProgress value) { return write(out -> {
            out.writeUTF(value.seasonId().toString());
            player(out, value.playerId());
            out.writeInt(value.definitionVersion());
            out.writeLong(value.experience());
            out.writeInt(value.tier());
            out.writeInt(value.claimedFreeTiers().size());
            for (Integer tier : value.claimedFreeTiers()) { out.writeInt(tier); }
            out.writeLong(value.revision());
            optionalKey(out, value.lastEvent());
            out.writeLong(value.updatedAt().toEpochMilli());
        }); }
        static SeasonProgress season(final byte[] payload) { return read(payload, in -> {
            final SeasonId id = SeasonId.parse(in.readUTF());
            final PlayerProgressionId player = player(in);
            final int version = in.readInt();
            final long experience = in.readLong();
            final int tier = in.readInt();
            final int size = in.readInt();
            if (size < 0 || size > 10000) { throw new IllegalArgumentException("invalid claimed-tier count"); }
            final Set<Integer> claims = new LinkedHashSet<Integer>();
            for (int index = 0; index < size; index++) { claims.add(in.readInt()); }
            return new SeasonProgress(id, player, version, experience, tier, claims,
                    in.readLong(), optionalKey(in), Instant.ofEpochMilli(in.readLong()));
        }); }
        private static void player(final DataOutputStream out, final PlayerProgressionId value)
                throws IOException { out.writeUTF(value.toString()); }
        private static PlayerProgressionId player(final DataInputStream in) throws IOException {
            return PlayerProgressionId.of(PlayerId.parse(in.readUTF()));
        }
        private static void optionalKey(final DataOutputStream out,
                                        final Optional<IdempotencyKey> value) throws IOException {
            out.writeBoolean(value.isPresent());
            if (value.isPresent()) { out.writeUTF(value.get().toString()); }
        }
        private static Optional<IdempotencyKey> optionalKey(final DataInputStream in)
                throws IOException { return in.readBoolean() ? Optional.of(IdempotencyKey.parse(in.readUTF())) : Optional.empty(); }
        private static void optionalInstant(final DataOutputStream out,
                                            final Optional<Instant> value) throws IOException {
            out.writeBoolean(value.isPresent());
            if (value.isPresent()) { out.writeLong(value.get().toEpochMilli()); }
        }
        private static Optional<Instant> optionalInstant(final DataInputStream in)
                throws IOException { return in.readBoolean() ? Optional.of(Instant.ofEpochMilli(in.readLong())) : Optional.empty(); }
        private static void audit(final DataOutputStream out, final AuditMetadata value)
                throws IOException {
            out.writeUTF(value.actor());
            out.writeUTF(value.correlationId().toString());
            out.writeLong(value.createdAt().toEpochMilli());
            out.writeLong(value.updatedAt().toEpochMilli());
        }
        private static AuditMetadata audit(final DataInputStream in) throws IOException {
            return new AuditMetadata(in.readUTF(), CorrelationId.parse(in.readUTF()),
                    Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong()));
        }
        private static byte[] write(final Writer writer) {
            try {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DataOutputStream out = new DataOutputStream(bytes)) { writer.write(out); }
                return bytes.toByteArray();
            } catch (IOException failure) { throw new IllegalStateException("M13 encoding failed", failure); }
        }
        private static <T> T read(final byte[] payload, final Reader<T> reader) {
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
                final T value = reader.read(in);
                if (in.available() != 0) { throw new IllegalArgumentException("trailing M13 payload bytes"); }
                return value;
            } catch (IOException | RuntimeException failure) {
                throw new IllegalArgumentException("invalid M13 payload", failure);
            }
        }
        private interface Writer { void write(DataOutputStream out) throws IOException; }
        private interface Reader<T> { T read(DataInputStream in) throws IOException; }
    }
}
