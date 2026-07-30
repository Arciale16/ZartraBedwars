# M23 migration, ecosystem and operations

## Scope and boundaries

M23 implements `ZBW-ECO-001..005`, the M23 portion of `ZBW-OPS-006/009` and
`ZBW-ADDON-283..290`. It adds no durable business owner. Progression, statistics, permissions,
shop content and replay remain authoritative in their existing modules.

`MigrationApi` is the Java 8 public contract. `DeterministicMigrationEngine` runs on a caller-owned
bounded worker executor and coordinates only injected target, backup and journal ports. A target
port must provide an atomic apply operation. Dry runs never call backup, journal or target mutation.
Apply always captures a validated backup and records a prepared journal entry first. Any thrown
apply failure restores the backup before returning a sanitized failure report.

`MigrationCommandWorkflow` exposes the neutral, audited `/zbw admin migrate-layout` operations
without binding the engine to Paper. `M23MigrationConfiguration` keeps migration disabled by
default, requires backups and enforces the same source and record limits as the engine.

The lawful layout adapter receives a caller-confined `Reader`; it cannot select or traverse a
filesystem path. Input is limited to one MiB, 10,000 records, 64 scalar attributes per record and
4,096 characters per value. Imported actions are allowlisted data and are never executed.

## Evidence

- immutable API boundary, validation and serialization-oriented tests;
- deterministic ordering, duplicate, conflict, dry-run, apply, restore and rollback tests;
- permission, before/after audit and command-routing tests;
- disabled-by-default and bounded migration configuration tests;
- valid, lossy, malformed and unsupported layout conversion tests;
- Plugin Doctor operational category and severity aggregation tests;
- compiled SDK example depending only on `zbw-api`;
- strict Java 8 compilation, JavaDoc and API compatibility gates.

M22 exact runtime/provider certification remains external and active. M23 implementation does not
create a compatibility support claim or begin M24 release qualification.
