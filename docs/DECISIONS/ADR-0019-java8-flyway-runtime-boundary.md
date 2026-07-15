# ADR-0019 — Java 8 and Flyway runtime boundary

**Status:** Accepted technical refinement
**Date:** 2026-07-15
**Requirements:** `ZBW-ARC-008`, `ZBW-DEPLOY-007`, `ZBW-READY-005`, `ZBW-READY-008`

## Context

The approved build fixes Flyway Core at 10.20.1 while shared public/storage artifacts must load as Java 8 bytecode. Flyway 10 classes target a newer JVM. Direct linkage would make the storage artifact fail before a fallback could run. Current Flyway versions also separate some database support into vendor modules, and those modules have no approved dependency-audit rows.

## Decision

`zbw-storage-sql` remains Java 8 bytecode and never imports Flyway classes. `FlywayMigrationProvider` loads the exact approved Flyway runtime reflectively on a compatible JVM and returns a typed failure if it is absent/incompatible. A real classpath migration contract proves Flyway discovery, validation, execution and history behavior.

`SchemaMigrator` is the canonical always-available runner for SQLite, MySQL and MariaDB. It uses the same ordered/versioned/checksummed migration invariants, persists history and rejects drift. This preserves automatic validated migration on Java 8 and does not weaken any database feature. External-engine unsafe DDL requires validated backup and restore-based rollback.

No Flyway vendor module is resolved until its exact coordinate, licence, transitive graph, checksum, Java compatibility and redistribution scope are added to the audit and lock.

## Consequences

- Java 8 can load and use every M04 storage adapter without parsing newer Flyway class files.
- Modern runtimes can execute the approved Flyway core provider; Java 8 uses the deterministic runner.
- Migration semantics have two implementations and therefore share contract/checksum/order tests.
- MySQL/MariaDB automatic migration remains available through `SchemaMigrator`; vendor-module acquisition is a future compatibility enhancement, not missing functionality.
- Thin artifacts bundle neither Flyway nor drivers.

## Verification

The Java 8/11/16/17/21 matrix verifies class major 52. The Flyway contract runs on a compatible JVM. Schema tests cover order, checksum drift, restart/crash behavior and unsafe-DDL classification; architecture validation rejects direct `org.flywaydb` imports.
