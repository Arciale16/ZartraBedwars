package io.zartra.bedwars.paper.atlas;

import io.zartra.bedwars.atlas.api.AtlasCaseId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

/** Async Atlas application port consumed by the Paper adapter. */
public interface AtlasPaperPort {
    CompletionStage<List<AtlasCaseSummary>> list(UUID actorId);
    CompletionStage<AtlasView> open(UUID actorId, AtlasCaseId caseId);
    CompletionStage<Boolean> beginReview(UUID actorId, AtlasCaseId caseId);
    CompletionStage<Boolean> submitVerdict(
            UUID actorId, AtlasCaseId caseId, String verdict, String reason);
    CompletionStage<Boolean> finalReview(
            UUID actorId, AtlasCaseId caseId, String disposition);
    CompletionStage<String> diagnostics();
}
