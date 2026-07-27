package io.zartra.bedwars.replay.playback;

import io.zartra.bedwars.replay.api.ReplayEvent;

/** Pure boundary for deterministic replay-event state reconstruction. */
public interface ReplayEventApplier {
    /**
     * Applies exactly one next event without mutating the prior snapshot.
     *
     * @param snapshot prior immutable state
     * @param event next ordered event
     * @return new immutable state positioned at the event
     */
    ReplaySnapshot apply(ReplaySnapshot snapshot, ReplayEvent event);
}
