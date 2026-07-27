package io.zartra.bedwars.atlas.sql;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.atlas.api.AtlasCase;
import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasCaseMetadata;
import io.zartra.bedwars.atlas.api.AtlasCaseSource;
import io.zartra.bedwars.atlas.api.AtlasCaseStatus;
import io.zartra.bedwars.atlas.api.AtlasEvidenceId;
import io.zartra.bedwars.atlas.api.AtlasEvidenceReference;
import io.zartra.bedwars.atlas.api.AtlasReview;
import io.zartra.bedwars.atlas.api.AtlasReviewId;
import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import io.zartra.bedwars.atlas.api.ReviewDecision;
import io.zartra.bedwars.atlas.api.ReviewReason;
import io.zartra.bedwars.atlas.api.ReviewVerdict;
import io.zartra.bedwars.atlas.core.AtlasAuditRecord;
import io.zartra.bedwars.replay.api.ReplayId;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

/** ZBW-ATLAS-003/004/005/007/009 persistence, restart and privacy evidence. */
class JdbcAtlasRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-07-27T14:00:00Z");
    private static final Executor DIRECT = Runnable::run;
    @TempDir Path temporary;

    @Test void roundTripDuplicatesAndRestart() {
        SQLiteDataSource source = source("atlas.db");
        AtlasSchemaMigrator migrator = new AtlasSchemaMigrator();
        assertTrue(migrator.migrate(source, DIRECT).toCompletableFuture().join());
        JdbcAtlasRepository repository = new JdbcAtlasRepository(source, DIRECT);
        AtlasCase value = atlasCase();
        assertTrue(repository.create(value).toCompletableFuture().join());
        assertFalse(repository.create(value).toCompletableFuture().join());
        assertEquals(value, repository.find(value.caseId()).toCompletableFuture().join().get());
        AtlasReview review = review(value.caseId());
        assertTrue(repository.append(review).toCompletableFuture().join());
        assertFalse(repository.append(review).toCompletableFuture().join());
        assertEquals(review, repository.find(review.reviewId()).toCompletableFuture().join().get());
        assertEquals(1, repository.findByCase(value.caseId()).toCompletableFuture().join().size());
        assertTrue(repository.reserve(value.caseId(), AtlasReviewerId.random(), NOW.plusSeconds(60))
                .toCompletableFuture().join());
        assertFalse(repository.reserve(value.caseId(), AtlasReviewerId.random(), NOW.plusSeconds(60))
                .toCompletableFuture().join());
        repository.saveProfile(new JdbcAtlasRepository.ReviewerProfile(
                AtlasReviewerId.random(), 20, 5, 0.8, 0.9, 12.5, NOW))
                .toCompletableFuture().join();
        assertTrue(repository.appendAudit(1, new AtlasAuditRecord(
                "reviewer:1", "review.submit", "case:1", NOW,
                "accepted", "revision:0", "revision:1")).toCompletableFuture().join());
        assertFalse(repository.appendAudit(1, new AtlasAuditRecord(
                "reviewer:1", "review.submit", "case:1", NOW,
                "accepted", "revision:0", "revision:1")).toCompletableFuture().join());
        assertFalse(migrator.migrate(source("atlas.db"), DIRECT).toCompletableFuture().join());
        JdbcAtlasRepository restarted = new JdbcAtlasRepository(source("atlas.db"), DIRECT);
        assertEquals(value, restarted.find(value.caseId()).toCompletableFuture().join().get());
    }

    @Test void failedEvidenceBatchRollsBackWholeCase() throws Exception {
        SQLiteDataSource source = source("rollback.db");
        new AtlasSchemaMigrator().migrate(source, DIRECT).toCompletableFuture().join();
        JdbcAtlasRepository repository = new JdbcAtlasRepository(source, DIRECT);
        AtlasCase value = atlasCase();
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO atlas_evidence VALUES(?,?,?,?,?,?,?)")) {
            statement.setString(1, value.caseId().toString());
            statement.setString(2, value.evidence().get(0).evidenceId().toString());
            statement.setString(3, "REPORT");
            statement.setString(4, null);
            statement.setString(5, "report:preexisting");
            statement.setLong(6, 0);
            statement.setLong(7, 0);
            statement.executeUpdate();
        }
        assertFalse(repository.create(value).toCompletableFuture().join());
        assertFalse(repository.find(value.caseId()).toCompletableFuture().join().isPresent());
    }

    @Test void identityVaultIsSeparatedAndInputIsDefensive() throws Exception {
        SQLiteDataSource source = source("privacy.db");
        new AtlasSchemaMigrator().migrate(source, DIRECT).toCompletableFuture().join();
        JdbcAtlasRepository repository = new JdbcAtlasRepository(source, DIRECT);
        AtlasCaseId caseId = AtlasCaseId.random();
        byte[] encrypted = new byte[32];
        Arrays.fill(encrypted, (byte) 7);
        assertTrue(repository.storeIdentity(caseId, encrypted).toCompletableFuture().join());
        encrypted[0] = 1;
        assertArrayEquals(repeat((byte) 7, 32),
                repository.findIdentity(caseId).toCompletableFuture().join().get());
        try (Connection connection = source.getConnection();
             Statement statement = connection.createStatement()) {
            assertFalse(statement.executeQuery(
                    "SELECT name FROM pragma_table_info('atlas_cases') WHERE name='encrypted_identity'")
                    .next());
        }
        assertThrows(IllegalArgumentException.class,
                () -> repository.storeIdentity(caseId, new byte[2]));
    }

    @Test void malformedPersistenceAndReplayReferenceFailClosed() throws Exception {
        SQLiteDataSource source = source("malformed.db");
        new AtlasSchemaMigrator().migrate(source, DIRECT).toCompletableFuture().join();
        AtlasCase value = atlasCase();
        JdbcAtlasRepository repository = new JdbcAtlasRepository(source, DIRECT);
        repository.create(value).toCompletableFuture().join();
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE atlas_cases SET status='CORRUPT' WHERE case_id=?")) {
            statement.setString(1, value.caseId().toString());
            statement.executeUpdate();
        }
        assertThrows(CompletionException.class,
                () -> repository.find(value.caseId()).toCompletableFuture().join());
        assertThrows(NullPointerException.class, () -> AtlasEvidenceReference.replay(
                AtlasEvidenceId.random(), null, 0, 1));
    }

    private AtlasCase atlasCase() {
        AtlasEvidenceReference replay = AtlasEvidenceReference.replay(
                AtlasEvidenceId.random(), ReplayId.random(), 20, 80);
        AtlasEvidenceReference report = AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "report:18");
        AtlasEvidenceReference signal = AtlasEvidenceReference.internalSignal(
                AtlasEvidenceId.random(), "signal:18");
        AtlasCaseMetadata metadata = new AtlasCaseMetadata(
                AtlasCaseSource.REPLAY_EVIDENCE, NOW, "combat",
                "source:" + java.util.UUID.randomUUID(), 50, 1);
        return new AtlasCase(AtlasCaseId.random(), AtlasCaseStatus.OPEN,
                metadata, Arrays.asList(replay, report, signal), 0);
    }

    private static AtlasReview review(final AtlasCaseId caseId) {
        return new AtlasReview(AtlasReviewId.random(), caseId, AtlasReviewerId.random(),
                ReviewDecision.VERDICT, ReviewVerdict.NOT_CHEATING,
                ReviewReason.INSUFFICIENT_EVIDENCE, NOW, 60_000, 1);
    }

    private SQLiteDataSource source(final String name) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + temporary.resolve(name));
        return source;
    }

    private static byte[] repeat(final byte value, final int count) {
        byte[] result = new byte[count];
        Arrays.fill(result, value);
        return result;
    }
}
