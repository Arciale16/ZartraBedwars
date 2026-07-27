package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.replay.api.ReplaySession;
import java.util.Objects;

/** Immutable outcome of one replay event-ingestion attempt. */
public final class ReplayIngestionResult {
    /** Stable outcome categories for caller recovery and metrics. */
    public enum Status {
        /** One or more immutable replay events were appended. */ ACCEPTED,
        /** The source event had already been observed. */ DUPLICATE,
        /** The source event violated structural or temporal constraints. */ MALFORMED,
        /** No registered M08, M11 or M12 adapter supports the source event. */ UNSUPPORTED,
        /** The replay session is not currently recording. */ INVALID_STATE
    }

    private final Status status;
    private final ReplaySession session;
    private final String detail;

    private ReplayIngestionResult(final Status status, final ReplaySession session,
                                  final String detail) {
        this.status = Objects.requireNonNull(status, "status");
        this.session = Objects.requireNonNull(session, "session");
        this.detail = Objects.requireNonNull(detail, "detail");
    }

    static ReplayIngestionResult of(final Status status, final ReplaySession session,
                                    final String detail) {
        return new ReplayIngestionResult(status, session, detail);
    }

    /** Returns the typed ingestion status. */ public Status status() { return status; }
    /** Returns the resulting immutable session, unchanged on non-acceptance. */
    public ReplaySession session() { return session; }
    /** Returns a stable sanitized diagnostic code. */ public String detail() { return detail; }
    /** Returns whether the timeline changed. */ public boolean accepted() { return status == Status.ACCEPTED; }
}
