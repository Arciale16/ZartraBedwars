package io.zartra.bedwars.atlas.core;

import io.zartra.bedwars.atlas.api.AtlasReview;
import io.zartra.bedwars.atlas.api.ReviewDecision;
import io.zartra.bedwars.atlas.api.ReviewVerdict;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic community-verdict aggregation that never authorizes punishment. */
public final class VerdictAggregationPolicy {
    /** Aggregates substantive reviews and returns staff-pending advice only. */
    public Result aggregate(final List<AtlasReview> reviews, final double minimumConfidence) {
        if (reviews == null || reviews.isEmpty() || minimumConfidence < 0 || minimumConfidence > 1) {
            throw new IllegalArgumentException("invalid aggregation input");
        }
        Map<ReviewVerdict, Integer> counts = new EnumMap<ReviewVerdict, Integer>(ReviewVerdict.class);
        int substantive = 0;
        for (AtlasReview review : reviews) {
            Objects.requireNonNull(review, "review");
            if (review.decision() == ReviewDecision.VERDICT) {
                substantive++;
                counts.put(review.verdict(), counts.getOrDefault(review.verdict(), 0) + 1);
            }
        }
        if (substantive == 0) { return new Result(ReviewVerdict.UNABLE_TO_REVIEW, 0); }
        ReviewVerdict selected = null;
        int votes = 0;
        for (Map.Entry<ReviewVerdict, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > votes) {
                selected = entry.getKey();
                votes = entry.getValue();
            }
        }
        double confidence = (double) votes / substantive;
        return new Result(confidence >= minimumConfidence
                ? selected : ReviewVerdict.UNABLE_TO_REVIEW, confidence);
    }

    /** Immutable advisory result; staff authority is always required. */
    public static final class Result {
        private final ReviewVerdict verdict;
        private final double confidence;
        private Result(final ReviewVerdict verdict, final double confidence) {
            this.verdict = verdict;
            this.confidence = confidence;
        }
        public ReviewVerdict verdict() { return verdict; }
        public double confidence() { return confidence; }
        /** Community output can never directly authorize permanent punishment. */
        public boolean permanentPunishmentAuthorized() { return false; }
    }
}
