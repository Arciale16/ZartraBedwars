package io.zartra.bedwars.atlas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.replay.api.ReplayId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ZBW-ATLAS-001/003/006 immutable identity, case and evidence contract tests. */
class AtlasApiModelTest {
    private static final Instant NOW = Instant.parse("2026-07-27T10:00:00Z");
    private static final UUID FIXED = UUID.fromString("00000000-0000-0000-0000-000000000018");

    @Test void identifiersRoundTripWithDeterministicEqualityAndOrdering() {
        final AtlasCaseId caseId = AtlasCaseId.of(FIXED);
        assertEquals(caseId, AtlasCaseId.parse(caseId.toString()));
        assertEquals(caseId.hashCode(), AtlasCaseId.parse(caseId.toString()).hashCode());
        assertEquals(0, caseId.compareTo(AtlasCaseId.parse(caseId.toString())));
        assertEquals(FIXED, caseId.value());
        assertNotEquals(caseId, AtlasCaseId.random());
        final AtlasReviewId reviewId = AtlasReviewId.of(FIXED);
        assertEquals(reviewId, AtlasReviewId.parse(reviewId.toString()));
        assertEquals(0, reviewId.compareTo(AtlasReviewId.parse(reviewId.toString())));
        assertEquals(FIXED, reviewId.value());
        assertNotEquals(reviewId, AtlasReviewId.random());
        final AtlasReviewerId reviewerId = AtlasReviewerId.of(FIXED);
        assertEquals(reviewerId, AtlasReviewerId.parse(reviewerId.toString()));
        assertEquals(0, reviewerId.compareTo(AtlasReviewerId.parse(reviewerId.toString())));
        assertEquals(FIXED, reviewerId.value());
        assertNotEquals(reviewerId, AtlasReviewerId.random());
        final AtlasEvidenceId evidenceId = AtlasEvidenceId.of(FIXED);
        assertEquals(evidenceId, AtlasEvidenceId.parse(evidenceId.toString()));
        assertEquals(0, evidenceId.compareTo(AtlasEvidenceId.parse(evidenceId.toString())));
        assertEquals(FIXED, evidenceId.value());
        assertNotEquals(evidenceId, AtlasEvidenceId.random());
        assertThrows(NullPointerException.class, () -> AtlasCaseId.of(null));
        assertThrows(IllegalArgumentException.class, () -> AtlasReviewId.parse("not-a-uuid"));
    }

    @Test void caseAndEvidenceAreImmutableAndSerializationSafe() {
        final AtlasEvidenceReference replay = AtlasEvidenceReference.replay(
                AtlasEvidenceId.random(), ReplayId.random(), 20, 90);
        final AtlasEvidenceReference report = AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "report:42");
        final AtlasEvidenceReference signal = AtlasEvidenceReference.internalSignal(
                AtlasEvidenceId.random(), "signal:7");
        final List<AtlasEvidenceReference> mutable =
                new ArrayList<AtlasEvidenceReference>(Arrays.asList(replay, report, signal));
        final AtlasCaseMetadata metadata = metadata(AtlasCaseSource.REPORT);
        final AtlasCase atlasCase = new AtlasCase(
                AtlasCaseId.of(FIXED), AtlasCaseStatus.OPEN, metadata, mutable, 0);
        mutable.clear();
        assertEquals(3, atlasCase.evidence().size());
        assertThrows(UnsupportedOperationException.class, () -> atlasCase.evidence().clear());
        assertEquals(ReplayId.class, replay.replayId().get().getClass());
        assertFalse(replay.externalReference().isPresent());
        assertEquals("report:42", report.externalReference().get());
        assertFalse(report.replayId().isPresent());
        assertEquals(20, replay.startMillis());
        assertEquals(90, replay.endMillis());
        assertEquals(AtlasEvidenceReference.Type.INTERNAL_SIGNAL, signal.type());
        assertEquals(metadata, metadata(AtlasCaseSource.REPORT));
        assertEquals(metadata.hashCode(), metadata(AtlasCaseSource.REPORT).hashCode());
        final AtlasCase copy = new AtlasCase(atlasCase.caseId(), atlasCase.status(),
                metadata, atlasCase.evidence(), atlasCase.revision());
        assertEquals(atlasCase, copy);
        assertEquals(atlasCase.hashCode(), copy.hashCode());
    }

    @Test void malformedCaseAndEvidenceBoundariesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, "bad category", "report:1", 50, 1));
        assertThrows(IllegalArgumentException.class, () -> new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, "combat", "report:1", 101, 1));
        assertThrows(IllegalArgumentException.class, () -> new AtlasCaseMetadata(
                AtlasCaseSource.REPORT, NOW, "combat", "report:1", 50, 0));
        assertThrows(IllegalArgumentException.class, () -> AtlasEvidenceReference.replay(
                AtlasEvidenceId.random(), ReplayId.random(), 10, 9));
        assertThrows(IllegalArgumentException.class, () -> AtlasEvidenceReference.report(
                AtlasEvidenceId.random(), "raw report text is forbidden"));
        assertThrows(IllegalArgumentException.class, () -> new AtlasCase(
                AtlasCaseId.random(), AtlasCaseStatus.OPEN, metadata(AtlasCaseSource.INTERNAL_SIGNAL),
                Collections.<AtlasEvidenceReference>emptyList(), 0));
        final AtlasEvidenceReference evidence = AtlasEvidenceReference.internalSignal(
                AtlasEvidenceId.random(), "signal:1");
        assertThrows(IllegalArgumentException.class, () -> new AtlasCase(
                AtlasCaseId.random(), AtlasCaseStatus.OPEN, metadata(AtlasCaseSource.INTERNAL_SIGNAL),
                Arrays.asList(evidence, evidence), 0));
        assertThrows(IllegalArgumentException.class, () -> new AtlasCase(
                AtlasCaseId.random(), AtlasCaseStatus.OPEN, metadata(AtlasCaseSource.INTERNAL_SIGNAL),
                Collections.singletonList(evidence), -1));
        assertTrue(AtlasCaseStatus.values().length >= 6);
        assertEquals(3, AtlasCaseSource.values().length);
    }

    static AtlasCaseMetadata metadata(final AtlasCaseSource source) {
        return new AtlasCaseMetadata(source, NOW, "combat", "source:18", 50, 1);
    }
}
