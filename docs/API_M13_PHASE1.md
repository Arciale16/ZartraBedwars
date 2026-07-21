# M13 Phase 1 API

## Public neutral packages

| Package | Contracts |
|---|---|
| `io.zartra.bedwars.progression.objective` | `ObjectiveId`, `ObjectiveEventType`, `ObjectiveFilter`, `ObjectiveDefinition`, `ObjectiveProgress`, `ObjectiveProgressRepository` |
| `io.zartra.bedwars.progression.quest` | `QuestId`, `QuestDefinition`, `QuestAssignment`, `QuestAssignmentRepository` |
| `io.zartra.bedwars.progression.achievement` | `AchievementId`, `AchievementDefinition` and immutable tier values |
| `io.zartra.bedwars.progression.challenge` | `ChallengeId`, `ChallengeDefinition` and typed variants |
| `io.zartra.bedwars.progression.pass` | `SeasonId`, `BattlePassDefinition` and free/premium tier values |
| `io.zartra.bedwars.progression.catalog` | `M13Catalog` deterministic immutable definition graph |

## Contract rules

- Public values are Java 8 compatible, immutable and null rejecting.
- Definition versions are positive and stable IDs are namespaced.
- Objective progress is monotonic, target bounded and revisioned; durable non-consecutive event
  deduplication remains coupled to the existing M12/M04 idempotency transaction in a later phase.
- Repository ports are synchronous contracts only; callers must execute concrete persistence off the
  Minecraft owner thread.
- No type exposes Bukkit, Paper, NMS, JDBC, filesystem, Redis, proxy or runtime configuration classes.
- Additive public API changes remain subject to strict JavaDoc and binary compatibility validation.
