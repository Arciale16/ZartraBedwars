# M13 Phase 2 API

## Added neutral contracts

| Package | Public surface |
|---|---|
| `progression.objective` | `ObjectiveEvent`, `ObjectiveRuntimeState`, `ObjectiveExecutionEngine` and immutable evaluation evidence |
| `progression.integration` | `M13EventAdapter` and explicit source mapping rules |
| `progression.runtime` | `M13StateRepository`, `M13ProjectionEngine`, `M13RewardIntent` |
| `progression.quest` | `QuestRuntime` lifecycle, chain and reward-intent policy |
| `progression.achievement` | `AchievementProgress`, `AchievementRuntime` |
| `progression.challenge` | `ChallengeProgress`, `ChallengeRuntime` |
| `progression.pass` | `SeasonProgress`, `BattlePassRuntime` |
| `storage.sql` | `JdbcM13StateRepository`, `M13SchemaMigrator` |

## Contract guarantees

- Neutral public contracts target Java 8 and expose no Bukkit, Paper, NMS, JDBC implementation,
  filesystem, Redis or proxy type.
- Runtime objects are immutable. Collections are defensive and inputs are null/bounds checked.
- Repository operations execute in an explicit caller-owned `UnitOfWork`; concrete JDBC work must
  run off the Minecraft owner thread.
- An objective event has a stable idempotency key and audit metadata. The durable event claim,
  state save and downstream outbox work are expected in one M04 transaction.
- Optimistic revision conflicts and storage failures are returned as typed `Result` failures.
- Reward intents reference M12 `RewardId` and do not bypass the M12 reward engine.
- Phase 1 and M12 public signatures remain immutable; Phase 2 is additive.
