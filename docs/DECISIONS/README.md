# Architecture Decision Records

Major decisions are immutable records. Copy `ADR-TEMPLATE.md` to `ADR-NNN-short-title.md`, assign the next number, and set status to `Proposed`. Superseded ADRs remain in the repository and link to their replacement. Every ADR lists affected requirement IDs and updates PRD, architecture, milestones, traceability, risks and tests as applicable.

Initial decision queue:

1. Runtime/version compatibility and Maven toolchains.
2. Module graph and API compatibility tooling.
3. SQL migration/outbox and distributed consistency.
4. Redis protocol, fencing and degradation policy.
5. Scheduler/thread ownership across Paper generations.
6. Replay format, fidelity, storage, encryption and retention.
7. Proxy message authentication and rolling upgrades.
8. Privacy jurisdiction, deletion and legal hold.
9. Reference performance hardware/workloads/budgets.
10. Canonical commands, permission nodes and compatibility aliases.
11. Provider supported-version and licensing matrix.
12. Custom script/action sandbox.
13. Original 300-cosmetic catalogue and asset licensing.

## Accepted owner decisions

| ADR | Decision | Resolves |
|---|---|---|
| [ADR-0001](ADR-0001-resource-scarcity.md) | RESOURCE SCARCITY is the original eleventh Private Games modifier | RC-072 |
| [ADR-0002](ADR-0002-original-content-provenance.md) | Original/configurable starter content and default-deny asset provenance | RC-073 |
| [ADR-0003](ADR-0003-discord-provider-topology.md) | Optional webhook, external-bot and custom Discord providers | RC-074 |
| [ADR-0004](ADR-0004-minecraft-1-8-fallbacks.md) | Mandatory Minecraft 1.8 adapters and safe compatibility fallbacks | RC-075 |
| [ADR-0005](ADR-0005-dependency-licensing.md) | Exact-version, default-deny dependency and redistribution audit | RC-076 |

Accepted ADRs settle policy and architecture; they do not claim implementation. Their execution gates remain in the PRD, traceability matrix and milestone exit criteria.
