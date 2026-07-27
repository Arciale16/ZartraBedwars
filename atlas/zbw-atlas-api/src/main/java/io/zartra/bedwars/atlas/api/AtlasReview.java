package io.zartra.bedwars.atlas.api;

import java.time.Instant;
import java.util.Objects;

/** Immutable Atlas review submission (ZBW-ATLAS-005/006). */
public final class AtlasReview {
    private final AtlasReviewId reviewId;
    private final AtlasCaseId caseId;
    private final AtlasReviewerId reviewerId;
    private final ReviewDecision decision;
    private final ReviewVerdict verdict;
    private final ReviewReason reason;
    private final Instant submittedAt;
    private final long interactionMillis;
    private final int schemaVersion;

    /** Creates a validated immutable review boundary. */
    public AtlasReview(final AtlasReviewId reviewId, final AtlasCaseId caseId,
                       final AtlasReviewerId reviewerId, final ReviewDecision decision,
                       final ReviewVerdict verdict, final ReviewReason reason,
                       final Instant submittedAt, final long interactionMillis,
                       final int schemaVersion) {
        if (interactionMillis < 0) {
            throw new IllegalArgumentException("interactionMillis must be non-negative");
        }
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        final ReviewDecision checkedDecision = Objects.requireNonNull(decision, "decision");
        final ReviewVerdict checkedVerdict = Objects.requireNonNull(verdict, "verdict");
        final ReviewReason checkedReason = Objects.requireNonNull(reason, "reason");
        if (checkedDecision == ReviewDecision.VERDICT
                && checkedVerdict == ReviewVerdict.UNABLE_TO_REVIEW) {
            throw new IllegalArgumentException("verdict decision requires a substantive verdict");
        }
        if (checkedDecision != ReviewDecision.VERDICT
                && checkedVerdict != ReviewVerdict.UNABLE_TO_REVIEW) {
            throw new IllegalArgumentException("skip and abstain cannot record a substantive verdict");
        }
        if (checkedDecision == ReviewDecision.SKIP
                && checkedReason != ReviewReason.EVIDENCE_ERROR
                && checkedReason != ReviewReason.OTHER) {
            throw new IllegalArgumentException("skip reason must describe evidence failure or other");
        }
        this.reviewId = Objects.requireNonNull(reviewId, "reviewId");
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.reviewerId = Objects.requireNonNull(reviewerId, "reviewerId");
        this.decision = checkedDecision;
        this.verdict = checkedVerdict;
        this.reason = checkedReason;
        this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
        this.interactionMillis = interactionMillis;
        this.schemaVersion = schemaVersion;
    }

    /** Returns review identity. */ public AtlasReviewId reviewId() { return reviewId; }
    /** Returns reviewed case identity. */ public AtlasCaseId caseId() { return caseId; }
    /** Returns internal reviewer identity; community projections must omit it. */
    public AtlasReviewerId reviewerId() { return reviewerId; }
    /** Returns submission action. */ public ReviewDecision decision() { return decision; }
    /** Returns stable verdict. */ public ReviewVerdict verdict() { return verdict; }
    /** Returns stable reason. */ public ReviewReason reason() { return reason; }
    /** Returns submission instant. */ public Instant submittedAt() { return submittedAt; }
    /** Returns measured meaningful interaction duration. */
    public long interactionMillis() { return interactionMillis; }
    /** Returns serialized contract version. */ public int schemaVersion() { return schemaVersion; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasReview)) { return false; }
        final AtlasReview that = (AtlasReview) other;
        return interactionMillis == that.interactionMillis && schemaVersion == that.schemaVersion
                && reviewId.equals(that.reviewId) && caseId.equals(that.caseId)
                && reviewerId.equals(that.reviewerId) && decision == that.decision
                && verdict == that.verdict && reason == that.reason
                && submittedAt.equals(that.submittedAt);
    }
    @Override public int hashCode() {
        return Objects.hash(reviewId, caseId, reviewerId, decision, verdict, reason,
                submittedAt, interactionMillis, schemaVersion);
    }
}
