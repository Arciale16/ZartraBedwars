package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.replay.api.ReplayId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Async provider boundary for indexed staff queries and moderation metadata. */
public interface ReplayStaffStore {
    /** Returns deterministic creation-time/replay-ID ordered matches. */
    CompletionStage<List<ReplayStaffRecord>> search(ReplayStaffQuery query);
    /** Finds one replay for authorized staff inspection. */
    CompletionStage<Optional<ReplayStaffRecord>> find(ReplayId replayId);
    /** Idempotently changes the staff-review mark. */
    CompletionStage<Boolean> mark(ReplayId replayId, boolean marked);
    /** Removes only provider-validated invalid replay data. */
    CompletionStage<Boolean> removeInvalid(ReplayId replayId);
}
