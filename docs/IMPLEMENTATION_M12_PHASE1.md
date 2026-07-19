# M12 Phase 1 implementation evidence

**Status:** ACTIVE CHECKPOINT — progression foundation implemented; later M12 phases not started
**Requirements:** foundational portions of `ZBW-PROG-001..005` and `ZBW-PROG-011`

## Scope delivered

M12 Phase 1 materializes the Java-8-neutral `zbw-progression` application module. It provides
immutable typed progression accounts, XP, levels, prestige, persistent currencies, append-only
ledger entries, reward registrations and entitlements. Every aggregate snapshot carries an M04
`RecordRevision`; mutations and grants carry stable transaction or idempotency identities and
auditable timestamps/correlation metadata.

Eight repository interfaces define caller-owned `UnitOfWork` boundaries without JDBC, SQL,
migrations or storage implementation dependencies. Projection contracts consume immutable,
bounded M08 event envelopes and expose typed applied/duplicate/retryable/rejected results,
checkpoints and bounded recovery state using M04 inbox/outbox semantics.

## Currency boundary

M11 remains authoritative for match-local iron, gold, diamond, emerald and custom tender resources,
quotes and match purchases. M12 types represent persistent currency accounts and ledger entries
only. Phase 1 does not bind persistent currency to M11 purchases and does not modify M11 behavior.

## Explicitly deferred

No persistence implementation, migration, reward delivery engine, currency transaction service,
GUI, command, Paper projection, statistics, PlaceholderAPI, replay, Atlas, Redis/proxy provider or
legacy compatibility work is included. These remain with later M12 phases or M13+ as allocated.

## Verification

The affected Java 21 reactor compiles the new module to Java 8 bytecode. Ten focused tests cover
typed-ID equality, immutable/defensive models, revisions, validation, repository-interface shape,
bounded event serialization, duplicate-result semantics, checkpoints and recovery limits. Final
quality and governance results are recorded by the Phase 1 checkpoint commit.
