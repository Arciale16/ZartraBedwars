package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.replay.api.ReplaySession;

/** Type-safe internal adapter from an existing immutable source fact to replay events. */
interface ReplaySourceEventAdapter<T> {
    /** Returns the exact supported source contract. */ Class<T> sourceType();
    /** Returns whether the producer already classified this source fact as duplicate. */
    boolean duplicate(T sourceEvent, ReplaySession session, String sourceEventId);
    /** Converts and appends deterministic replay events. */
    ReplaySession ingest(ReplaySession session, T sourceEvent, String sourceEventId);
}
