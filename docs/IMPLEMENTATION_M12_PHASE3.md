# M12 Phase 3 — Progression and reward engine implementation

## Scope and requirements

This checkpoint implements the gameplay-independent Phase 3 portions of `ZBW-PROG-001`,
`ZBW-PROG-002`, `ZBW-PROG-003`, `ZBW-PROG-005` and `ZBW-PROG-011`. It is complete and contributes to full
M12 progression closure; only later `M13+` ownership (statistics/PlaceholderAPI/objectives/atlas/cosmetics) remains unchanged.

## Delivered

- `ExperiencePolicy`: versioned source rules, integer basis-point multipliers, bonuses, caps and
  bounded anti-farming reduction with deterministic audit output.
- `LevelFormula`: validated contiguous cumulative thresholds, preview, recalculation and
  definition-version migration input.
- `PrestigePolicy`: contiguous definitions and immutable transition intents containing their
  reward output. `ProgressionService.MutationPort` requires state, history, audit and reward intent
  to commit or roll back together.
- `RewardEngine`: atomic claim boundary, immutable plans and outputs, offline pending state,
  expiration, bounded retry, sanitized failure queue evidence and terminal compensation.
- `UnlockPolicy`: generic level/prestige entitlement outputs only; it does not implement quests,
  achievements, passes, cosmetics or profiles.
- `ProgressionEventAdapter` and `ProgressionProjectionService`: configurable M08 match-completion
  and M11 settlement mappings that retain original inbox keys and delegate atomic projection and
  recovery to caller-owned M04 transactions.
- `ProgressionService`: centrally authorized grant/remove XP, reward grant, inspect, recalculate and
  prestige application use cases. It defines no command or GUI.

## Transaction and threading contract

All policy objects are immutable and Java-8 neutral. Calculations perform no I/O. Durable adapters
must execute the mutation and projection ports away from the Minecraft owner thread. Reward stores
must atomically claim an idempotency key before delivery; delivery implementations must also honor
that key. Prestige and XP ports must append immutable ledger/history/audit evidence in the same M04
unit of work as aggregate changes and outbox publication.

## Deferred explicitly

No command engine, command routing, Paper adapter ownership, quest, achievement, battle-pass, cosmetic,
profile, replay, Atlas, Redis/proxy, provider or compatibility implementation was introduced.
