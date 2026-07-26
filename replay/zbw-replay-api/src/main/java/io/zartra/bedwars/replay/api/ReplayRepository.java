package io.zartra.bedwars.replay.api;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Asynchronous, JDBC-free replay metadata and stream repository boundary. */
public interface ReplayRepository {
    /** Saves metadata without blocking the caller thread. */
    CompletionStage<Void> saveMetadata(ReplayMetadata metadata);
    /** Finds metadata asynchronously. */
    CompletionStage<Optional<ReplayMetadata>> findMetadata(ReplayId replayId);
    /** Appends one ordered immutable event asynchronously and idempotently. */
    CompletionStage<Void> appendEvent(ReplayId replayId, ReplayEvent event);
    /** Loads an ordered timeline asynchronously. */
    CompletionStage<ReplayTimeline> loadTimeline(ReplayId replayId);
    /** Persists the latest lifecycle state asynchronously. */
    CompletionStage<Void> saveSession(ReplaySession session);
}
