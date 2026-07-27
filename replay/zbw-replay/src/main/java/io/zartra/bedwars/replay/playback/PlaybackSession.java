package io.zartra.bedwars.replay.playback;

import io.zartra.bedwars.replay.api.ReplayId;
import io.zartra.bedwars.replay.api.ReplayTimeline;
import java.util.Objects;
import java.util.Optional;

/** Immutable platform-neutral playback aggregate (ZBW-REPLAY-004). */
public final class PlaybackSession {
    private final ReplayId replayId;
    private final ReplayTimeline timeline;
    private final PlaybackState state;
    private final PlaybackSpeed speed;
    private final ReplaySnapshot snapshot;
    private final String failureReason;

    private PlaybackSession(final ReplayId replayId, final ReplayTimeline timeline,
                            final PlaybackState state, final PlaybackSpeed speed,
                            final ReplaySnapshot snapshot, final String failureReason) {
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.timeline = Objects.requireNonNull(timeline, "timeline");
        this.state = Objects.requireNonNull(state, "state");
        this.speed = Objects.requireNonNull(speed, "speed");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.failureReason = failureReason;
    }

    /** Creates a playback before an authoritative replay has loaded. */
    public static PlaybackSession create(final ReplayId replayId) {
        return new PlaybackSession(replayId, ReplayTimeline.empty(), PlaybackState.CREATED,
                PlaybackSpeed.NORMAL, ReplaySnapshot.empty(), null);
    }

    static PlaybackSession loading(final ReplayId replayId) {
        return create(replayId).with(PlaybackState.LOADING, ReplayTimeline.empty(),
                ReplaySnapshot.empty(), PlaybackSpeed.NORMAL, null);
    }

    PlaybackSession with(final PlaybackState nextState, final ReplayTimeline nextTimeline,
                         final ReplaySnapshot nextSnapshot, final PlaybackSpeed nextSpeed,
                         final String nextFailure) {
        if ((nextState == PlaybackState.FAILED) != (nextFailure != null)) {
            throw new IllegalArgumentException("failure reason must exist only for FAILED state");
        }
        return new PlaybackSession(replayId, nextTimeline, nextState, nextSpeed,
                nextSnapshot, nextFailure);
    }

    /** Returns replay identity. */
    public ReplayId replayId() {
        return replayId;
    }

    /** Returns the immutable source timeline. */
    public ReplayTimeline timeline() {
        return timeline;
    }

    /** Returns playback lifecycle state. */
    public PlaybackState state() {
        return state;
    }

    /** Returns current bounded speed. */
    public PlaybackSpeed speed() {
        return speed;
    }

    /** Returns current cursor. */
    public PlaybackCursor cursor() {
        return snapshot.cursor();
    }

    /** Returns current deterministic snapshot. */
    public ReplaySnapshot snapshot() {
        return snapshot;
    }

    /** Returns a sanitized failure code for failed playback. */
    public Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }
}
