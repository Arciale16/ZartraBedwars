package io.zartra.bedwars.progression.runtime;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.progression.achievement.AchievementId;
import io.zartra.bedwars.progression.achievement.AchievementProgress;
import io.zartra.bedwars.progression.challenge.ChallengeId;
import io.zartra.bedwars.progression.challenge.ChallengeProgress;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import io.zartra.bedwars.progression.objective.ObjectiveId;
import io.zartra.bedwars.progression.objective.ObjectiveRuntimeState;
import io.zartra.bedwars.progression.pass.SeasonId;
import io.zartra.bedwars.progression.pass.SeasonProgress;
import io.zartra.bedwars.progression.quest.QuestAssignment;
import io.zartra.bedwars.progression.quest.QuestId;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Instant;
import java.util.Optional;

/** Transaction-aware durable port for all M13 runtime state and inbox claims. */
public interface M13StateRepository {
    /** Claims an authoritative event once in the caller-owned transaction. */
    Result<Boolean> claimEvent(UnitOfWork unitOfWork, IdempotencyKey key, Instant occurredAt);
    /** Reads objective execution state. */
    Result<Optional<ObjectiveRuntimeState>> findObjective(UnitOfWork unitOfWork,
            PlayerProgressionId playerId, ObjectiveId objectiveId);
    /** Saves objective state with optimistic revision validation. */
    Result<ObjectiveRuntimeState> saveObjective(UnitOfWork unitOfWork,
            ObjectiveRuntimeState state, long expectedRevision);
    /** Reads quest assignment state. */
    Result<Optional<QuestAssignment>> findQuest(UnitOfWork unitOfWork,
            PlayerProgressionId playerId, QuestId questId);
    /** Saves quest assignment state with optimistic revision validation. */
    Result<QuestAssignment> saveQuest(UnitOfWork unitOfWork, QuestAssignment state,
            long expectedRevision);
    /** Reads achievement state. */
    Result<Optional<AchievementProgress>> findAchievement(UnitOfWork unitOfWork,
            PlayerProgressionId playerId, AchievementId achievementId);
    /** Saves achievement state with optimistic revision validation. */
    Result<AchievementProgress> saveAchievement(UnitOfWork unitOfWork,
            AchievementProgress state, long expectedRevision);
    /** Reads challenge state. */
    Result<Optional<ChallengeProgress>> findChallenge(UnitOfWork unitOfWork,
            PlayerProgressionId playerId, ChallengeId challengeId);
    /** Saves challenge state with optimistic revision validation. */
    Result<ChallengeProgress> saveChallenge(UnitOfWork unitOfWork,
            ChallengeProgress state, long expectedRevision);
    /** Reads battle-pass season state. */
    Result<Optional<SeasonProgress>> findSeason(UnitOfWork unitOfWork,
            PlayerProgressionId playerId, SeasonId seasonId);
    /** Saves season state with optimistic revision validation. */
    Result<SeasonProgress> saveSeason(UnitOfWork unitOfWork, SeasonProgress state,
            long expectedRevision);
}
