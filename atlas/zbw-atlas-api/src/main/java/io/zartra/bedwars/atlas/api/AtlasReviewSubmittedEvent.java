package io.zartra.bedwars.atlas.api;

import io.zartra.bedwars.api.event.EventMetadata;
import java.util.Objects;

/** Immutable post-commit event emitted after a review submission is persisted. */
public final class AtlasReviewSubmittedEvent implements AtlasEvent {
    private final EventMetadata metadata;
    private final AtlasReview review;

    /** Creates a review-submitted event snapshot. */
    public AtlasReviewSubmittedEvent(final EventMetadata metadata, final AtlasReview review) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.review = Objects.requireNonNull(review, "review");
    }
    @Override public EventMetadata metadata() { return metadata; }
    /** Returns the immutable submitted review. */ public AtlasReview review() { return review; }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasReviewSubmittedEvent)) { return false; }
        final AtlasReviewSubmittedEvent that = (AtlasReviewSubmittedEvent) other;
        return metadata.equals(that.metadata) && review.equals(that.review);
    }
    @Override public int hashCode() { return Objects.hash(metadata, review); }
}
