package io.zartra.bedwars.atlas.api;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Asynchronous Atlas case persistence port; implementations perform no caller-thread blocking. */
public interface AtlasCaseRepository {
    /** Creates a case if its identity does not already exist. */
    CompletionStage<Boolean> create(AtlasCase atlasCase);
    /** Saves a case only if the authoritative revision equals {@code expectedRevision}. */
    CompletionStage<SaveResult> save(AtlasCase atlasCase, long expectedRevision);
    /** Loads one immutable case snapshot. */
    CompletionStage<Optional<AtlasCase>> find(AtlasCaseId caseId);

    /** Stable optimistic-persistence outcomes. */
    enum SaveResult {
        /** Snapshot was persisted. */ UPDATED,
        /** Authoritative revision differs. */ CONFLICT,
        /** Case does not exist. */ NOT_FOUND
    }
}
