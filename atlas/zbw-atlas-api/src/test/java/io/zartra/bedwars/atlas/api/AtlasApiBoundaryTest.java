package io.zartra.bedwars.atlas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.zartra.bedwars.replay.api.ReplayId;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-001/003/004/005/006 exhaustive API boundary tests. */
class AtlasApiBoundaryTest {
    private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");

    @Test void metadataRejectsEveryUnsafeSerializationBoundary() {
        assertThrows(IllegalArgumentException.class, () -> metadata(-1, 1, "combat", "source:1"));
        assertThrows(IllegalArgumentException.class, () -> metadata(101, 1, "combat", "source:1"));
        assertThrows(IllegalArgumentException.class, () -> metadata(1, 0, "combat", "source:1"));
        assertThrows(NullPointerException.class, () -> new AtlasCaseMetadata(
                null, NOW, "combat", "source:1", 1, 1));
        assertThrows(NullPointerException.class, () -> new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, null, "combat", "source:1", 1, 1));
        for (String invalid : new String[] {null, "", " ", "human readable", repeat('x', 81)}) {
            assertThrows(IllegalArgumentException.class, () -> metadata(1, 1, invalid, "source:1"));
        }
        for (String invalid : new String[] {null, "", " ", "raw evidence", repeat('x', 161)}) {
            assertThrows(IllegalArgumentException.class, () -> metadata(1, 1, "combat", invalid));
        }
        AtlasCaseMetadata value = metadata(0, 1, "combat", "source:1");
        assertEquals(AtlasCaseSource.REPORT, value.source());
        assertEquals(NOW, value.createdAt());
        assertEquals("combat", value.category());
        assertEquals("source:1", value.sourceReference());
        assertEquals(0, value.priority());
        assertEquals(1, value.schemaVersion());
        assertNotEquals(value, null);
        assertNotEquals(value, "metadata");
    }

    @Test void evidenceRejectsMalformedAndHasDeterministicValueSemantics() {
        AtlasEvidenceId evidenceId = AtlasEvidenceId.random();
        ReplayId replayId = ReplayId.random();
        AtlasEvidenceReference replay =
                AtlasEvidenceReference.replay(evidenceId, replayId, 0, 20);
        AtlasEvidenceReference replayCopy =
                AtlasEvidenceReference.replay(evidenceId, replayId, 0, 20);
        assertEquals(replay, replayCopy);
        assertEquals(replay.hashCode(), replayCopy.hashCode());
        assertNotEquals(replay, null);
        assertNotEquals(replay, "evidence");
        assertNotEquals(replay, AtlasEvidenceReference.replay(evidenceId, replayId, 1, 20));
        assertThrows(IllegalArgumentException.class, () ->
                AtlasEvidenceReference.replay(evidenceId, replayId, -1, 0));
        assertThrows(NullPointerException.class, () ->
                AtlasEvidenceReference.replay(null, replayId, 0, 0));
        assertThrows(NullPointerException.class, () ->
                AtlasEvidenceReference.replay(evidenceId, null, 0, 0));
        for (String invalid : new String[] {null, "", " ", "raw report", repeat('x', 161)}) {
            assertThrows(IllegalArgumentException.class, () ->
                    AtlasEvidenceReference.report(evidenceId, invalid));
        }
    }

    @Test void privacyValuesAreDeterministicAndFailClosed() {
        AtlasCaseId caseId = AtlasCaseId.random();
        AtlasReviewerId reviewerId = AtlasReviewerId.random();
        AnonymizedIdentity alias = new AnonymizedIdentity("CASE-ABC123");
        IdentityProjection anonymous = IdentityProjection.anonymous(caseId, alias);
        assertEquals(anonymous, IdentityProjection.anonymous(caseId, alias));
        assertEquals(anonymous.hashCode(), IdentityProjection.anonymous(caseId, alias).hashCode());
        assertNotEquals(anonymous, null);
        assertNotEquals(anonymous, "projection");
        IdentityRevealRequest request =
                IdentityRevealRequest.authorized(caseId, reviewerId, "auth:1");
        assertEquals(request, IdentityRevealRequest.authorized(caseId, reviewerId, "auth:1"));
        assertEquals(request.hashCode(),
                IdentityRevealRequest.authorized(caseId, reviewerId, "auth:1").hashCode());
        assertNotEquals(request, IdentityRevealRequest.denied(caseId, reviewerId, "auth:1"));
        assertNotEquals(request, null);
        assertNotEquals(request, "request");
        assertThrows(NullPointerException.class, () -> anonymous.reveal(null, "vault:1"));
        for (String invalid : new String[] {null, "", " ", "raw identity", repeat('x', 161)}) {
            assertThrows(IllegalArgumentException.class, () -> anonymous.reveal(request, invalid));
        }
        assertThrows(NullPointerException.class, () -> IdentityProjection.anonymous(null, alias));
        assertThrows(NullPointerException.class, () -> IdentityProjection.anonymous(caseId, null));
    }

    @Test void reviewsAndCasesRejectNullOrInvalidState() {
        AtlasEvidenceReference evidence = AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "report:1");
        assertThrows(NullPointerException.class, () -> new AtlasCase(
                null, AtlasCaseStatus.OPEN, AtlasApiModelTest.metadata(AtlasCaseSource.REPORT),
                Collections.singletonList(evidence), 0));
        assertThrows(NullPointerException.class, () -> new AtlasCase(
                AtlasCaseId.random(), null, AtlasApiModelTest.metadata(AtlasCaseSource.REPORT),
                Collections.singletonList(evidence), 0));
        assertThrows(NullPointerException.class, () -> new AtlasCase(
                AtlasCaseId.random(), AtlasCaseStatus.OPEN, null,
                Collections.singletonList(evidence), 0));
        assertThrows(NullPointerException.class, () -> review(null, ReviewDecision.ABSTAIN,
                ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.OTHER, 1));
        assertThrows(NullPointerException.class, () -> review(AtlasReviewId.random(), null,
                ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.OTHER, 1));
        assertThrows(IllegalArgumentException.class, () -> review(AtlasReviewId.random(),
                ReviewDecision.ABSTAIN, ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.OTHER, 0));
        AtlasReview skip = review(AtlasReviewId.random(), ReviewDecision.SKIP,
                ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.EVIDENCE_ERROR, 1);
        assertEquals(ReviewDecision.SKIP, skip.decision());
        assertFalse(skip.equals(null));
        assertFalse(skip.equals("review"));
    }

    private static AtlasCaseMetadata metadata(final int priority, final int version,
                                              final String category, final String reference) {
        return new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, category, reference, priority, version);
    }

    private static AtlasReview review(final AtlasReviewId reviewId,
                                      final ReviewDecision decision,
                                      final ReviewVerdict verdict,
                                      final ReviewReason reason,
                                      final int version) {
        return new AtlasReview(reviewId, AtlasCaseId.random(), AtlasReviewerId.random(),
                decision, verdict, reason, NOW, 1, version);
    }

    private static String repeat(final char value, final int count) {
        char[] characters = new char[count];
        java.util.Arrays.fill(characters, value);
        return new String(characters);
    }
}
