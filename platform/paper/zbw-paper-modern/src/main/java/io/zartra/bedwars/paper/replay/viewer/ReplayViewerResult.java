package io.zartra.bedwars.paper.replay.viewer;

import java.util.Objects;
import java.util.Optional;

/** Immutable viewer adapter and command-routing outcome. */
public final class ReplayViewerResult {
    /** Sanitized outcomes exposed to Paper presentation. */
    public enum Status {
        SUCCESS,
        FORBIDDEN,
        NOT_FOUND,
        FAILED,
        NO_SESSION,
        INVALID_STATE,
        INVALID_COMMAND
    }

    private final Status status;
    private final ReplayViewerSession session;

    private ReplayViewerResult(final Status status, final ReplayViewerSession session) {
        this.status = Objects.requireNonNull(status, "status");
        this.session = session;
    }

    /** @return outcome without a viewer projection */
    public static ReplayViewerResult of(final Status status) {
        return new ReplayViewerResult(status, null);
    }

    /** @return outcome with current viewer projection */
    public static ReplayViewerResult success(final ReplayViewerSession session) {
        return new ReplayViewerResult(Status.SUCCESS,
                Objects.requireNonNull(session, "session"));
    }

    /** @return stable sanitized status */
    public Status status() { return status; }
    /** @return current viewer session when available */
    public Optional<ReplayViewerSession> session() { return Optional.ofNullable(session); }
}
