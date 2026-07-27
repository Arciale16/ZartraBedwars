package io.zartra.bedwars.atlas.core;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasReview;
import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Atomic single-node reservation and duplicate-submission domain boundary. */
public final class AtlasReservationBook {
    private final Clock clock;
    private final Map<AtlasCaseId, Reservation> active = new HashMap<AtlasCaseId, Reservation>();
    private final Set<String> submitted = new HashSet<String>();

    public AtlasReservationBook(final Clock clock) { this.clock = Objects.requireNonNull(clock, "clock"); }

    /** Reserves a case until a positive TTL expires. */
    public synchronized Reservation reserve(final AtlasCaseId caseId,
                                            final AtlasReviewerId reviewerId,
                                            final Duration ttl) {
        expire();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (active.containsKey(caseId)) { throw new IllegalStateException("case already reserved"); }
        Reservation value = new Reservation(caseId, reviewerId, clock.instant().plus(ttl));
        active.put(caseId, value);
        return value;
    }

    /** Releases only a reservation owned by the reviewer. */
    public synchronized void release(final AtlasCaseId caseId, final AtlasReviewerId reviewerId) {
        Reservation value = active.get(caseId);
        if (value == null || !value.reviewerId().equals(reviewerId)) {
            throw new IllegalStateException("reservation ownership mismatch");
        }
        active.remove(caseId);
    }

    /** Accepts one review per case/reviewer and consumes its reservation. */
    public synchronized void submit(final AtlasReview review) {
        expire();
        Reservation value = active.get(review.caseId());
        if (value == null || !value.reviewerId().equals(review.reviewerId())) {
            throw new IllegalStateException("active reservation required");
        }
        String key = review.caseId().toString() + ":" + review.reviewerId().toString();
        if (!submitted.add(key)) { throw new IllegalStateException("duplicate review"); }
        active.remove(review.caseId());
    }

    private void expire() {
        Instant now = clock.instant();
        active.values().removeIf(value -> !value.expiresAt().isAfter(now));
    }

    /** Immutable reservation snapshot. */
    public static final class Reservation {
        private final AtlasCaseId caseId;
        private final AtlasReviewerId reviewerId;
        private final Instant expiresAt;
        private Reservation(final AtlasCaseId caseId, final AtlasReviewerId reviewerId,
                            final Instant expiresAt) {
            this.caseId = caseId;
            this.reviewerId = reviewerId;
            this.expiresAt = expiresAt;
        }
        public AtlasCaseId caseId() { return caseId; }
        public AtlasReviewerId reviewerId() { return reviewerId; }
        public Instant expiresAt() { return expiresAt; }
    }
}
