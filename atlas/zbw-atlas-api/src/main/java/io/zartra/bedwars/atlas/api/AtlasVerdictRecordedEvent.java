package io.zartra.bedwars.atlas.api;

import io.zartra.bedwars.api.event.EventMetadata;
import java.util.Objects;

/** Immutable post-commit fact that a verdict was recorded; it never represents punishment. */
public final class AtlasVerdictRecordedEvent implements AtlasEvent {
    private final EventMetadata metadata;
    private final AtlasCaseId caseId;
    private final AtlasReviewId reviewId;
    private final ReviewVerdict verdict;
    private final ReviewReason reason;

    /** Creates a recorded-verdict event without identity or punishment payload. */
    public AtlasVerdictRecordedEvent(final EventMetadata metadata, final AtlasCaseId caseId,
                                     final AtlasReviewId reviewId, final ReviewVerdict verdict,
                                     final ReviewReason reason) {
        if (verdict == ReviewVerdict.UNABLE_TO_REVIEW) {
            throw new IllegalArgumentException("recorded verdict must be substantive");
        }
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.reviewId = Objects.requireNonNull(reviewId, "reviewId");
        this.verdict = Objects.requireNonNull(verdict, "verdict");
        this.reason = Objects.requireNonNull(reason, "reason");
    }
    @Override public EventMetadata metadata() { return metadata; }
    /** Returns case identity. */ public AtlasCaseId caseId() { return caseId; }
    /** Returns source review identity. */ public AtlasReviewId reviewId() { return reviewId; }
    /** Returns recorded verdict. */ public ReviewVerdict verdict() { return verdict; }
    /** Returns recorded reason. */ public ReviewReason reason() { return reason; }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasVerdictRecordedEvent)) { return false; }
        final AtlasVerdictRecordedEvent that = (AtlasVerdictRecordedEvent) other;
        return metadata.equals(that.metadata) && caseId.equals(that.caseId)
                && reviewId.equals(that.reviewId) && verdict == that.verdict
                && reason == that.reason;
    }
    @Override public int hashCode() {
        return Objects.hash(metadata, caseId, reviewId, verdict, reason);
    }
}
