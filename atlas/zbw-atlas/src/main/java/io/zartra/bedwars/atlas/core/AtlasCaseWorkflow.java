package io.zartra.bedwars.atlas.core;

import io.zartra.bedwars.atlas.api.AtlasCase;
import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasCaseMetadata;
import io.zartra.bedwars.atlas.api.AtlasCaseStatus;
import io.zartra.bedwars.atlas.api.AtlasEvidenceReference;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Creates, validates and transitions immutable Atlas cases. */
public final class AtlasCaseWorkflow {
    private static final Map<AtlasCaseStatus, EnumSet<AtlasCaseStatus>> EDGES =
            new EnumMap<AtlasCaseStatus, EnumSet<AtlasCaseStatus>>(AtlasCaseStatus.class);
    static {
        EDGES.put(AtlasCaseStatus.CREATED, EnumSet.of(AtlasCaseStatus.OPEN, AtlasCaseStatus.INVALID));
        EDGES.put(AtlasCaseStatus.OPEN, EnumSet.of(AtlasCaseStatus.REVIEWING, AtlasCaseStatus.INVALID));
        EDGES.put(AtlasCaseStatus.REVIEWING,
                EnumSet.of(AtlasCaseStatus.OPEN, AtlasCaseStatus.VERDICT_PENDING, AtlasCaseStatus.INVALID));
        EDGES.put(AtlasCaseStatus.VERDICT_PENDING,
                EnumSet.of(AtlasCaseStatus.REVIEWING, AtlasCaseStatus.RESOLVED, AtlasCaseStatus.INVALID));
        EDGES.put(AtlasCaseStatus.RESOLVED, EnumSet.of(AtlasCaseStatus.ARCHIVED));
        EDGES.put(AtlasCaseStatus.INVALID, EnumSet.of(AtlasCaseStatus.ARCHIVED));
        EDGES.put(AtlasCaseStatus.ARCHIVED, EnumSet.noneOf(AtlasCaseStatus.class));
    }

    /** Creates a validated case containing references only. */
    public AtlasCase create(final AtlasCaseId id, final AtlasCaseMetadata metadata,
                            final AtlasEvidenceReference evidence) {
        return new AtlasCase(id, AtlasCaseStatus.CREATED, metadata,
                Arrays.asList(Objects.requireNonNull(evidence, "evidence")), 0);
    }

    /** Applies one permitted lifecycle edge. */
    public AtlasCase transition(final AtlasCase value, final AtlasCaseStatus target) {
        EnumSet<AtlasCaseStatus> allowed = EDGES.get(Objects.requireNonNull(value, "value").status());
        if (allowed == null || !allowed.contains(target)) {
            throw new IllegalStateException("invalid Atlas case transition");
        }
        return copy(value, target);
    }

    /** Detects exact duplicate source/evidence submissions without reading evidence payloads. */
    public boolean duplicate(final AtlasCase left, final AtlasCase right) {
        if (!left.metadata().sourceReference().equals(right.metadata().sourceReference())) {
            return false;
        }
        List<AtlasEvidenceReference> rightEvidence = right.evidence();
        for (AtlasEvidenceReference evidence : left.evidence()) {
            if (rightEvidence.contains(evidence)) {
                return true;
            }
        }
        return false;
    }

    private static AtlasCase copy(final AtlasCase value, final AtlasCaseStatus status) {
        return new AtlasCase(value.caseId(), status, value.metadata(),
                value.evidence(), value.revision() + 1);
    }
}
