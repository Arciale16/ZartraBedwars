package io.zartra.bedwars.atlas.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.atlas.api.AtlasCase;
import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasCaseMetadata;
import io.zartra.bedwars.atlas.api.AtlasCaseSource;
import io.zartra.bedwars.atlas.api.AtlasCaseStatus;
import io.zartra.bedwars.atlas.api.AtlasEvidenceId;
import io.zartra.bedwars.atlas.api.AtlasEvidenceReference;
import io.zartra.bedwars.atlas.api.AtlasReview;
import io.zartra.bedwars.atlas.api.AtlasReviewId;
import io.zartra.bedwars.atlas.api.AtlasReviewerId;
import io.zartra.bedwars.atlas.api.ReviewDecision;
import io.zartra.bedwars.atlas.api.ReviewReason;
import io.zartra.bedwars.atlas.api.ReviewVerdict;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-001/002/003/005/006/012 Atlas core domain evidence. */
class AtlasCoreDomainTest {
    private static final Instant NOW = Instant.parse("2026-07-27T12:00:00Z");

    @Test void lifecycleAndDuplicatesAreDeterministic() {
        AtlasCaseWorkflow workflow = new AtlasCaseWorkflow();
        AtlasEvidenceReference evidence = AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "report:18");
        AtlasCase value = workflow.create(AtlasCaseId.random(), metadata(), evidence);
        assertEquals(AtlasCaseStatus.CREATED, value.status());
        value = workflow.transition(value, AtlasCaseStatus.OPEN);
        value = workflow.transition(value, AtlasCaseStatus.REVIEWING);
        value = workflow.transition(value, AtlasCaseStatus.VERDICT_PENDING);
        value = workflow.transition(value, AtlasCaseStatus.RESOLVED);
        value = workflow.transition(value, AtlasCaseStatus.ARCHIVED);
        assertEquals(5, value.revision());
        final AtlasCase archived = value;
        assertThrows(IllegalStateException.class,
                () -> workflow.transition(archived, AtlasCaseStatus.OPEN));
        AtlasCase duplicate = workflow.create(AtlasCaseId.random(), metadata(), evidence);
        assertFalse(workflow.duplicate(value, workflow.create(
                AtlasCaseId.random(), otherMetadata(), evidence)));
        assertEquals(true, workflow.duplicate(value, duplicate));
    }

    @Test void eligibilityRejectsAllConflicts() {
        ReviewerEligibilityPolicy policy = new ReviewerEligibilityPolicy();
        AtlasReviewerId reviewer = AtlasReviewerId.random();
        UUID subject = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        assertEquals(ReviewerEligibilityPolicy.Result.INSUFFICIENT_EXPERIENCE,
                policy.evaluate(reviewer, subject, player, Collections.<UUID>emptySet(), 2, 3, false));
        assertEquals(ReviewerEligibilityPolicy.Result.SELF_CONFLICT,
                policy.evaluate(reviewer, subject, subject, Collections.<UUID>emptySet(), 3, 3, false));
        assertEquals(ReviewerEligibilityPolicy.Result.PARTY_CONFLICT,
                policy.evaluate(reviewer, subject, player, Collections.singleton(player), 3, 3, false));
        assertEquals(ReviewerEligibilityPolicy.Result.DUPLICATE_ASSIGNMENT,
                policy.evaluate(reviewer, subject, player, Collections.<UUID>emptySet(), 3, 3, true));
        assertEquals(ReviewerEligibilityPolicy.Result.ELIGIBLE,
                policy.evaluate(reviewer, subject, player, Collections.<UUID>emptySet(), 3, 3, false));
    }

    @Test void reservationsPreventConcurrencyAndDuplicateReview() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        AtlasReservationBook book = new AtlasReservationBook(clock);
        AtlasCaseId caseId = AtlasCaseId.random();
        AtlasReviewerId reviewer = AtlasReviewerId.random();
        book.reserve(caseId, reviewer, Duration.ofMinutes(5));
        assertThrows(IllegalStateException.class,
                () -> book.reserve(caseId, AtlasReviewerId.random(), Duration.ofMinutes(5)));
        book.submit(review(caseId, reviewer));
        book.reserve(caseId, reviewer, Duration.ofMinutes(5));
        assertThrows(IllegalStateException.class, () -> book.submit(review(caseId, reviewer)));
        book.release(caseId, reviewer);
        assertThrows(IllegalArgumentException.class,
                () -> book.reserve(caseId, reviewer, Duration.ZERO));
    }

    @Test void verdictIsAdvisoryAndAuditImmutable() {
        AtlasCaseId caseId = AtlasCaseId.random();
        AtlasReview first = review(caseId, AtlasReviewerId.random());
        AtlasReview second = review(caseId, AtlasReviewerId.random());
        VerdictAggregationPolicy.Result result =
                new VerdictAggregationPolicy().aggregate(Arrays.asList(first, second), 0.6);
        assertEquals(ReviewVerdict.NOT_CHEATING, result.verdict());
        assertEquals(1.0, result.confidence());
        assertFalse(result.permanentPunishmentAuthorized());
        AtlasAuditRecord audit = new AtlasAuditRecord(
                "reviewer:1", "review.submit", "case:1", NOW, "accepted", "rev:0", "rev:1");
        assertEquals(audit, new AtlasAuditRecord(
                "reviewer:1", "review.submit", "case:1", NOW, "accepted", "rev:0", "rev:1"));
        assertThrows(IllegalArgumentException.class, () -> new AtlasAuditRecord(
                "human actor", "x", "y", NOW, "ok", "a", "b"));
    }

    private static AtlasReview review(final AtlasCaseId caseId, final AtlasReviewerId reviewer) {
        return new AtlasReview(AtlasReviewId.random(), caseId, reviewer,
                ReviewDecision.VERDICT, ReviewVerdict.NOT_CHEATING,
                ReviewReason.INSUFFICIENT_EVIDENCE, NOW, 60_000, 1);
    }

    private static AtlasCaseMetadata metadata() {
        return new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, "combat", "source:18", 50, 1);
    }

    private static AtlasCaseMetadata otherMetadata() {
        return new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, "combat", "source:other", 50, 1);
    }
}
