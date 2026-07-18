# M11.1 corrective implementation evidence

## Phase 1 — scripting, configuration and content completion

**Status:** implemented checkpoint; M11 remains active and M12 remains blocked.

### Requirements

This phase implements the remaining Phase 1 portions of `ZBW-READY-004`, `ZBW-READY-015`,
`ZBW-CONTENT-002/003` and the configuration/content foundations shared by `ZBW-GAME-004/005`,
`ZBW-SHOP-001..007` and the M11 addon allocations. It does not claim any M11.1 Phase 2 mechanic.

### Modules and boundaries

- `zbw-scripting-engine` is materialized as Java 8 neutral code. It depends only on `zbw-api`,
  `zbw-application` and `zbw-scripting-api` and submits execution exclusively through the injected
  M05 `SchedulerPort`.
- `zbw-config` adds one immutable eight-section M11 snapshot boundary for shops, generators,
  upgrades, traps, utility items, modes, rotations and scripts.
- `zbw-content` adds versioned identity-ordered mode balance profiles and pure golden simulation.
- No module imports Bukkit, Paper, NMS, filesystem, network, Redis, proxy, database implementation
  or a later-milestone API.

### Security behavior

The declarative engine is disabled by default. Graphs must match the exact schema version and pass
action, capability, handler, child, node-count and depth validation before execution. Work is
bounded by scheduler admission/deadline, cooperative cancellation, maximum depth and maximum
operation count. The authenticated caller is checked through the M03 central `AuthorizationService`
using `zartrabedwars.script.execute`; denials and handler exceptions fail closed and are audited
without exposing exception details. Argument and input size/control-character validation is fail-closed. Scripts are
data only and have no reflection, classloader, host, file, process, socket, native or thread API.
Terminal audit records contain only typed IDs, correlation ID, stable code and operation count.

### Configuration and activation

Every activation supplies all eight versioned documents. Consecutive pure migrations run before
semantic validation. Participants prepare first, then apply in deterministic section order; any
failure rolls back and retains the previous immutable snapshot. Balance simulations run twice and
reject differing output or configured score bounds.

### Verification scope

Tests cover disabled execution, allowlisted execution, audit output, schema/action/capability/
handler/child rejection, recursion and operation limits, cancellation, handler failure, malformed
input, duplicate handlers, migration, all-section activation, validation failure, apply rollback,
last-known-good retention, incomplete/duplicate/future documents and deterministic simulations.

Checkpoint evidence: 11 deterministic unit/security tests passed; Java 8 source and test compilation
passed; Checkstyle and SpotBugs reported zero violations. JaCoCo measured engine 95.71% line/80.39%
branch, configuration 92.98%/73.40%, and content 93.33%/80.77%, satisfying the affected module
thresholds. Strict JavaDoc passed for the complete affected Java 8 source closure. The additive
M11.1 baseline locks 29 public classes while retaining the immutable M11 baseline. All 36 governance
tests, dependency/licence locks, inventories, dashboard, catalogue, coverage and pre-code decision
validators passed. The local Maven test reactor itself remains unavailable in this managed Windows
sandbox because `javac` raises `AccessDeniedException` while closing verified JAR files; the same
sources and tests were compiled and executed directly with the locked Temurin toolchain and no test
was skipped. M11.1 Phase 2 remains blocked until this checkpoint is accepted; M12 remains blocked
until all M11.1 phases close RC-087.
