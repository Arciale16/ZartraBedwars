package io.zartra.bedwars.paper.replay;

import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.playback.PlaybackSession;
import java.util.Objects;

/** Immutable binding between an authoritative replay and its playback aggregate. */
public final class ReplayRuntimeContext {
    private final ReplaySession replay;
    private final PlaybackSession playback;

    /** Creates a validated runtime context. */
    public ReplayRuntimeContext(final ReplaySession replay, final PlaybackSession playback) {
        this.replay = Objects.requireNonNull(replay, "replay");
        this.playback = Objects.requireNonNull(playback, "playback");
        if (!replay.metadata().replayId().equals(playback.replayId())) {
            throw new IllegalArgumentException("replay and playback identities differ");
        }
    }

    /** @return authoritative immutable replay */ public ReplaySession replay() { return replay; }
    /** @return current immutable playback aggregate */ public PlaybackSession playback() { return playback; }
    /** @return a context retaining the same authoritative replay */
    public ReplayRuntimeContext withPlayback(final PlaybackSession next) {
        return new ReplayRuntimeContext(replay, next);
    }
}
