package io.zartra.bedwars.replay.api;

import java.util.Objects;
import java.util.Optional;

/** Immutable replay recording state machine (ZBW-REPLAY-001, ZBW-REPLAY-009). */
public final class ReplaySession {
    private final ReplayMetadata metadata;
    private final ReplayState state;
    private final ReplayTimeline timeline;
    private final String failureReason;

    private ReplaySession(final ReplayMetadata metadata, final ReplayState state,
                          final ReplayTimeline timeline, final String failureReason) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.state = Objects.requireNonNull(state, "state");
        this.timeline = Objects.requireNonNull(timeline, "timeline");
        this.failureReason = failureReason;
    }

    /** Creates a session before recording begins. */
    public static ReplaySession create(final ReplayMetadata metadata) {
        return new ReplaySession(metadata, ReplayState.CREATED, ReplayTimeline.empty(), null);
    }

    /** Starts event admission. */ public ReplaySession start() { return transition(ReplayState.RECORDING, null); }

    /** Appends one event while recording; duplicate identities are idempotent. */
    public ReplaySession record(final ReplayEvent event) {
        requireState(ReplayState.RECORDING);
        return new ReplaySession(metadata, state, timeline.append(event), null);
    }

    /** Closes a successful recording. */
    public ReplaySession complete() { return transition(ReplayState.COMPLETED, null); }

    /** Moves a completed recording to archive state. */
    public ReplaySession archive() { return transition(ReplayState.ARCHIVED, null); }

    /** Terminates a created or recording session with a sanitized reason. */
    public ReplaySession fail(final String reason) {
        final String value = requireReason(reason);
        if (state != ReplayState.CREATED && state != ReplayState.RECORDING) {
            throw new IllegalStateException("cannot fail replay in " + state);
        }
        return new ReplaySession(metadata, ReplayState.FAILED, timeline, value);
    }

    private ReplaySession transition(final ReplayState target, final String reason) {
        final boolean valid = (state == ReplayState.CREATED && target == ReplayState.RECORDING)
                || (state == ReplayState.RECORDING && target == ReplayState.COMPLETED)
                || (state == ReplayState.COMPLETED && target == ReplayState.ARCHIVED);
        if (!valid) { throw new IllegalStateException("invalid replay transition " + state + " -> " + target); }
        return new ReplaySession(metadata, target, timeline, reason);
    }

    private void requireState(final ReplayState expected) {
        if (state != expected) { throw new IllegalStateException("replay is " + state); }
    }

    private static String requireReason(final String reason) {
        if (reason == null || reason.trim().isEmpty() || reason.length() > 256) {
            throw new IllegalArgumentException("failure reason must contain 1..256 characters");
        }
        return reason;
    }

    /** Returns immutable metadata. */ public ReplayMetadata metadata() { return metadata; }
    /** Returns current state. */ public ReplayState state() { return state; }
    /** Returns immutable ordered timeline. */ public ReplayTimeline timeline() { return timeline; }
    /** Returns sanitized failure reason when failed. */
    public Optional<String> failureReason() { return Optional.ofNullable(failureReason); }
}
