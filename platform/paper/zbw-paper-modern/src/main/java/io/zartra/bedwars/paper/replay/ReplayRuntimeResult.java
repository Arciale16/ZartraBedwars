package io.zartra.bedwars.paper.replay;

import java.util.Objects;
import java.util.Optional;

/** Immutable command/service outcome for the M17 Paper runtime boundary. */
public final class ReplayRuntimeResult {
    /** Stable outcomes suitable for command localization without leaking storage failures. */
    public enum Status {
        OPENED, STARTED, PAUSED, STOPPED, SEEKED, SPEED_CHANGED, INSPECTED,
        FORBIDDEN, NOT_FOUND,
        FAILED, INACTIVE, ALREADY_OPEN, NO_SESSION, INVALID_STATE
    }

    private final Status status;
    private final SpectatorReplaySession session;

    private ReplayRuntimeResult(final Status status, final SpectatorReplaySession session) {
        this.status = Objects.requireNonNull(status, "status");
        this.session = session;
    }

    /** @return an outcome without a session projection */
    public static ReplayRuntimeResult of(final Status status) {
        return new ReplayRuntimeResult(status, null);
    }

    /** @return an outcome with the current immutable session projection */
    public static ReplayRuntimeResult of(final Status status,
                                         final SpectatorReplaySession session) {
        return new ReplayRuntimeResult(status, Objects.requireNonNull(session, "session"));
    }

    /** @return stable outcome */
    public Status status() { return status; }
    /** @return current spectator session when applicable */
    public Optional<SpectatorReplaySession> session() { return Optional.ofNullable(session); }
}
