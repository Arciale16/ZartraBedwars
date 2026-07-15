# M04 storage API reference

## Compatibility and threading

All M04 exported classes compile to Java 8 bytecode. Public values are immutable or defensive snapshots, reject `null` unless explicitly documented and use typed `Result` failures for expected operational errors. `zbw-storage-api` exposes no JDBC type or implementation class. Every engine/repository/recovery call is blocking and must run on a bounded storage worker; M04 never dispatches work itself.

The append-only signature is `build/api-signature-baseline-m04.txt`. Validation requires every M02 and M03 signature to remain intact. Deprecation is additive for at least one compatibility window; removal requires a major public API version and accepted ADR.

## Durable records and transactions

| Type | Contract |
|---|---|
| `RecordKey` | Typed aggregate type plus typed aggregate identity. |
| `RecordRevision` | Non-negative optimistic revision with overflow-safe increment. |
| `StoredRecord` | Defensive versioned byte payload and durable timestamp. Feature serializers remain with later owning milestones. |
| `TransactionOptions` | Read/write intent, positive timeout and bounded deadlock retries. |
| `UnitOfWork` | Creator-thread-confined commit/rollback boundary; close never commits. |
| `StorageRepository` | Transaction-bound find/save/delete with revision conflict results. |
| `StorageEngine` | Lifecycle and unit-of-work entry point for SQLite, MySQL or MariaDB. |

`JdbcStorageEngine` is constructed explicitly from `SqlStorageConfiguration`; it is never a service locator. The configuration validates engine-specific JDBC schemes, redacts diagnostic rendering, copies password characters and restricts pools to 1..64 connections. SQLite requires exactly one.

## Messaging, migration and cache

`MessageEnvelope` contains an `IdempotencyKey`, canonical `EventMetadata`, defensive payload and availability time. `MessageRepository` enqueues, claims, acknowledges and receives within the supplied unit of work. Claims are capped at 1,000 and ordered by causal sequence, availability and operation ID.

`Migration` and `MigrationPlan` require contiguous positive versions and lowercase SHA-256 checksums. `SchemaMigrator` validates persisted history before mutation. `FlywayMigrationProvider` is reflectively linked so the shared artifact remains loadable on Java 8 while Flyway 10 executes on a compatible runtime.

`VersionedCache` is explicitly non-authoritative. `CaffeineVersionedCache` requires a maximum size, per-entry positive lifetime and minimum accepted revision; stale values are invalidated rather than returned.

## Privacy and recovery

`RetentionPolicy` carries the stable retention class, retention duration and deletion deadline. `RetentionRepository` persists retention, authorized hold/release and content-free tombstones in the same unit-of-work model. Identity separation and feature-specific encryption remain mandatory in M17/M18.

`RecoveryService` exposes backup/restore evidence and recovery objectives. `SqlRecoveryCoordinator` composes a database-specific `BackupDriver`, rejects unencrypted or unvalidated artifacts and exports only backup ID, completion time and SHA-256—not paths, credentials or content.

## Error and lifecycle behavior

Expected conflicts, closed engines, database failures, invalid backup evidence and retryable connection/deadlock states use stable `ApiError` codes and localization keys. Vendor messages are not exported. Programmer misuse (foreign/closed unit, cross-thread access, malformed construction) fails immediately. Engine/cache close is idempotent; active unit close rolls back.
