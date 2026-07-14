# Operational Defaults and Recovery Objectives

**Status:** Accepted
**Decision:** RC-061
**Requirement:** `ZBW-READY-016`

Every provider has a deterministic safe **degradation** state; no outage silently changes a durable gameplay or evidence invariant.

## Recovery objectives

| Data/service | RPO | RTO | Backup/replication | Restore evidence |
|---|---:|---:|---|---|
| SQL identity, currency, rewards, progression, stats, cases, audit | Zero acknowledged committed transactions; asynchronous replica may lag ≤5 s but is never used for consistency-sensitive reads | ≤15 min | Encrypted daily full + 15-minute incremental/binlog; 35 daily + 12 monthly | Quarterly clean-environment restore, integrity/reconciliation and application smoke suite |
| Active match recovery marker | ≤5 s | ≤5 min | Versioned marker every 5 s and lifecycle transition; encrypted bounded local spool | Kill -9 at every state transition; rejoin/abort/refund invariants |
| Ordinary replay chunks | ≤5 s of accepted buffer | ≤30 min | Chunk checksum and async durable store; ordinary quota | Store outage/restart and incomplete-manifest recovery |
| Finalized/report/anticheat/Atlas evidence | Zero finalized manifests; accepted in-flight chunk RPO ≤1 s | ≤30 min | Reserved queue/store, checksum tree, encrypted immutable copy and legal-hold index | Quarterly sampled checksum/restore and annual full evidence drill |
| Configuration/content/provenance | Last accepted version; zero accepted change loss | ≤10 min | Atomic versioned snapshots and daily encrypted repository backup | Restore then validate/migrate/rollback test |
| Redis/cache/presence | No durable RPO guarantee | ≤5 min | Reconstructed from SQL/backends; optional HA | Flush/restart/partition chaos suite |
| Proxy/CloudNet service | No session state treated as durable | ≤5 min replacement | Declarative config + service discovery | Drain/crash/recreate/transfer test |

Backups use authenticated encryption with secret-provider keys, distinct backup credentials and off-host storage. Key rotation is every 180 days; destroying a retired key requires proof no retained backup needs it. Restore credentials and production write credentials are separate.

## Quotas and alarms

- SQL pool: min 4/max 16 shared; max 24/backend proxy; wait queue bounded at 200; p99 wait ≤10 ms nominal.
- Redis: max 16 connections/backend; command queue 5,000; message dedupe 250,000 entries/24 h bounded by time and size.
- Replay: ordinary 100 GiB/server default, evidence reserve 25 GiB or 20% of configured capacity (whichever is greater), 1 GiB/match hard cap, 256 KiB chunk target.
- Recovery spool: 2 GiB/server; admission stops at 80%, critical alarm at 90%, no overwrite.
- Audit: 10 GiB active plus archive; never drop authorization/security/hold changes to make room for ordinary diagnostics.
- World operations: two concurrent clones and one attach/reset owner-thread phase; disk free alarm at 20%, admission stop at 10%.

Warnings fire at 70% and 80%, critical at 90% or predicted exhaustion inside 24 hours. Metrics labels are bounded and contain no raw player/case IDs.

## Provider failure defaults

| Failure | Safe behavior |
|---|---|
| Discord/webhook/external stats | Circuit-open and queue bounded notifications; gameplay and local stats continue; dead-letter is operator-visible |
| PlaceholderAPI/Vault/LuckPerms/provider absent | Capability reports unavailable; native fallback where defined; no startup failure unless operator marked provider mandatory |
| World/NPC/hologram provider | Use approved built-in provider if capability-equivalent; otherwise block affected arena/content activation with precise diagnostic, not global crash |
| Anticheat | No-provider telemetry; gameplay continues; Atlas case never invents alert data |
| SQL | Follow network spec: pause unsafe admissions/purchases/claims/finalization; bounded encrypted recovery journal for eligible active matches |
| Redis/proxy | Local shared play may continue; cross-node admission/party/private/reservation stops; no duplicate ownership |
| Replay store | Evidence reserve/buffer first; ordinary recording degrades sampling or pauses visibly; protected evidence never silently drops |
| Object/backup store | Live gameplay continues; release/retention deletion proof and new legal-hold archival pause until safe |
| Scripting | Definition/action fails typed; configured native fallback runs; no partial mutation |

## Drills and acceptance

Monthly: automated backup verification, key/secret expiry, quota and no-restore-with-production-credentials checks. Quarterly: SQL/config/evidence restore into an isolated environment and measured RPO/RTO. Before each major release: Redis/SQL/proxy partition, backend crash/drain, disk-full, replay saturation and secret rotation. Annually: full disaster recovery and legal-hold/export/delete exercise.

A drill records scenario, immutable artifact/config/data checksums, start/end, measured RPO/RTO, lost/reconciled operations, security observations, owner and corrective action. Failure blocks release or service expansion until corrected and rerun.
