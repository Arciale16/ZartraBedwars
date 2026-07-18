# Milestone 11 implementation evidence

**Milestone status:** ACTIVE — PR #17 merged at `355748d2386a7d0c554e346bd8911dd72799e395`; final-integration checkpoint complete; M11 exit criteria remain open
**Checkpoint scope:** Phases 1–4 plus M08 lifecycle, M09 presentation and primary Paper projection integration
**Branch:** `agent/milestone-11-shop-content-generators-upgrades`

## Implemented in Phase 1

- Materialized Java 8 modules `zbw-shop`, `zbw-content` and `zbw-scripting-api`.
- Added typed immutable catalog/category/item/rotation/script/action identities.
- Added scoped catalog, configurable categories and item definitions, native/custom/multiple
  resource prices, purchase rules, cooldowns and limits.
- Added central-authorization quote validation and one atomic, revision-fenced, idempotent purchase
  commit contract covering debit, grant, counters, cooldown and history.
- Added Quick Buy, favourite, history and local rotation state/event contracts.
- Added the four original shop balance profiles and the original 24-item starter catalog from
  `docs/BALANCING_BASELINE.md`.
- Preserved the M09 command/UI frameworks unchanged; later M11 feature adapters must invoke these
  use cases and may not recreate a command or GUI framework.

## Implemented in Phase 2

- Added Java 8-neutral immutable generator configuration for native and custom resources.
- Added arena/team ownership, enabled state, independent intervals, bounded capacities, yields and
  world-drop/direct/split delivery rules.
- Added a deterministic synchronized runtime with monotonic sequences, stable idempotency keys,
  bounded catch-up work, retry-preserved batches and terminal cleanup.
- Added a deterministic M07 arena projection with validated per-arena overrides and an M08
  `MatchSnapshot` fleet coordinator that starts only in `PLAYING` and cleans on completion/reset.
- Added fair rotating allocation of indivisible Generator Split amounts and independent runtime
  state for multiple generators and resource types.
- Implemented Phase 2 portions of `ZBW-SHOP-006`, `ZBW-ADDON-194..201` and
  `ZBW-ADDON-363..368`; later-milestone cells remain deferred.

## Implemented in Phase 3

- Added an immutable upgrade catalogue with typed kinds, levels, costs, dependencies and runtime
  effect identifiers for team upgrades, forge, heal pool, dragon buff, traps and custom upgrades.
- Added recoverable match/team state, revision control, idempotent atomic purchases, bounded queued
  traps, team isolation and terminal cleanup.
- Added neutral effect intents and a deterministic bounded forge scheduler with recovery-safe keys.
- Implemented the neutral M11 core portion of `ZBW-SHOP-005`; M09 presentation is consumed later in
  M11, while M16 placeholders and M22 compatibility remain deferred.

## Implemented in Phase 4

- Added a Java 8-neutral utility-item catalogue, typed targets and deterministic action results.
- Added exact authorization, atomic match-resource/inventory and owner-thread effect ports.
- Added synchronized lifecycle validation, cooldowns, limits, target/team rules, idempotency,
  conflict handling and M08-driven cleanup.
- Added bounded original mechanics for Pop-up Tower, Rush, Ultimate, BedSteal, Voidless, Sponge,
  remaining local generator actions and Item Rotation.
- Implemented Phase 4 neutral portions of `ZBW-SHOP-007`, `ZBW-READY-004`,
  `ZBW-ADDON-141..147`, `184..201`, `300..322`, `341..349`, `363..368`, `379..388` and
  `438..452`; final presentation, Paper certification and later-owner cells remain open.

## Deliberately not implemented

This checkpoint does not implement final named-mode orchestration,
Paper inventory/effect adapters, shop/generator/item GUIs or commands, runtime configuration loaders or the
declarative interpreter. It also does not implement any M12, M15, M16, M19, M20, M21 or M22
ownership. `zbw-scripting-engine` remains planned until a real later-M11 execution phase.

## Final integration checkpoint

- `M11MatchRuntime` consumes immutable M08 match snapshots and coordinates generator ticks,
  upgrade observation, utility-action synchronization and idempotent terminal cleanup without
  owning any M08 state transition.
- `PresentationActions.Catalog.m11()` adds 25 deterministic command/GUI parity actions to the M09
  framework. Generated inventories now contain 140 actions and granular permissions.
- `M11PaperProjection` provides an owner-thread guarded Java 21 translation boundary for generator
  batches, team effects and utility effects; it contains no feature policy.
- Exact M11 Java 8/21 API baselines and strict JavaDoc archives are generated independently while
  proving every M10 public signature remains present.

This checkpoint intentionally does not claim the M11 milestone exit. The planned
`zbw-scripting-engine`, complete named-mode orchestration, concrete shop/inventory/configuration
adapters and the full item/purchase/generator/upgrade/mode/Paper acceptance matrices remain required.
M12 is not active.

## Post-merge closure audit

The governance-only audit of merged PR #17 confirmed that its published checks passed, but the
merged evidence itself retains the missing `zbw-scripting-engine`, complete named-mode
orchestration, concrete inventory/configuration adapters and full M11 acceptance matrices.
Consequently M11 cannot be moved to `completed_milestones`, and M12 cannot be activated, without
new implementation and verification evidence. This is an evidence gate, not a scope reduction.

## Verification

The Phase 1 reactor compiles on the pinned Temurin 21.0.6 toolchain while emitting Java 8 bytecode.
Tests are deterministic and contain no skipped cases:

| Module | Tests | Result |
|---|---:|---|
| `zbw-scripting-api` | 3 | PASS |
| `zbw-shop` | 18 | PASS |
| `zbw-content` | 3 | PASS |
| **Phase 1 total** | **24** | **PASS** |

Phase 2 adds six generator tests covering definitions/resources, timing, capacity, multiple and
custom resources, per-arena overrides, enable/disable, deterministic and duplicate-free generation,
retry-safe delivery, concurrent ticks, split fairness, bounded catch-up and match-end cleanup. The
affected reactor now runs 24 `zbw-shop` tests with zero failures/errors/skips. JDK 21
`-Pquality verify` passes Checkstyle and SpotBugs with zero findings and every JaCoCo threshold.

Phase 3 adds nine tests for purchases, resources, idempotency, levels, dependencies, forge timing and
resources, traps, effects, isolation, reconnect recovery and cleanup. The affected reactor now runs
33 `zbw-shop` tests with zero failures/errors/skips.

Phase 4 adds seven focused tests covering definitions, all eight action families, permission,
atomic costs/inventory consumption, targets, cooldowns, concurrent duplicates, transaction
failures, team isolation, reconnect-safe ownership and cleanup.

The final reactor passed 349 tests in 69 suites with zero failures, errors or skips. The complete
matrix passed on pinned Temurin 8u442, 11.0.26, 16.0.2, 17.0.14 and 21.0.6 toolchains; only the
approved Java 21 profile materializes modern Paper artifacts. Strict JavaDoc passed for 336 Java 8
and 49 Java 21 sources and generated deterministic M11 archives. Checkstyle and SpotBugs reported
zero findings. JaCoCo met every configured module threshold; current M11 module evidence is:

| Module | Line coverage | Branch coverage |
|---|---:|---:|
| `zbw-scripting-api` | 100.00% | 100.00% |
| `zbw-shop` | 93.22% | 76.65% |
| `zbw-content` | 96.63% | 85.71% |

The M10 Paper 1.21.1 build 133 certification was rerun against the M11 plugin artifact and passed
selector, queue command/GUI, parity, owner-thread, bounded-worker, stale/duplicate and cleanup
assertions. M11-specific Paper projection tests pass, but a complete M11 gameplay certification
harness remains part of the open M11 exit work described above.

All 36 governance tests pass. The deterministic gates retain 672/672 requirement rows, 6,438
Master Prompt assertions, 49 addon references/473 addon IDs, 55 decision IDs and 100% coverage.
The dependency/licence lock remains unchanged and validates 212 Maven components/650 exact files
with zero bundled product dependencies because Phase 1 introduces no external dependency.

The generated M11 API baseline contains 677 Java 8 and 51 Java 21 public classes and proves the
immutable M10 baselines remain additive. Inventories contain 140 command/GUI actions and 140
granular permissions. The feature dashboard contains all 672 requirement rows deterministically.

## Reactor wiring note

The planned `zbw-storage-sql -> zbw-shop` adapter dependency is intentionally not activated in
Phase 1: no SQL shop adapter exists yet, and enabling the edge would close the existing test-only
reactor path `zbw-arena -> zbw-storage-sql -> zbw-shop -> zbw-arena`. The future SQL adapter phase
must introduce a cycle-free test-fixture boundary before activating that planned edge. This does
not affect the neutral asynchronous preference/history/rotation ports delivered here.
