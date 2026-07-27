package io.zartra.bedwars.replay.api;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Async replay-session boundary with compare-state conflict protection. */
public interface ReplaySessionRepository {
    /** Atomically creates metadata, participants and initial session state. */
    CompletionStage<Boolean> create(ReplaySession session);
    /** Saves a state transition only when the persisted state equals the expected state. */
    CompletionStage<SaveResult> save(ReplaySession session, ReplayState expectedState);
    /** Restart-safe load of metadata, state and ordered timeline. */
    CompletionStage<Optional<ReplaySession>> findSession(ReplayId replayId);

    /** Stable compare-state persistence outcomes. */
    enum SaveResult {
        /** State was persisted. */ UPDATED,
        /** Persisted state did not match the caller expectation. */ CONFLICT,
        /** Replay session does not exist. */ NOT_FOUND
    }
}
