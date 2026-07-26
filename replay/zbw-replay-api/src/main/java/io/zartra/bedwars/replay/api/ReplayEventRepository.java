package io.zartra.bedwars.replay.api;

import java.util.List;
import java.util.concurrent.CompletionStage;

/** Async append-only event-stream boundary with deterministic reads. */
public interface ReplayEventRepository {
    /** Appends one event atomically. */
    CompletionStage<AppendResult> append(ReplayId replayId, ReplayEvent event);
    /** Appends an ordered batch in one transaction; any conflict rolls back the whole batch. */
    CompletionStage<AppendResult> appendAll(ReplayId replayId, List<ReplayEvent> events);
    /** Loads the complete timeline ordered strictly by replay-local sequence. */
    CompletionStage<ReplayTimeline> loadTimeline(ReplayId replayId);

    /** Stable append outcomes. */
    enum AppendResult {
        /** Every supplied event was inserted. */ INSERTED,
        /** Every supplied event identity was already present. */ DUPLICATE,
        /** Sequence, identity or session state conflicts with persisted data. */ CONFLICT,
        /** Replay session does not exist. */ NOT_FOUND
    }
}
