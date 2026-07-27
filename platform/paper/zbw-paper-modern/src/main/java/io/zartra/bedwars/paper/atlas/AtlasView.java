package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import java.util.List;
import java.util.Objects;

/** Sanitized evidence/reviewer/audit GUI projection with no identity-vault data. */
public record AtlasView(AtlasCaseId caseId, List<String> evidenceReferences,
                        String reviewerState, List<String> verdicts,
                        List<String> auditReferences) {
    public AtlasView {
        Objects.requireNonNull(caseId, "caseId");
        evidenceReferences = List.copyOf(evidenceReferences);
        Objects.requireNonNull(reviewerState, "reviewerState");
        verdicts = List.copyOf(verdicts);
        auditReferences = List.copyOf(auditReferences);
    }
}
