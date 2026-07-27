package io.zartra.bedwars.replay.sql;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayEventRepository;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplayMetadataRepository;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.api.ReplayTimeline;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Prepared-statement, transaction-safe asynchronous replay repository. */
public final class JdbcReplayRepository implements ReplayMetadataRepository,
        ReplayEventRepository, ReplaySessionRepository {
    private final DataSource dataSource;
    private final Executor executor;
    private final int timeoutSeconds;

    /** Creates a repository whose blocking JDBC work runs only on the supplied executor. */
    public JdbcReplayRepository(final DataSource dataSource, final Executor executor,
                                final int timeoutSeconds) {
        if (timeoutSeconds < 1 || timeoutSeconds > 60) {
            throw new IllegalArgumentException("timeoutSeconds must be 1..60");
        }
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override public CompletionStage<Boolean> create(final ReplayMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        return async(connection -> {
            if (metadataExists(connection, metadata.replayId())) { return false; }
            insertMetadata(connection, metadata);
            return true;
        });
    }

    @Override public CompletionStage<Optional<ReplayMetadata>> findMetadata(final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        return async(connection -> loadMetadata(connection, replayId));
    }

    @Override public CompletionStage<Boolean> create(final ReplaySession session) {
        Objects.requireNonNull(session, "session");
        return async(connection -> {
            if (sessionExists(connection, session.metadata().replayId())) { return false; }
            if (!metadataExists(connection, session.metadata().replayId())) {
                insertMetadata(connection, session.metadata());
            }
            try (PreparedStatement statement = prepare(connection,
                    "INSERT INTO replay_sessions(replay_id,state,failure_reason) VALUES(?,?,?)")) {
                statement.setString(1, session.metadata().replayId().toString());
                statement.setString(2, session.state().name());
                statement.setString(3, session.failureReason().orElse(null));
                statement.executeUpdate();
            }
            if (!session.timeline().events().isEmpty()) {
                insertEvents(connection, session.metadata().replayId(), session.timeline().events());
            }
            return true;
        });
    }

    @Override public CompletionStage<SaveResult> save(final ReplaySession session,
                                                       final ReplayState expectedState) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(expectedState, "expectedState");
        return async(connection -> {
            final ReplayState persisted = loadState(connection, session.metadata().replayId());
            if (persisted == null) { return SaveResult.NOT_FOUND; }
            if (persisted != expectedState
                    || eventCount(connection, session.metadata().replayId())
                    != session.timeline().events().size()) {
                return SaveResult.CONFLICT;
            }
            try (PreparedStatement statement = prepare(connection,
                    "UPDATE replay_sessions SET state=?,failure_reason=? WHERE replay_id=? AND state=?")) {
                statement.setString(1, session.state().name());
                statement.setString(2, session.failureReason().orElse(null));
                statement.setString(3, session.metadata().replayId().toString());
                statement.setString(4, expectedState.name());
                return statement.executeUpdate() == 1 ? SaveResult.UPDATED : SaveResult.CONFLICT;
            }
        });
    }

    @Override public CompletionStage<Optional<ReplaySession>> findSession(final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        return async(connection -> {
            final Optional<ReplayMetadata> metadata = loadMetadata(connection, replayId);
            if (!metadata.isPresent()) { return Optional.empty(); }
            try (PreparedStatement statement = prepare(connection,
                    "SELECT state,failure_reason FROM replay_sessions WHERE replay_id=?")) {
                statement.setString(1, replayId.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) { return Optional.empty(); }
                    final ReplayState state;
                    try { state = ReplayState.valueOf(result.getString(1)); }
                    catch (IllegalArgumentException malformed) {
                        throw new ReplayPersistenceException("malformed replay session state", malformed);
                    }
                    return Optional.of(ReplaySession.restore(metadata.get(), state,
                            loadTimelineNow(connection, replayId), result.getString(2)));
                }
            }
        });
    }

    @Override public CompletionStage<AppendResult> append(final ReplayId replayId,
                                                           final ReplayEvent event) {
        Objects.requireNonNull(event, "event");
        return appendAll(replayId, Collections.singletonList(event));
    }

    @Override public CompletionStage<AppendResult> appendAll(final ReplayId replayId,
                                                              final List<ReplayEvent> events) {
        Objects.requireNonNull(replayId, "replayId");
        final List<ReplayEvent> copy = immutableEvents(events);
        if (copy.isEmpty()) {
            return CompletableFuture.completedFuture(AppendResult.CONFLICT);
        }
        return async(connection -> appendNow(connection, replayId, copy));
    }

    @Override public CompletionStage<ReplayTimeline> loadTimeline(final ReplayId replayId) {
        Objects.requireNonNull(replayId, "replayId");
        return async(connection -> loadTimelineNow(connection, replayId));
    }

    private AppendResult appendNow(final Connection connection, final ReplayId replayId,
                                   final List<ReplayEvent> events) throws SQLException {
        final ReplayState state = loadState(connection, replayId);
        if (state == null) { return AppendResult.NOT_FOUND; }
        if (state != ReplayState.RECORDING) { return AppendResult.CONFLICT; }
        long expected = eventCount(connection, replayId);
        int duplicates = 0;
        for (ReplayEvent event : events) {
            if (eventExists(connection, replayId, event.eventId())) {
                duplicates++;
            } else if (event.sequence() != expected++) {
                return AppendResult.CONFLICT;
            }
        }
        if (duplicates == events.size()) { return AppendResult.DUPLICATE; }
        if (duplicates != 0) { return AppendResult.CONFLICT; }
        insertEvents(connection, replayId, events);
        return AppendResult.INSERTED;
    }

    private void insertMetadata(final Connection connection, final ReplayMetadata metadata)
            throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "INSERT INTO replay_metadata(replay_id,match_id,created_at,format_version,protected_evidence) VALUES(?,?,?,?,?)")) {
            statement.setString(1, metadata.replayId().toString());
            statement.setString(2, metadata.matchId().toString());
            statement.setLong(3, metadata.createdAt().toEpochMilli());
            statement.setInt(4, metadata.formatVersion());
            statement.setInt(5, metadata.protectedEvidence() ? 1 : 0);
            statement.executeUpdate();
        }
        final Set<PlayerId> orderedParticipants = new TreeSet<PlayerId>((left, right) ->
                left.toString().compareTo(right.toString()));
        orderedParticipants.addAll(metadata.participants());
        for (PlayerId participant : orderedParticipants) {
            try (PreparedStatement statement = prepare(connection,
                    "INSERT INTO replay_participants(replay_id,player_id) VALUES(?,?)")) {
                statement.setString(1, metadata.replayId().toString());
                statement.setString(2, participant.toString());
                statement.executeUpdate();
            }
        }
    }

    private Optional<ReplayMetadata> loadMetadata(final Connection connection,
                                                   final ReplayId replayId) throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "SELECT match_id,created_at,format_version,protected_evidence FROM replay_metadata WHERE replay_id=?")) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) { return Optional.empty(); }
                final MatchId matchId = MatchId.parse(result.getString(1));
                final Instant createdAt = Instant.ofEpochMilli(result.getLong(2));
                final int formatVersion = result.getInt(3);
                final boolean protectedEvidence = result.getInt(4) != 0;
                return Optional.of(new ReplayMetadata(replayId, matchId, createdAt, formatVersion,
                        loadParticipants(connection, replayId), protectedEvidence));
            } catch (RuntimeException malformed) {
                throw new ReplayPersistenceException("malformed replay metadata row", malformed);
            }
        }
    }

    private Set<PlayerId> loadParticipants(final Connection connection, final ReplayId replayId)
            throws SQLException {
        final Set<PlayerId> participants = new TreeSet<PlayerId>((left, right) ->
                left.toString().compareTo(right.toString()));
        try (PreparedStatement statement = prepare(connection,
                "SELECT player_id FROM replay_participants WHERE replay_id=? ORDER BY player_id")) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) { participants.add(PlayerId.parse(result.getString(1))); }
            }
        }
        return participants;
    }

    private void insertEvents(final Connection connection, final ReplayId replayId,
                              final List<ReplayEvent> events) throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "INSERT INTO replay_events(replay_id,sequence_number,event_id,offset_millis,occurred_at,source,event_type,attributes) VALUES(?,?,?,?,?,?,?,?)")) {
            for (ReplayEvent event : events) {
                statement.setString(1, replayId.toString());
                statement.setLong(2, event.sequence());
                statement.setString(3, event.eventId());
                statement.setLong(4, event.offsetMillis());
                statement.setLong(5, event.occurredAt().toEpochMilli());
                statement.setString(6, event.source().name());
                statement.setString(7, event.type());
                statement.setString(8, encodeAttributes(event.attributes()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private ReplayTimeline loadTimelineNow(final Connection connection, final ReplayId replayId)
            throws SQLException {
        ReplayTimeline timeline = ReplayTimeline.empty();
        try (PreparedStatement statement = prepare(connection,
                "SELECT event_id,sequence_number,offset_millis,occurred_at,source,event_type,attributes FROM replay_events WHERE replay_id=? ORDER BY sequence_number")) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    try {
                        timeline = timeline.append(new ReplayEvent(result.getString(1), result.getLong(2),
                                result.getLong(3), Instant.ofEpochMilli(result.getLong(4)),
                                ReplayEvent.Source.valueOf(result.getString(5)), result.getString(6),
                                decodeAttributes(result.getString(7))));
                    } catch (RuntimeException malformed) {
                        throw new ReplayPersistenceException("malformed replay event row", malformed);
                    }
                }
            }
        }
        return timeline;
    }

    private static String encodeAttributes(final Map<String, String> attributes) {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<String, String>(attributes).entrySet()) {
            if (result.length() != 0) { result.append('\n'); }
            result.append(encoder.encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)))
                    .append(':').append(encoder.encodeToString(
                            entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }
        return result.toString();
    }

    private static Map<String, String> decodeAttributes(final String encoded) {
        final Map<String, String> result = new TreeMap<String, String>();
        if (encoded == null) { throw new ReplayPersistenceException("null replay attributes"); }
        if (encoded.isEmpty()) { return result; }
        final Base64.Decoder decoder = Base64.getUrlDecoder();
        for (String row : encoded.split("\\n", -1)) {
            final int separator = row.indexOf(':');
            if (separator < 1 || separator == row.length() - 1) {
                throw new ReplayPersistenceException("malformed replay attributes");
            }
            try {
                final String key = new String(decoder.decode(row.substring(0, separator)),
                        StandardCharsets.UTF_8);
                final String value = new String(decoder.decode(row.substring(separator + 1)),
                        StandardCharsets.UTF_8);
                if (result.put(key, value) != null) {
                    throw new ReplayPersistenceException("duplicate replay attribute");
                }
            } catch (IllegalArgumentException malformed) {
                throw new ReplayPersistenceException("malformed replay attribute encoding", malformed);
            }
        }
        return result;
    }

    private boolean metadataExists(final Connection connection, final ReplayId replayId)
            throws SQLException {
        return exists(connection, "SELECT 1 FROM replay_metadata WHERE replay_id=?", replayId);
    }

    private boolean sessionExists(final Connection connection, final ReplayId replayId)
            throws SQLException {
        return exists(connection, "SELECT 1 FROM replay_sessions WHERE replay_id=?", replayId);
    }

    private boolean eventExists(final Connection connection, final ReplayId replayId,
                                final String eventId) throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "SELECT 1 FROM replay_events WHERE replay_id=? AND event_id=?")) {
            statement.setString(1, replayId.toString());
            statement.setString(2, eventId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private boolean exists(final Connection connection, final String sql, final ReplayId replayId)
            throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql)) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private ReplayState loadState(final Connection connection, final ReplayId replayId)
            throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "SELECT state FROM replay_sessions WHERE replay_id=?")) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) { return null; }
                try { return ReplayState.valueOf(result.getString(1)); }
                catch (IllegalArgumentException malformed) {
                    throw new ReplayPersistenceException("malformed replay session state", malformed);
                }
            }
        }
    }

    private long eventCount(final Connection connection, final ReplayId replayId)
            throws SQLException {
        try (PreparedStatement statement = prepare(connection,
                "SELECT COUNT(*) FROM replay_events WHERE replay_id=?")) {
            statement.setString(1, replayId.toString());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private PreparedStatement prepare(final Connection connection, final String sql)
            throws SQLException {
        final PreparedStatement statement = connection.prepareStatement(sql);
        statement.setQueryTimeout(timeoutSeconds);
        return statement;
    }

    private <T> CompletionStage<T> async(final SqlOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    final T result = operation.execute(connection);
                    connection.commit();
                    return result;
                } catch (RuntimeException | SQLException failure) {
                    rollback(connection);
                    if (failure instanceof ReplayPersistenceException) {
                        throw (ReplayPersistenceException) failure;
                    }
                    throw new ReplayPersistenceException("replay persistence operation failed", failure);
                }
            } catch (SQLException failure) {
                throw new ReplayPersistenceException("replay persistence connection failed", failure);
            }
        }, executor);
    }

    private static List<ReplayEvent> immutableEvents(final List<ReplayEvent> events) {
        final List<ReplayEvent> copy = new ArrayList<ReplayEvent>(
                Objects.requireNonNull(events, "events"));
        if (copy.contains(null)) { throw new IllegalArgumentException("events cannot contain null"); }
        return Collections.unmodifiableList(copy);
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    /** JDBC operation executed inside one caller-invisible transaction. */
    private interface SqlOperation<T> { T execute(Connection connection) throws SQLException; }
}
