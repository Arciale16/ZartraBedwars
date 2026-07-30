# M24 Threat Model and Security Qualification

**Status:** local repository qualification complete; deployment evidence pending
**Requirements:** `ZBW-QA-003/005`, `ZBW-GOV-007`, `ZBW-LICENSE-001..007`,
`ZBW-READY-005/010/011/013/018/019`

M24 reviews existing ownership and controls; it does not introduce another authorization,
moderation, replay, provider or network implementation.

## Assets and trust boundaries

| Asset | Boundary | Required control |
|---|---|---|
| Currency, rewards, progression, statistics and verdicts | SQL authority | transaction, revision, inbox/idempotency and audit; Redis/proxy messages never authorize durable effects |
| Replay and Atlas evidence | participant/staff/community projections | purpose-based access, default anonymization, identity-vault separation, retention and audited reveal |
| Secrets and provider credentials | M03 secret provider | references only, zeroized leases, redacted diagnostics, key identifiers and rotation |
| Proxy/Redis messages and transfer tokens | untrusted network | authenticate before parsing, schema/environment/audience/deadline/nonce validation, size/rate limits and fencing |
| Optional provider callbacks | operator-supplied plugin boundary | vendor-isolated gateways, optional lifecycle, bounded callbacks and absence-safe startup |
| World, player, inventory and entity state | Paper owner thread | typed owner dispatch; filesystem, SQL, Redis and network work remains off-thread |
| Migration source and backups | external filesystem/storage | read-only source, dry-run, validated backup, journal, atomic apply and rollback |

## Threat review

| Threat | Existing control and qualification evidence | Remaining evidence |
|---|---|---|
| Privilege escalation or wildcard permission drift | Generated M09-M14 permission inventories, canonical dotted namespace and M03 authorization; Paper privileged nodes default to `op`; `m24_qualification.py` rejects invalid declarations | Real-server operator-role acceptance |
| Identity or moderation-data disclosure | `PRIVACY_AND_RETENTION.md`, replay purpose access, Atlas anonymous projection and separated identity vault | Deployment export/delete/legal-hold exercise |
| Forged, replayed or oversized distributed message | HMAC/key ID, nonce/deadline/audience/schema checks and bounded rate/size tests in M19/M20 | TLS deployment and secret-rotation exercise |
| Provider classpath or lifecycle compromise | Neutral gateways, no vendor imports, optional `softdepend`, duplicate/incompatible/absent-provider suites | Exact provider provenance and runtime fixtures |
| Owner-thread blocking | Static Paper scan plus M05 thread guards; world filesystem steps require `Affinity.WORKER`; certification evidence writes on a dedicated thread | JFR/agent observation during all benchmark profiles |
| Dependency substitution or unlicensed packaging | Exact URL/hash/license locks, CycloneDX SBOM, notices, prohibited-binary scan and non-bundled provider policy | Provider lock completion and release-candidate scan |
| Replay, Redis or proxy loss causing duplicate durable effects | SQL remains authoritative; dedupe, epochs, leases and fencing fail closed | Partition/crash drills on the target topology |
| Migration corruption | deterministic plan, duplicate detection, dry-run, backup, journal, atomic apply and automatic restore | Representative lawful source datasets |

## Permission audit policy

Permissions must match `zartrabedwars.<segment>[.<segment>...]`; wildcard and vendor-native nodes are
not accepted as canonical product permissions. Destructive, staff, administration and rollback
permissions are deny-by-default to ordinary players. Commands and GUIs continue to invoke the same
authorized application use cases, so adapters cannot bypass policy.

## Result

Repository controls and deterministic negative-path tests are qualified. This document makes no
claim about live TLS, external provider binaries, production secrets or M22 runtime compatibility.
Those inputs remain explicit external gates in `build/m24-qualification.json`.
