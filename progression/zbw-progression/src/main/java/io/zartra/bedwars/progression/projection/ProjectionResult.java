package io.zartra.bedwars.progression.projection;

import io.zartra.bedwars.storage.api.RecordRevision;
import java.util.Objects;
import java.util.Optional;

/** Typed projection outcome with explicit duplicate semantics. */
public final class ProjectionResult {
    private final Status status;
    private final ProjectionCheckpoint checkpoint;
    private final RecordRevision revision;
    private final String detail;

    /** Creates a projection result. */
    public ProjectionResult(final Status status, final ProjectionCheckpoint checkpoint,
                            final RecordRevision revision, final String detail) {
        this.status = Objects.requireNonNull(status, "status");
        this.checkpoint = checkpoint;
        this.revision = revision;
        if (detail != null && detail.length() > 256) { throw new IllegalArgumentException("detail exceeds 256 characters"); }
        if (status != Status.RETRYABLE_FAILURE && status != Status.REJECTED && detail != null) { throw new IllegalArgumentException("successful results cannot carry failure detail"); }
        this.detail = detail;
    }
    /** @return projection outcome */ public Status status() { return status; }
    /** @return committed checkpoint when available */ public Optional<ProjectionCheckpoint> checkpoint() { return Optional.ofNullable(checkpoint); }
    /** @return resulting aggregate revision when available */ public Optional<RecordRevision> revision() { return Optional.ofNullable(revision); }
    /** @return sanitized failure detail when present */ public Optional<String> detail() { return Optional.ofNullable(detail); }
    /** Projection lifecycle outcomes. */
    public enum Status {
        /** Mutation and checkpoint committed. */ APPLIED,
        /** Existing inbox checkpoint suppressed a duplicate. */ DUPLICATE,
        /** Temporary failure may be retried. */ RETRYABLE_FAILURE,
        /** Invalid or unauthorized input failed closed. */ REJECTED
    }
}
