package io.zartra.bedwars.atlas.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable Atlas case snapshot with ordered evidence references (ZBW-ATLAS-001/003). */
public final class AtlasCase {
    private final AtlasCaseId caseId;
    private final AtlasCaseStatus status;
    private final AtlasCaseMetadata metadata;
    private final List<AtlasEvidenceReference> evidence;
    private final long revision;

    /** Restores a validated immutable case snapshot from an authoritative boundary. */
    public AtlasCase(final AtlasCaseId caseId, final AtlasCaseStatus status,
                     final AtlasCaseMetadata metadata, final List<AtlasEvidenceReference> evidence,
                     final long revision) {
        if (revision < 0) { throw new IllegalArgumentException("revision must be non-negative"); }
        final List<AtlasEvidenceReference> copy = new ArrayList<AtlasEvidenceReference>(
                Objects.requireNonNull(evidence, "evidence"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("case requires non-null evidence");
        }
        final Set<AtlasEvidenceId> identities = new HashSet<AtlasEvidenceId>();
        for (AtlasEvidenceReference reference : copy) {
            if (!identities.add(reference.evidenceId())) {
                throw new IllegalArgumentException("duplicate evidence identity");
            }
        }
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.status = Objects.requireNonNull(status, "status");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.evidence = Collections.unmodifiableList(copy);
        this.revision = revision;
    }

    /** Returns case identity. */ public AtlasCaseId caseId() { return caseId; }
    /** Returns lifecycle status. */ public AtlasCaseStatus status() { return status; }
    /** Returns identity-free origin metadata. */ public AtlasCaseMetadata metadata() { return metadata; }
    /** Returns immutable, insertion-ordered evidence references. */
    public List<AtlasEvidenceReference> evidence() { return evidence; }
    /** Returns optimistic persistence revision. */ public long revision() { return revision; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasCase)) { return false; }
        final AtlasCase that = (AtlasCase) other;
        return revision == that.revision && caseId.equals(that.caseId) && status == that.status
                && metadata.equals(that.metadata) && evidence.equals(that.evidence);
    }
    @Override public int hashCode() {
        return Objects.hash(caseId, status, metadata, evidence, revision);
    }
}
