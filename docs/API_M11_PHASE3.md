# M11 Phase 3 — Team upgrade API

Phase 3 implements the Java 8-neutral core portion of `ZBW-SHOP-005` in
`io.zartra.bedwars.shop.upgrade`. It exposes no Bukkit, Paper, NMS, persistent progression,
statistics, PlaceholderAPI, distributed coordination, provider implementation or legacy adapter.

## Catalogue and state

- `UpgradeDefinition` describes a typed kind, ordered levels, match-resource costs, dependencies,
  effect identifiers and optional forge timing.
- `UpgradeCatalog` is immutable and rejects duplicate or unresolved entries.
- `TeamUpgradeState` is an immutable, recoverable match/team snapshot with levels, queued traps,
  completed idempotency keys and cleanup state.

## Application boundaries

- `TeamUpgradeService` consumes M08 `MatchSnapshot` state and does not own the match lifecycle.
- `UpgradeTransactionPort` is the atomic resource-debit boundary with revision and idempotency
  guarantees.
- `UpgradePurchaseResult` models expected outcomes without generic exceptions.
- `TeamEffectIntent` requests platform-neutral upgrade, trap, heal pool, dragon buff and forge
  effects. Paper may translate intents but must not decide policy.

## Forge runtime

`ForgePolicy` defines level-specific intervals and native or custom resource outputs.
`ForgeRuntime` emits deterministic, bounded, idempotently keyed delivery intents and performs
terminal cleanup when the M08 match leaves its playable state.

Mutable coordination methods are synchronized. Returned definitions, collections and states are
immutable. Null or malformed inputs are rejected at construction or service boundaries.
