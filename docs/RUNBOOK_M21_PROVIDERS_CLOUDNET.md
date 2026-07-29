# M21 provider and CloudNet operations runbook

Scope: `ZBW-DEPLOY-005`, `ZBW-INT-002/003/006/007/008/009`,
`ZBW-OPS-006` and `ZBW-ADDON-226..235`.

## Plugin Doctor

Register the check returned by
`ZartraBedWarsPlugin.providerCompatibilityCheck()` with the existing bounded
Plugin Doctor engine. The check is nonblocking, reads only cached lifecycle
health and emits public stable fields for the canonical Vault, LuckPerms,
Citizens, ZNPCsPlus, DecentHolograms, AlessioDP Parties, Grim, Vulcan and
CloudNet provider identities.

Interpret each provider result as follows:

- `PRESENT`: one compatible adapter is installed and its lifecycle is usable.
- `ABSENT`: the optional plugin is not installed or is intentionally disabled.
- `INCOMPATIBLE`: the plugin was detected but the approved gateway/API binding
  is unavailable or failed its compatibility probe.
- `DUPLICATE`: more than one adapter claimed the canonical provider identity;
  the second registration was rejected.

Absence is a degraded, safe fallback. Incompatibility and duplicates are
unavailable states and must be corrected before enabling dependent operations.
Diagnostic evidence never includes credentials, endpoints, player identity,
party membership, anticheat payloads or moderation data.

## Provider recovery

1. Put dependent presentation or migration operations into maintenance mode.
2. Confirm the exact provider binary/version is independently installed and
   approved by the dependency/licence policy; no vendor binary is bundled.
3. Replace the operator-supplied gateway binding and restart its optional
   adapter lifecycle.
4. Run Plugin Doctor and require `PRESENT` for that provider.
5. Exercise one non-mutating provider query before reopening traffic.

For `DUPLICATE`, remove the duplicate composition binding. Do not select one
implicitly. For `INCOMPATIBLE`, retain the native/no-provider fallback until an
approved binding is deployed.

## Party migration

Native party SQL is authoritative until `Party.beginMigration` records exactly
one external target. While `MIGRATING`, native mutations are fenced and only a
`TRANSFER` intent may cross the AlessioDP gateway. Preserve the privacy
projection during transfer.

- Success: commit the external transfer, then call `completeMigration`; the
  native aggregate becomes `DISBANDED`.
- Failure or timeout: do not activate the external authority; call
  `rollbackMigration` and verify the native aggregate returns to `ACTIVE` with
  unchanged privacy and membership.

Never run native and external party mutation authorities concurrently.

## Anticheat operation

Grim and Vulcan may run independently or together. Both publish only normalized
`AntiCheatProvider.Alert` signals and deduplicate their own provider alert IDs.
They never create Atlas cases, record verdicts or punish players. If one source
fails, stop only that adapter and retain the other source; Atlas remains the
sole case/verdict decision owner.

## CloudNet degradation and recovery

CloudNet owns service lifecycle only. M19 owns leases/fencing and M20 owns
routing. During CloudNet or Redis failure:

- pause new distributed scale, drain and crash-replacement actions;
- keep already-running local gameplay untouched;
- route only through fresh M20 backend registry entries;
- reject concurrent reconciliation and stale metadata/fencing tokens;
- use the static M20 registry with autoscaling disabled if CloudNet is absent.

After recovery:

1. Discover and sort current services by canonical ID.
2. Reject duplicate IDs and observations older than the accepted epoch/revision.
3. Rebuild proxy capacity/health projections without starting replacements.
4. Acquire a fresh M19 fence before the first lifecycle mutation.
5. Reconcile once; a concurrent request must receive the retryable
   `cloudnet.reconciliation_in_progress` result.
6. For a planned stop, drain an idle service before stop. For a crash, accept
   one fresh `OFFLINE` observation, remove that exact backend epoch and request
   at most one replacement.

Escalate if metadata remains stale, a fresh fence cannot be acquired or the
bounded callback executor rejects work. Do not bypass these guards.

## Closure limitation

This runbook certifies the materialized provider and CloudNet scope above. The
M21 milestone itself cannot be marked complete while the separately allocated
`ZBW-INT-005` optional WorldEdit/FAWE/WorldGuard/SlimeWorldManager/Multiverse
adapters remain unmaterialized. That allocation must be implemented or changed
through explicit owner-approved PRD/ADR/traceability governance.
