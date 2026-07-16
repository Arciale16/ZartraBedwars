package io.zartra.bedwars.game.spi;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.game.model.MatchSnapshot;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Non-blocking persistence boundary for recoverable match aggregates. */
public interface MatchRepository {
    /** Loads the latest snapshot without blocking the caller. */
    CompletionStage<Optional<MatchSnapshot>> load(MatchId matchId);

    /**
     * Persists a transition with optimistic compare-and-set semantics.
     *
     * @param previousRevision revision expected in storage
     * @param snapshot next snapshot
     * @return stage resolving to true only when the revision was accepted
     */
    CompletionStage<Boolean> save(long previousRevision, MatchSnapshot snapshot);

    /**
     * Atomically persists the completion snapshot and durable outbox facts.
     * Repeating the same key must succeed without duplicating either side effect.
     */
    CompletionStage<Boolean> commitCompletion(
            long previousRevision, MatchSnapshot snapshot, IdempotencyKey key);
}
