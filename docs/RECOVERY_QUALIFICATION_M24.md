# M24 Recovery Qualification

**Status:** procedures and owner-module contracts verified; production drills pending
**Requirements:** `ZBW-QA-001/003/005`, `ZBW-OPS-005/008`, `ZBW-READY-014/016/017`

M24 exercises existing recovery boundaries and never becomes a second persistence or coordination
owner.

| Scenario | Repository evidence | Binding drill |
|---|---|---|
| Backup validation and rollback | M04 checksum/encryption/recovery contracts; M23 dry-run, journal, automatic restore and explicit rollback tests | Restore representative encrypted backup with separate credentials |
| SQL crash/restart | SQLite and external MySQL/MariaDB transaction, restart, inbox/outbox and backup/restore suites | Kill/restart primary, reconcile zero acknowledged loss, RTO no more than 15 minutes |
| Redis flush/restart/partition | M19 cache rebuild, bounded retry, dedupe, lease/fencing and unavailable-mode tests | Flush and partition target Redis; unsafe cross-node admission remains stopped; RTO no more than 5 minutes |
| Backend/proxy crash | M20 stale epoch, acknowledgement loss, replayed token and duplicate-admission tests | Drain, kill and recreate backend/proxy with no double admission |
| Replay/Atlas persistence | transactional append, malformed-row, archive, rollback and restart suites | Restore ordinary and protected evidence, verify checksum/access/hold |
| Migration failure | mandatory backup, zero-mutation dry-run, duplicate rejection and failed-apply restore | Run against lawful representative source layouts |

## Procedure

Every drill starts from immutable artifact/config/data hashes and an isolated environment. Record
start/end, measured RPO/RTO, acknowledged and reconciled operation counts, integrity queries,
security observations and corrective action. Production credentials must not be used for restore.

Release requires the pre-release SQL/Redis/proxy/backend/disk-full/replay-saturation/secret-rotation
exercise from `OPERATIONAL_DEFAULTS.md`. Quarterly and annual exercises remain operational
obligations after release. Because those external systems are unavailable in this checkout, M24
records them as `PENDING_EXTERNAL_DRILL`, not as passed.
