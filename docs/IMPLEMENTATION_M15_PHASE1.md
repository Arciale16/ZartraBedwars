# M15 Phase 1 — Statistics Foundation

## Scope

Implements the Java-8-neutral foundation for `ZBW-STATS-001..008` and the foundational portions of `ZBW-ADDON-217..225`, `ZBW-ADDON-260..265` and `ZBW-ADDON-350..356`.

`zbw-statistics` owns immutable statistic definitions and aggregates, M08/M11/M12 event-adaptation contracts, transaction-bound idempotent projection and bounded rebuild contracts, JDBC-free repository ports, and deterministic leaderboard ordering/pagination.

## Explicit exclusions

This phase creates no SQL migration or adapter, Paper code, command, GUI, PlaceholderAPI expansion, web endpoint, hologram, external provider, Redis cache, cross-server ranking, replay consumer, or game lifecycle behavior. Those remain with their assigned later milestones.

## Validation evidence

- JDK 8 direct compilation of all new module sources passed with `-Xlint:all -Werror`.
- `git diff --check` passed before test execution.
- Maven test and quality reactor are pending environmental access to the checksum-locked Maven plugin artifacts; no dependency version or lock was changed to bypass this gate.
