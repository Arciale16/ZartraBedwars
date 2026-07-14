# Architecture Decision Records

Major decisions are immutable records. Copy `ADR-TEMPLATE.md` to `ADR-NNN-short-title.md`, assign the next number, and set status to `Proposed`. Superseded ADRs remain in the repository and link to their replacement. Every ADR lists affected requirement IDs and updates PRD, architecture, milestones, traceability, risks and tests as applicable.

The initial pre-code decision queue is resolved. Future major decisions use the next ADR number and may not overwrite accepted records. Implementation-time ADRs are created only when a concrete design changes; they do not reopen the accepted pre-code policies or authorize scope reduction.

## Accepted owner decisions

| ADR | Decision | Resolves |
|---|---|---|
| [ADR-0001](ADR-0001-resource-scarcity.md) | RESOURCE SCARCITY is the original eleventh Private Games modifier | RC-072 |
| [ADR-0002](ADR-0002-original-content-provenance.md) | Original/configurable starter content and default-deny asset provenance | RC-073 |
| [ADR-0003](ADR-0003-discord-provider-topology.md) | Optional webhook, external-bot and custom Discord providers | RC-074 |
| [ADR-0004](ADR-0004-minecraft-1-8-fallbacks.md) | Mandatory Minecraft 1.8 adapters and safe compatibility fallbacks | RC-075 |
| [ADR-0005](ADR-0005-dependency-licensing.md) | Exact-version, default-deny dependency and redistribution audit | RC-076 |
| [ADR-0006](ADR-0006-runtime-artifacts-and-matrix.md) | Multi-artifact toolchains and exact server/client matrix | RC-003, RC-004, RC-022 |
| [ADR-0007](ADR-0007-dependency-and-provider-baseline.md) | Exact dependency/framework/provider selections and scopes | RC-021, RC-024, RC-027 |
| [ADR-0008](ADR-0008-declarative-scripting-sandbox.md) | Disabled declarative capability scripting sandbox | RC-018 |
| [ADR-0009](ADR-0009-performance-and-quality-gates.md) | Reference benchmarks and numeric quality gates | RC-029, RC-062 |
| [ADR-0010](ADR-0010-privacy-retention-and-visibility.md) | Replay privacy, retention/hold and default visibility | RC-040, RC-041, RC-065 |
| [ADR-0011](ADR-0011-network-security-and-authority.md) | Authenticated messaging, SQL authority and distributed consistency | RC-046, RC-050 |
| [ADR-0012](ADR-0012-cosmetic-production.md) | Five-batch original 300-cosmetic plan | RC-017 |
| [ADR-0013](ADR-0013-clean-room-addon-provenance.md) | Clean-room addon/migration provenance | RC-043, RC-071 |
| [ADR-0014](ADR-0014-original-balance-baseline.md) | Original configurable balance baseline | RC-059 |
| [ADR-0015](ADR-0015-operational-recovery-defaults.md) | RPO/RTO, quotas, degradation and recovery drills | RC-061 |
| [ADR-0016](ADR-0016-project-licensing-model.md) | Proprietary product and separately licensed public SDK model | RC-066 |
| [ADR-0017](ADR-0017-extension-metadata-format.md) | Restricted schema-versioned extension metadata and deterministic compatibility validation | M02 implementation decision |

Accepted ADRs settle policy and architecture; they do not claim implementation. Their execution gates remain in the PRD, traceability matrix and milestone exit criteria.
