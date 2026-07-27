package io.zartra.bedwars.atlas.core;

import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Fail-closed reviewer eligibility and conflict policy. */
public final class ReviewerEligibilityPolicy {
    /** Evaluates experience, self-review, party and previous-assignment conflicts. */
    public Result evaluate(final AtlasReviewerId reviewer, final UUID subject,
                           final UUID reviewerPlayer, final Set<UUID> subjectParty,
                           final long completedReviews, final long minimumReviews,
                           final boolean alreadyAssigned) {
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(reviewerPlayer, "reviewerPlayer");
        Objects.requireNonNull(subjectParty, "subjectParty");
        if (completedReviews < minimumReviews) { return Result.INSUFFICIENT_EXPERIENCE; }
        if (subject.equals(reviewerPlayer)) { return Result.SELF_CONFLICT; }
        if (subjectParty.contains(reviewerPlayer)) { return Result.PARTY_CONFLICT; }
        if (alreadyAssigned) { return Result.DUPLICATE_ASSIGNMENT; }
        return Result.ELIGIBLE;
    }

    /** Stable eligibility outcomes. */
    public enum Result {
        ELIGIBLE, INSUFFICIENT_EXPERIENCE, SELF_CONFLICT, PARTY_CONFLICT, DUPLICATE_ASSIGNMENT
    }
}
