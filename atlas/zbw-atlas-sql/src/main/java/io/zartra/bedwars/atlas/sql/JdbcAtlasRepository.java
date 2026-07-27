package io.zartra.bedwars.atlas.sql;

import io.zartra.bedwars.atlas.api.AtlasCase;
import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasCaseMetadata;
import io.zartra.bedwars.atlas.api.AtlasCaseRepository;
import io.zartra.bedwars.atlas.api.AtlasCaseSource;
import io.zartra.bedwars.atlas.api.AtlasCaseStatus;
import io.zartra.bedwars.atlas.api.AtlasEvidenceId;
import io.zartra.bedwars.atlas.api.AtlasEvidenceReference;
import io.zartra.bedwars.atlas.api.AtlasReview;
import io.zartra.bedwars.atlas.api.AtlasReviewId;
import io.zartra.bedwars.atlas.api.AtlasReviewRepository;
import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import io.zartra.bedwars.atlas.api.ReviewDecision;
import io.zartra.bedwars.atlas.api.ReviewReason;
import io.zartra.bedwars.atlas.api.ReviewVerdict;
import io.zartra.bedwars.atlas.core.AtlasAuditRecord;
import io.zartra.bedwars.replay.api.ReplayId;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/** Prepared-statement asynchronous Atlas repositories and supporting aggregate stores. */
public final class JdbcAtlasRepository implements AtlasCaseRepository, AtlasReviewRepository {
    private final DataSource source;
    private final Executor executor;

    public JdbcAtlasRepository(final DataSource source, final Executor executor) {
        this.source = Objects.requireNonNull(source, "source");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override public CompletionStage<Boolean> create(final AtlasCase value) {
        return transaction(connection -> {
            insertCase(connection, value);
            insertEvidence(connection, value);
            return true;
        }, false);
    }

    @Override public CompletionStage<SaveResult> save(final AtlasCase value,
                                                       final long expectedRevision) {
        return transaction(connection -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE atlas_cases SET status=?,revision=? WHERE case_id=? AND revision=?")) {
                update.setString(1, value.status().name());
                update.setLong(2, value.revision());
                update.setString(3, value.caseId().toString());
                update.setLong(4, expectedRevision);
                return update.executeUpdate() == 1 ? SaveResult.UPDATED : SaveResult.CONFLICT;
            }
        }, SaveResult.CONFLICT);
    }

    @Override public CompletionStage<Optional<AtlasCase>> find(final AtlasCaseId id) {
        return transaction(connection -> loadCase(connection, id), Optional.<AtlasCase>empty());
    }

    @Override public CompletionStage<Boolean> append(final AtlasReview review) {
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO atlas_reviews VALUES(?,?,?,?,?,?,?,?,?)")) {
                statement.setString(1, review.reviewId().toString());
                statement.setString(2, review.caseId().toString());
                statement.setString(3, review.reviewerId().toString());
                statement.setString(4, review.decision().name());
                statement.setString(5, review.verdict().name());
                statement.setString(6, review.reason().name());
                statement.setLong(7, review.submittedAt().toEpochMilli());
                statement.setLong(8, review.interactionMillis());
                statement.setInt(9, review.schemaVersion());
                statement.executeUpdate();
                return true;
            }
        }, false);
    }

    @Override public CompletionStage<Optional<AtlasReview>> find(final AtlasReviewId id) {
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM atlas_reviews WHERE review_id=?")) {
                statement.setString(1, id.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(readReview(rows)) : Optional.empty();
                }
            }
        }, Optional.<AtlasReview>empty());
    }

    @Override public CompletionStage<List<AtlasReview>> findByCase(final AtlasCaseId id) {
        return transaction(connection -> {
            List<AtlasReview> values = new ArrayList<AtlasReview>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM atlas_reviews WHERE case_id=? ORDER BY submitted_at,review_id")) {
                statement.setString(1, id.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) { values.add(readReview(rows)); }
                }
            }
            return values;
        }, new ArrayList<AtlasReview>());
    }

    /** Atomically claims a case reservation. */
    public CompletionStage<Boolean> reserve(final AtlasCaseId caseId,
                                            final AtlasReviewerId reviewerId,
                                            final Instant expiresAt) {
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO atlas_reservations VALUES(?,?,?)")) {
                statement.setString(1, caseId.toString());
                statement.setString(2, reviewerId.toString());
                statement.setLong(3, expiresAt.toEpochMilli());
                statement.executeUpdate();
                return true;
            }
        }, false);
    }

    /** Upserts bounded reviewer/reputation projections owned by Atlas. */
    public CompletionStage<Void> saveProfile(final ReviewerProfile value) {
        return transaction(connection -> {
            try (PreparedStatement remove = connection.prepareStatement(
                    "DELETE FROM atlas_reviewer_profiles WHERE reviewer_id=?")) {
                remove.setString(1, value.reviewerId().toString());
                remove.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO atlas_reviewer_profiles VALUES(?,?,?,?,?,?,?)")) {
                insert.setString(1, value.reviewerId().toString());
                insert.setLong(2, value.lifetimeReviews());
                insert.setLong(3, value.recentReviews());
                insert.setDouble(4, value.accuracy());
                insert.setDouble(5, value.confidence());
                insert.setDouble(6, value.reputation());
                insert.setLong(7, value.updatedAt().toEpochMilli());
                insert.executeUpdate();
            }
            return null;
        }, null);
    }

    /** Appends one deterministic audit sequence. */
    public CompletionStage<Boolean> appendAudit(final long sequence, final AtlasAuditRecord value) {
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO atlas_audit VALUES(?,?,?,?,?,?,?,?)")) {
                statement.setLong(1, sequence);
                statement.setString(2, value.actor());
                statement.setString(3, value.action());
                statement.setString(4, value.target());
                statement.setLong(5, value.occurredAt().toEpochMilli());
                statement.setString(6, value.result());
                statement.setString(7, value.beforeReference());
                statement.setString(8, value.afterReference());
                statement.executeUpdate();
                return true;
            }
        }, false);
    }

    /** Stores encrypted identity separately from every public/community table. */
    public CompletionStage<Boolean> storeIdentity(final AtlasCaseId caseId, final byte[] encrypted) {
        if (encrypted == null || encrypted.length < 16 || encrypted.length > 4096) {
            throw new IllegalArgumentException("encrypted identity must be 16..4096 bytes");
        }
        byte[] copy = Arrays.copyOf(encrypted, encrypted.length);
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO atlas_identity_vault VALUES(?,?)")) {
                statement.setString(1, caseId.toString());
                statement.setBytes(2, copy);
                statement.executeUpdate();
                return true;
            }
        }, false);
    }

    /** Loads encrypted vault content without joining public/community projections. */
    public CompletionStage<Optional<byte[]>> findIdentity(final AtlasCaseId caseId) {
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT encrypted_identity FROM atlas_identity_vault WHERE case_id=?")) {
                statement.setString(1, caseId.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(rows.getBytes(1)) : Optional.empty();
                }
            }
        }, Optional.<byte[]>empty());
    }

    private void insertCase(final Connection connection, final AtlasCase value) throws SQLException {
        AtlasCaseMetadata metadata = value.metadata();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO atlas_cases VALUES(?,?,?,?,?,?,?,?,?)")) {
            statement.setString(1, value.caseId().toString());
            statement.setString(2, value.status().name());
            statement.setString(3, metadata.source().name());
            statement.setLong(4, metadata.createdAt().toEpochMilli());
            statement.setString(5, metadata.category());
            statement.setString(6, metadata.sourceReference());
            statement.setInt(7, metadata.priority());
            statement.setInt(8, metadata.schemaVersion());
            statement.setLong(9, value.revision());
            statement.executeUpdate();
        }
    }

    private void insertEvidence(final Connection connection, final AtlasCase value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO atlas_evidence VALUES(?,?,?,?,?,?,?)")) {
            for (AtlasEvidenceReference evidence : value.evidence()) {
                statement.setString(1, value.caseId().toString());
                statement.setString(2, evidence.evidenceId().toString());
                statement.setString(3, evidence.type().name());
                statement.setString(4, evidence.replayId().map(Object::toString).orElse(null));
                statement.setString(5, evidence.externalReference().orElse(null));
                statement.setLong(6, evidence.startMillis());
                statement.setLong(7, evidence.endMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Optional<AtlasCase> loadCase(final Connection connection,
                                         final AtlasCaseId id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM atlas_cases WHERE case_id=?")) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) { return Optional.empty(); }
                AtlasCaseMetadata metadata = new AtlasCaseMetadata(
                        parse(AtlasCaseSource.class, rows.getString("source")),
                        Instant.ofEpochMilli(rows.getLong("created_at")),
                        rows.getString("category"), rows.getString("source_ref"),
                        rows.getInt("priority"), rows.getInt("schema_version"));
                return Optional.of(new AtlasCase(id,
                        parse(AtlasCaseStatus.class, rows.getString("status")),
                        metadata, loadEvidence(connection, id), rows.getLong("revision")));
            }
        }
    }

    private List<AtlasEvidenceReference> loadEvidence(final Connection connection,
                                                      final AtlasCaseId id) throws SQLException {
        List<AtlasEvidenceReference> values = new ArrayList<AtlasEvidenceReference>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM atlas_evidence WHERE case_id=? ORDER BY rowid")) {
            statement.setString(1, id.toString());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    AtlasEvidenceId evidenceId = AtlasEvidenceId.parse(rows.getString("evidence_id"));
                    AtlasEvidenceReference.Type type =
                            parse(AtlasEvidenceReference.Type.class, rows.getString("type"));
                    if (type == AtlasEvidenceReference.Type.REPLAY_SEGMENT) {
                        values.add(AtlasEvidenceReference.replay(evidenceId,
                                ReplayId.parse(rows.getString("replay_id")),
                                rows.getLong("start_ms"), rows.getLong("end_ms")));
                    } else if (type == AtlasEvidenceReference.Type.REPORT) {
                        values.add(AtlasEvidenceReference.report(
                                evidenceId, rows.getString("external_ref")));
                    } else {
                        values.add(AtlasEvidenceReference.internalSignal(
                                evidenceId, rows.getString("external_ref")));
                    }
                }
            }
        }
        return values;
    }

    private static AtlasReview readReview(final ResultSet rows) throws SQLException {
        return new AtlasReview(AtlasReviewId.parse(rows.getString("review_id")),
                AtlasCaseId.parse(rows.getString("case_id")),
                AtlasReviewerId.parse(rows.getString("reviewer_id")),
                parse(ReviewDecision.class, rows.getString("decision")),
                parse(ReviewVerdict.class, rows.getString("verdict")),
                parse(ReviewReason.class, rows.getString("reason")),
                Instant.ofEpochMilli(rows.getLong("submitted_at")),
                rows.getLong("interaction_ms"), rows.getInt("schema_version"));
    }

    private <T> CompletionStage<T> transaction(final Work<T> work, final T duplicate) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = source.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    T result = work.run(connection);
                    connection.commit();
                    return result;
                } catch (SQLException failure) {
                    rollback(connection);
                    if (duplicate != null && isDuplicate(failure)) { return duplicate; }
                    throw new AtlasPersistenceException("Atlas transaction failed", failure);
                } catch (RuntimeException failure) {
                    rollback(connection);
                    throw new AtlasPersistenceException("malformed Atlas persistence data", failure);
                }
            } catch (SQLException failure) {
                throw new AtlasPersistenceException("Atlas connection failed", failure);
            }
        }, executor);
    }

    private static boolean isDuplicate(final SQLException failure) {
        String message = failure.getMessage();
        return message != null && (message.toLowerCase(Locale.ROOT).contains("unique")
                || message.toLowerCase(Locale.ROOT).contains("primary key"));
    }

    private static void rollback(final Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }

    private static <E extends Enum<E>> E parse(final Class<E> type, final String value) {
        try { return Enum.valueOf(type, value); }
        catch (RuntimeException failure) {
            throw new AtlasPersistenceException("malformed " + type.getSimpleName(), failure);
        }
    }

    private interface Work<T> { T run(Connection connection) throws SQLException; }

    /** Immutable reviewer/reputation persistence projection. */
    public static final class ReviewerProfile {
        private final AtlasReviewerId reviewerId;
        private final long lifetimeReviews;
        private final long recentReviews;
        private final double accuracy;
        private final double confidence;
        private final double reputation;
        private final Instant updatedAt;

        public ReviewerProfile(final AtlasReviewerId reviewerId, final long lifetimeReviews,
                               final long recentReviews, final double accuracy,
                               final double confidence, final double reputation,
                               final Instant updatedAt) {
            if (lifetimeReviews < 0 || recentReviews < 0 || accuracy < 0 || accuracy > 1
                    || confidence < 0 || confidence > 1 || !Double.isFinite(reputation)) {
                throw new IllegalArgumentException("invalid reviewer profile");
            }
            this.reviewerId = Objects.requireNonNull(reviewerId, "reviewerId");
            this.lifetimeReviews = lifetimeReviews;
            this.recentReviews = recentReviews;
            this.accuracy = accuracy;
            this.confidence = confidence;
            this.reputation = reputation;
            this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        }
        public AtlasReviewerId reviewerId() { return reviewerId; }
        public long lifetimeReviews() { return lifetimeReviews; }
        public long recentReviews() { return recentReviews; }
        public double accuracy() { return accuracy; }
        public double confidence() { return confidence; }
        public double reputation() { return reputation; }
        public Instant updatedAt() { return updatedAt; }
    }
}
