# M04 database migration policy

## Canonical schema

Schema version 1 creates `zbw_records`, `zbw_outbox`, `zbw_inbox`, `zbw_retention`, `zbw_legal_hold`, `zbw_tombstone`, `zbw_backup_history` and `zbw_schema_history`. The outbox claim index is `idx_zbw_outbox_claim(delivered_at, available_at, claimed_until, sequence_no)`. Tables store only generic durable envelopes; no feature behavior is introduced.

`SchemaMigrator.plan()` returns a contiguous plan starting at version 1. The SHA-256 is calculated over the exact ordered dialect statement list. Startup creates history first, compares the persisted checksum, applies only missing statements and records history last. A checksum mismatch is a permanent typed conflict. DDL is idempotent across a crash before the history insert; index existence is checked through JDBC metadata.

## Flyway boundary

Flyway Core is pinned to `10.20.1`. Its current bytecode is not link-safe for the Java 8 runtime artifact, so `FlywayMigrationProvider` loads it reflectively on compatible runtimes. The test migration `V1__flyway_contract.sql` proves real discovery, validation, execution and schema-history creation. The built-in deterministic runner preserves migration functionality on the mandatory Java 8 target.

MySQL/MariaDB Flyway database modules are not added because they have no approved dependency-audit row. The built-in runner is therefore canonical for those engines in M04. Adding a Flyway vendor module requires exact coordinate, licence, checksum and Java-matrix evidence before resolution.

## Unsafe DDL and rollback

SQLite version 1 is restart-safe and does not claim transactional DDL rollback. MySQL and MariaDB plans mark DDL as unsafe because server DDL may commit implicitly. Before applying any future unsafe migration, operations must:

1. quiesce writes and drain active units of work;
2. create an encrypted, independently validated backup;
3. record schema version, migration checksum and backup evidence;
4. apply and validate in order;
5. restore from the validated backup on failure—never attempt an assumed reverse DDL;
6. reconcile outbox/inbox operations before reopening writes.

Migration execution uses prepared history statements and bounded query timeouts. Schema repair never edits a checksum in place; operators restore or ship a new forward migration.
