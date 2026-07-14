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
