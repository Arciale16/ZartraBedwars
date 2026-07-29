# M21 Phase 1: Provider SPI and native party foundation

Requirement scope: `ZBW-DEPLOY-005`, `ZBW-INT-002`, `ZBW-INT-003`,
`ZBW-INT-005`, `ZBW-INT-006`, `ZBW-INT-007`, and `ZBW-INT-009`.

## Boundaries

`zbw-api` owns Java 8 neutral provider contracts for economy,
permission/meta, party, NPC, hologram, and service discovery. These contracts
contain no Paper or vendor type and do not acquire ledger, profile, world,
rendering, or deployment ownership.

`zbw-party` owns the native immutable party aggregate: identity, leader and
membership, invitations, privacy, bounded migration, and transfer intent.
SQL is authoritative. Redis may coordinate a migration and the proxy may
transport an opaque transfer intent, but neither is party state authority.

`zbw-party-sql` is the only Phase 1 persistence adapter. Its checksum-locked
migration separates parties, memberships, and invitations. Repository writes
are transactional, revision checked, duplicate protected, restart safe, and
fail closed on malformed persisted state.

Vendor adapters, Paper presentation, commands, GUI, CloudNet, Vault,
LuckPerms, Citizens, ZNPCsPlus, hologram providers, anticheat adapters, and
world-provider implementations are not materialized by this checkpoint.

## Lifecycle and recovery

The native lifecycle is `CREATED -> ACTIVE -> MIGRATING -> ACTIVE`, with
terminal `DISBANDED`. A migration can be rolled back before completion.
Invitation acceptance validates party state, invitee, expiry, revision, and
membership capacity. Persistence restores the same immutable revision and
rejects duplicate party/member records.

## Verification

Contract tests cover Java 8 provider neutrality, immutable snapshots and
bounded values. Party tests cover lifecycle transitions, privacy, invitation
expiry, duplicate membership, and migration. SQL tests cover round trip,
optimistic conflicts, duplicate creation, and restart recovery.
