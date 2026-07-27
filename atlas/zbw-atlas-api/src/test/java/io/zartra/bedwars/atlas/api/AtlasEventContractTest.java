package io.zartra.bedwars.atlas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.event.EventMetadata.ThreadContext;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-001/003/005/011 async repository and immutable event tests. */
class AtlasEventContractTest {
    private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");

    @Test void postCommitEventsHaveDeterministicValueSemantics() {
        final AtlasCase atlasCase = atlasCase();
        final AtlasReview review = review(atlasCase.caseId());
        final EventMetadata caseMetadata = metadata("atlas.case.created", 1);
        final AtlasCaseCreatedEvent created = new AtlasCaseCreatedEvent(caseMetadata, atlasCase);
        assertEquals(created, new AtlasCaseCreatedEvent(caseMetadata, atlasCase));
        assertEquals(created.hashCode(), new AtlasCaseCreatedEvent(caseMetadata, atlasCase).hashCode());
        assertEquals(atlasCase, created.atlasCase());
        assertEquals(caseMetadata, created.metadata());
        final EventMetadata reviewMetadata = metadata("atlas.review.submitted", 2);
        final AtlasReviewSubmittedEvent submitted =
                new AtlasReviewSubmittedEvent(reviewMetadata, review);
        assertEquals(submitted, new AtlasReviewSubmittedEvent(reviewMetadata, review));
        assertEquals(review, submitted.review());
        final EventMetadata verdictMetadata = metadata("atlas.verdict.recorded", 3);
        final AtlasVerdictRecordedEvent verdict = new AtlasVerdictRecordedEvent(
                verdictMetadata, atlasCase.caseId(), review.reviewId(),
                review.verdict(), review.reason());
        assertEquals(verdict, new AtlasVerdictRecordedEvent(
                verdictMetadata, atlasCase.caseId(), review.reviewId(),
                review.verdict(), review.reason()));
        assertEquals(review.reviewId(), verdict.reviewId());
        assertEquals(review.verdict(), verdict.verdict());
        assertEquals(review.reason(), verdict.reason());
        assertThrows(IllegalArgumentException.class, () -> new AtlasVerdictRecordedEvent(
                verdictMetadata, atlasCase.caseId(), review.reviewId(),
                ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.OTHER));
    }

    @Test void repositoryPortsAreAsynchronous() throws Exception {
        assertCompletionStage(AtlasCaseRepository.class, "create", AtlasCase.class);
        assertCompletionStage(AtlasCaseRepository.class, "save", AtlasCase.class, long.class);
        assertCompletionStage(AtlasCaseRepository.class, "find", AtlasCaseId.class);
        assertCompletionStage(AtlasReviewRepository.class, "append", AtlasReview.class);
        assertCompletionStage(AtlasReviewRepository.class, "find", AtlasReviewId.class);
        assertCompletionStage(AtlasReviewRepository.class, "findByCase", AtlasCaseId.class);
        assertCompletionStage(AtlasAuditRepository.class, "append", AtlasEvent.class);
    }

    private static void assertCompletionStage(final Class<?> type, final String method,
                                              final Class<?>... parameters) throws Exception {
        final Method found = type.getMethod(method, parameters);
        assertEquals(CompletionStage.class, found.getReturnType());
    }

    private static AtlasCase atlasCase() {
        final AtlasEvidenceReference evidence = AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "report:event-test");
        return new AtlasCase(AtlasCaseId.random(), AtlasCaseStatus.OPEN,
                AtlasApiModelTest.metadata(AtlasCaseSource.REPORT),
                Collections.singletonList(evidence), 0);
    }

    private static AtlasReview review(final AtlasCaseId caseId) {
        return new AtlasReview(AtlasReviewId.random(), caseId, AtlasReviewerId.random(),
                ReviewDecision.VERDICT, ReviewVerdict.NOT_CHEATING,
                ReviewReason.INSUFFICIENT_EVIDENCE, NOW, 50_000, 1);
    }

    private static EventMetadata metadata(final String type, final long sequence) {
        return EventMetadata.of(EventId.random(), EventTypeId.of("atlas", type),
                CorrelationId.random(), NOW, sequence, 1, ThreadContext.APPLICATION_WORKER);
    }
}
