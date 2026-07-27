package io.zartra.bedwars.replay.api;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Async authoritative metadata boundary; implementations must isolate replay identities. */
public interface ReplayMetadataRepository {
    /** Creates immutable metadata; returns false when the replay identity already exists. */
    CompletionStage<Boolean> create(ReplayMetadata metadata);
    /** Loads immutable metadata without blocking the caller thread. */
    CompletionStage<Optional<ReplayMetadata>> findMetadata(ReplayId replayId);
}
