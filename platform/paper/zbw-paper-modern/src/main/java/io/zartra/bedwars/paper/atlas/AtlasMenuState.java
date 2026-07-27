package io.zartra.bedwars.paper.atlas;

import java.util.List;

/** Immutable GUI state built only from sanitized Atlas projections. */
public record AtlasMenuState(Page page, List<AtlasCaseSummary> cases, AtlasView selected,
                             boolean loading, String failure) {
    public AtlasMenuState {
        cases = List.copyOf(cases);
    }
    public enum Page { CASE_LIST, EVIDENCE, REVIEWER, VERDICT, AUDIT }
}
