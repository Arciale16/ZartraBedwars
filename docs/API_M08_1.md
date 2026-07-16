# M08.1 typed API

M08.1 is an additive Java-8 API hardening release. It preserves every M08 binary
signature. Public inputs reject null or malformed values; expected operational
failures use typed `Result`/`ApiError`, while constructor contract violations use
`IllegalArgumentException` or `NullPointerException`.

## New contracts

| Package | Type | Contract |
|---|---|---|
| `io.zartra.bedwars.domain.team` | `TeamLayoutLimits` | Shared safety bounds; no preset selection policy. |
| `io.zartra.bedwars.arena.model` | `ArenaDefaultProfile` | Immutable copied defaults for new arena definitions. |
| `io.zartra.bedwars.arena.validation` | `ArenaValidationProfile` | Exact typed generator and team/NPC prerequisites. |
| `io.zartra.bedwars.game.model` | `TeamDefinition` | Stable runtime ID, label, semantic color and capacity. |
| `io.zartra.bedwars.game.model` | `MatchTimingPolicy` | Timings separated from arena-derived limits. |
| `io.zartra.bedwars.game.model` | `VictoryEvaluator` | Deterministic, I/O-free future override contract. |
| `io.zartra.bedwars.game.model` | `VictoryEvaluation` | No-completion result or winner/outcome completion intent. |
| `io.zartra.bedwars.game.model` | `StandardVictoryEvaluator` | Generic surviving-eligible-team policy. |
| `io.zartra.bedwars.game.application` | `MatchAssemblyRequest` | Atomic bundle, exact versions, timing and creation instant. |
| `io.zartra.bedwars.game.application` | `ArenaMatchAssembler` | Fail-closed arena-to-match construction. |

`TeamSnapshot`, `GameRules`, `MatchTransition` and `MatchStateMachine` gain additive
accessors/overloads. Existing construction and recovery signatures remain binary
compatible. Runtime composition should use `ArenaMatchAssembler`; legacy direct team
snapshot constructors remain solely for compatibility and low-level deterministic
tests.

All contracts are platform-neutral and perform no I/O. Immutable values and the
assembler/validator/evaluator are safe for concurrent reads. `MatchStateMachine`
mutation remains serialized. A completion intent is not authorization to commit: the
caller must supply the existing idempotency key and persist the resulting transitions.

M10 may supply another `VictoryEvaluator`; it must not change these contracts or move
policy into Paper. M08.1 introduces no game-mode SPI and does not promise M10 mode
semantics. The exact API is locked in `build/api-signature-baseline-m08-1.txt`; the
unchanged Java-21 boundary is locked separately.
