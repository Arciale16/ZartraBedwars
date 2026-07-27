package io.zartra.bedwars.atlas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-004/005/006 privacy and review validation tests. */
class AtlasReviewPrivacyTest {
    private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");

    @Test void reviewEqualityAndDecisionRulesAreDeterministic() {
        final AtlasReview review = review(ReviewDecision.VERDICT,
                ReviewVerdict.EVIDENTLY_CHEATING, ReviewReason.REACH);
        final AtlasReview copy = new AtlasReview(review.reviewId(), review.caseId(),
                review.reviewerId(), review.decision(), review.verdict(), review.reason(),
                review.submittedAt(), review.interactionMillis(), review.schemaVersion());
        assertEquals(review, copy);
        assertEquals(review.hashCode(), copy.hashCode());
        assertEquals(ReviewDecision.VERDICT, review.decision());
        assertEquals(42_000, review.interactionMillis());
        assertThrows(IllegalArgumentException.class, () -> review(
                ReviewDecision.VERDICT, ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.OTHER));
        assertThrows(IllegalArgumentException.class, () -> review(
                ReviewDecision.SKIP, ReviewVerdict.NOT_CHEATING, ReviewReason.OTHER));
        assertThrows(IllegalArgumentException.class, () -> review(
                ReviewDecision.SKIP, ReviewVerdict.UNABLE_TO_REVIEW, ReviewReason.REACH));
        assertThrows(IllegalArgumentException.class, () -> new AtlasReview(
                AtlasReviewId.random(), AtlasCaseId.random(), AtlasReviewerId.random(),
                ReviewDecision.ABSTAIN, ReviewVerdict.UNABLE_TO_REVIEW,
                ReviewReason.INSUFFICIENT_EVIDENCE, NOW, -1, 1));
    }

    @Test void identityDefaultsAnonymousAndRevealRequiresExplicitAuthorization() {
        final AtlasCaseId caseId = AtlasCaseId.random();
        final AtlasReviewerId staffId = AtlasReviewerId.random();
        final AnonymizedIdentity alias = new AnonymizedIdentity("CASE-ABC123");
        final IdentityProjection anonymous = IdentityProjection.anonymous(caseId, alias);
        assertFalse(anonymous.revealed());
        assertFalse(anonymous.identityVaultReference().isPresent());
        assertEquals("CASE-ABC123", anonymous.anonymousIdentity().alias());
        assertEquals(alias, new AnonymizedIdentity("CASE-ABC123"));
        assertEquals(alias.hashCode(), new AnonymizedIdentity("CASE-ABC123").hashCode());
        final IdentityRevealRequest denied =
                IdentityRevealRequest.denied(caseId, staffId, "auth:denied");
        assertFalse(denied.authorized());
        assertThrows(SecurityException.class, () -> anonymous.reveal(denied, "vault:one"));
        final IdentityRevealRequest wrongCase = IdentityRevealRequest.authorized(
                AtlasCaseId.random(), staffId, "auth:wrong-case");
        assertThrows(SecurityException.class, () -> anonymous.reveal(wrongCase, "vault:one"));
        final IdentityRevealRequest authorized =
                IdentityRevealRequest.authorized(caseId, staffId, "auth:approved");
        final IdentityProjection revealed = anonymous.reveal(authorized, "vault:subject-18");
        assertTrue(revealed.revealed());
        assertEquals("vault:subject-18", revealed.identityVaultReference().get());
        assertEquals(caseId, authorized.caseId());
        assertEquals(staffId, authorized.requesterId());
        assertEquals("auth:approved", authorized.authorizationReference());
    }

    @Test void privacyBoundaryRejectsNamesRawIdentifiersAndMalformedVaultKeys() {
        assertThrows(IllegalArgumentException.class, () -> new AnonymizedIdentity("PlayerName"));
        assertThrows(IllegalArgumentException.class, () -> IdentityRevealRequest.authorized(
                AtlasCaseId.random(), AtlasReviewerId.random(), "human readable justification"));
        final IdentityProjection anonymous = IdentityProjection.anonymous(
                AtlasCaseId.random(), new AnonymizedIdentity("CASE-XYZ789"));
        final IdentityRevealRequest authorized = IdentityRevealRequest.authorized(
                anonymous.caseId(), AtlasReviewerId.of(UUID.randomUUID()), "auth:1");
        assertThrows(IllegalArgumentException.class, () -> anonymous.reveal(
                authorized, "00000000-0000-0000-0000-000000000018 raw"));
    }

    private static AtlasReview review(final ReviewDecision decision,
                                      final ReviewVerdict verdict,
                                      final ReviewReason reason) {
        return new AtlasReview(AtlasReviewId.of(UUID.fromString(
                "00000000-0000-0000-0000-000000000018")), AtlasCaseId.random(),
                AtlasReviewerId.random(), decision, verdict, reason, NOW, 42_000, 1);
    }
}
