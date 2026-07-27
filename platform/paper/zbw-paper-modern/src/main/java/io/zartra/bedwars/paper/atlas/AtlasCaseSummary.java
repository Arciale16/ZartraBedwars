package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import io.zartra.bedwars.atlas.api.AtlasCaseStatus;
import java.util.Objects;

/** Identity-free Atlas case-list projection. */
public record AtlasCaseSummary(AtlasCaseId caseId, AtlasCaseStatus status, String category) {
    public AtlasCaseSummary {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(category, "category");
    }
}
