package io.zartra.bedwars.progression.quest;

import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.util.List;
import java.util.Optional;

/** Persistence-neutral quest assignment repository. */
public interface QuestAssignmentRepository {
    /** @return an assignment when present */
    Optional<QuestAssignment> find(PlayerProgressionId playerId, QuestId questId);

    /** @return bounded immutable-view assignments for a player */
    List<QuestAssignment> findByPlayer(PlayerProgressionId playerId, int limit);

    /** Saves a snapshot only when its expected prior revision matches. */
    boolean save(QuestAssignment assignment, long expectedRevision);
}
