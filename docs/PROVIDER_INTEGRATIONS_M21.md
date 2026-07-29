# M21 Paper provider integrations

## Scope and ownership

M21 Phase 2 materializes isolated Java 8 adapters for Vault, LuckPerms,
Citizens, ZNPCsPlus, DecentHolograms, AlessioDP Parties, Grim and Vulcan.
The adapters implement the neutral `zbw-api` SPIs introduced in Phase 1.
They do not own economy ledgers, profiles, parties, presentation state or
moderation verdicts. Those boundaries remain with M12, M14, native M21 party
SQL, the owning presentation modules and M18 respectively.

This phase covers ZBW-INT-002, ZBW-INT-003, ZBW-INT-006, ZBW-INT-007,
ZBW-INT-008, ZBW-INT-009, ZBW-READY-007 and ZBW-ARC-007.

## Isolation model

Each adapter receives a small vendor gateway from the Paper composition root.
No vendor class appears in a neutral API, adapter signature or packaged
dependency. `OptionalProviderLifecycle` probes the operator installation and
reports `AVAILABLE`, `ABSENT` or `INCOMPATIBLE`; absent and incompatible
providers fail closed without preventing Paper startup.

The reactor deliberately adds no vendor Maven artifact in this phase. This
preserves the dependency lock and prevents proprietary, snapshot or server
binaries from entering build products. A production binding may be enabled
only after its exact artifact/version/source/licence evidence satisfies
`DEPENDENCY_LICENSE_AUDIT.md`.

## Provider behavior

- Vault delegates balance and transaction intents; it never writes the M12
  ledger.
- LuckPerms projects permission and metadata queries and keeps only bounded
  invalidation versions; it never owns profiles.
- Citizens, ZNPCsPlus and DecentHolograms expose presentation operations only.
- AlessioDP is migration-only. Native party SQL remains authoritative and a
  migration authorization fence prevents live split brain.
- Grim and Vulcan subscribe to event-push alerts, normalize bounded signals and
  deduplicate provider alert identifiers. They never create Atlas cases or
  verdicts.

Paper registers the adapters through `PaperProviderIntegrationRuntime`.
Registration rejects duplicate provider IDs. Start, drain and stop are
asynchronous; shutdown cleanup runs in reverse registration order and never
waits on the Minecraft owner thread.

## Operator checks

1. Install only an approved provider version and its normal server-side
   dependencies.
2. Supply the corresponding gateway binding at Paper composition.
3. Confirm provider health is `AVAILABLE` before enabling dependent features.
4. Verify missing and incompatible provider fallbacks during staging.
5. For party migration, stop live writes at the source, authorize one transfer,
   verify native SQL recovery, then disable the external authority.
6. For anticheat providers, verify alert deduplication with both integrations
   enabled and confirm Atlas remains the sole case/verdict owner.

CloudNet, service discovery runtime, optional world providers, dashboards and
M22 compatibility certification remain outside this phase.
