package io.zartra.bedwars.replay.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.replay.api.ReplayEvent;
import io.zartra.bedwars.replay.api.ReplayEventRepository;
import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayMetadata;
import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplaySessionRepository;
import io.zartra.bedwars.replay.api.ReplayState;
import io.zartra.bedwars.replay.api.ReplayTimeline;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

/** ZBW-REPLAY-001/003/008/009 transaction, rollback and restart evidence. */
class JdbcReplayRepositoryTest {
    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Executor DIRECT = Runnable::run;
    @TempDir Path temporary;

    @Test void insertLoadDuplicateArchiveAndRestart() {
        SQLiteDataSource source = source("main.db");
        ReplaySchemaMigrator migrator = new ReplaySchemaMigrator();
        assertTrue(migrator.migrate(source, DIRECT).toCompletableFuture().join().applied());
        JdbcReplayRepository repository = new JdbcReplayRepository(source, DIRECT, 5);
        ReplaySession session = ReplaySession.create(metadata()).start();
        assertTrue(repository.create(session).toCompletableFuture().join());
        assertFalse(repository.create(session).toCompletableFuture().join());
        assertTrue(repository.findMetadata(session.metadata().replayId()).toCompletableFuture().join().isPresent());
        ReplayEvent zero = event("zero", 0, 1);
        ReplayEvent one = event("one", 1, 2);
        assertEquals(ReplayEventRepository.AppendResult.INSERTED,
                repository.appendAll(session.metadata().replayId(), Arrays.asList(zero, one)).toCompletableFuture().join());
        assertEquals(ReplayEventRepository.AppendResult.DUPLICATE,
                repository.append(session.metadata().replayId(), zero).toCompletableFuture().join());
        ReplayTimeline timeline = repository.loadTimeline(session.metadata().replayId()).toCompletableFuture().join();
        assertEquals(Arrays.asList("zero", "one"), Arrays.asList(
                timeline.events().get(0).eventId(), timeline.events().get(1).eventId()));
        session = ReplaySession.restore(session.metadata(), ReplayState.RECORDING, timeline, null);
        ReplaySession completed = session.complete();
        assertEquals(ReplaySessionRepository.SaveResult.UPDATED,
                repository.save(completed, ReplayState.RECORDING).toCompletableFuture().join());
        ReplaySession archived = completed.archive();
        assertEquals(ReplaySessionRepository.SaveResult.UPDATED,
                repository.save(archived, ReplayState.COMPLETED).toCompletableFuture().join());
        assertFalse(migrator.migrate(source("main.db"), DIRECT).toCompletableFuture().join().applied());
        JdbcReplayRepository restarted = new JdbcReplayRepository(source("main.db"), DIRECT, 5);
        ReplaySession loaded = restarted.findSession(archived.metadata().replayId()).toCompletableFuture().join().get();
        assertEquals(ReplayState.ARCHIVED, loaded.state());
        assertEquals(2, loaded.timeline().events().size());
    }

    @Test void conflictingBatchRollsBackAndFailurePersists() {
        SQLiteDataSource source = source("rollback.db");
        new ReplaySchemaMigrator().migrate(source, DIRECT).toCompletableFuture().join();
        JdbcReplayRepository repository = new JdbcReplayRepository(source, DIRECT, 5);
        ReplaySession recording = ReplaySession.create(metadata()).start();
        repository.create(recording).toCompletableFuture().join();
        assertEquals(ReplayEventRepository.AppendResult.CONFLICT, repository.appendAll(
                recording.metadata().replayId(), Arrays.asList(event("zero", 0, 0), event("gap", 2, 2)))
                .toCompletableFuture().join());
        assertTrue(repository.loadTimeline(recording.metadata().replayId()).toCompletableFuture().join().events().isEmpty());
        ReplaySession failed = recording.fail("disk unavailable");
        assertEquals(ReplaySessionRepository.SaveResult.UPDATED,
                repository.save(failed, ReplayState.RECORDING).toCompletableFuture().join());
        assertEquals("disk unavailable", repository.findSession(failed.metadata().replayId())
                .toCompletableFuture().join().get().failureReason().get());
        assertEquals(ReplaySessionRepository.SaveResult.CONFLICT,
                repository.save(failed, ReplayState.CREATED).toCompletableFuture().join());
        assertEquals(ReplayEventRepository.AppendResult.CONFLICT,
                repository.append(failed.metadata().replayId(), event("late", 0, 1)).toCompletableFuture().join());
    }

    @Test void missingMalformedAndChecksumDriftFailClosed() throws Exception {
        SQLiteDataSource source = source("bad.db");
        ReplaySchemaMigrator migrator = new ReplaySchemaMigrator();
        migrator.migrate(source, DIRECT).toCompletableFuture().join();
        JdbcReplayRepository repository = new JdbcReplayRepository(source, DIRECT, 5);
        ReplayId missing = ReplayId.random();
        assertFalse(repository.findMetadata(missing).toCompletableFuture().join().isPresent());
        assertFalse(repository.findSession(missing).toCompletableFuture().join().isPresent());
        assertEquals(ReplayEventRepository.AppendResult.NOT_FOUND,
                repository.append(missing, event("missing", 0, 0)).toCompletableFuture().join());
        assertEquals(ReplaySessionRepository.SaveResult.NOT_FOUND,
                repository.save(ReplaySession.create(metadata()), ReplayState.CREATED).toCompletableFuture().join());
        ReplaySession recording = ReplaySession.create(metadata()).start();
        repository.create(recording).toCompletableFuture().join();
        repository.append(recording.metadata().replayId(), event("bad", 0, 1)).toCompletableFuture().join();
        execute(source, "UPDATE replay_events SET attributes='not-base64:'");
        assertThrows(CompletionException.class, () -> repository.loadTimeline(recording.metadata().replayId()).toCompletableFuture().join());
        execute(source, "UPDATE zbw_schema_history SET checksum='drift' WHERE version=1701");
        assertThrows(CompletionException.class, () -> migrator.migrate(source, DIRECT).toCompletableFuture().join());
        assertThrows(IllegalArgumentException.class, () -> new JdbcReplayRepository(source, DIRECT, 0));
    }

    private ReplayMetadata metadata() {
        return new ReplayMetadata(ReplayId.random(), MatchId.random(), START, 1,
                Collections.singleton(PlayerId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"))), false);
    }
    private ReplayEvent event(String id, long sequence, long offset) {
        return new ReplayEvent(id, sequence, offset, START.plusMillis(offset), ReplayEvent.Source.GAME,
                "game.fact", Collections.singletonMap("key", "value"));
    }
    private SQLiteDataSource source(String name) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + temporary.resolve(name).toAbsolutePath());
        return source;
    }
    private static void execute(SQLiteDataSource source, String sql) throws SQLException {
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
