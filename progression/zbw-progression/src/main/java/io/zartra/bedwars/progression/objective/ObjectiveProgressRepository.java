package io.zartra.bedwars.progression.objective;

import java.util.Optional;

/** Persistence-neutral optimistic repository for objective progress. */
public interface ObjectiveProgressRepository {
    /** @return current snapshot when present */
    Optional<ObjectiveProgress> find(ObjectiveId objectiveId, String ownerId);

    /** Saves a snapshot only when its expected prior revision still matches. */
    boolean save(ObjectiveProgress progress, long expectedRevision);
}
