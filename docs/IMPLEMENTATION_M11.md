# Milestone 11 implementation evidence

**Milestone status:** ACTIVE — Phase 2 checkpoint complete; M11 exit criteria are not yet met
**Checkpoint scope:** Shop/content foundations plus generator and resource system
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

## Deliberately not implemented

This checkpoint does not implement upgrades, traps, utility-item mechanics, named-mode mechanics,
Paper inventory adapters, shop/generator GUIs or commands, runtime configuration loaders or the
declarative interpreter. It also does not implement any M12, M15, M16, M19, M20, M21 or M22
ownership. `zbw-scripting-engine` remains planned until a real later-M11 execution phase.

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

The full JDK 21 reactor passed 324 tests in 63 suites with zero failures, errors or skips. The
neutral Phase 1 reactor also passed on the pinned Temurin 8u442 toolchain. Strict JavaDoc passed
for all 23 new Java 8 production sources. Checkstyle and SpotBugs reported zero findings. JaCoCo
met every configured threshold with the following Phase 1 results:

| Module | Line coverage | Branch coverage |
|---|---:|---:|
| `zbw-scripting-api` | 100.00% | 100.00% |
| `zbw-shop` | 93.88% | 83.23% |
| `zbw-content` | 96.63% | 85.71% |

All 36 governance tests pass. The deterministic gates retain 672/672 requirement rows, 6,438
Master Prompt assertions, 49 addon references/473 addon IDs, 55 decision IDs and 100% coverage.
The dependency/licence lock remains unchanged and validates 212 Maven components/650 exact files
with zero bundled product dependencies because Phase 1 introduces no external dependency.

## Reactor wiring note

The planned `zbw-storage-sql -> zbw-shop` adapter dependency is intentionally not activated in
Phase 1: no SQL shop adapter exists yet, and enabling the edge would close the existing test-only
reactor path `zbw-arena -> zbw-storage-sql -> zbw-shop -> zbw-arena`. The future SQL adapter phase
must introduce a cycle-free test-fixture boundary before activating that planned edge. This does
not affect the neutral asynchronous preference/history/rotation ports delivered here.
