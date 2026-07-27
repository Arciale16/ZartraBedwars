# M18 Atlas moderation platform

## Closure scope

M18 materializes `ZBW-ATLAS-001..013` and `ZBW-ADDON-323..333` through three Java 8 modules and a Java 21 Paper adapter. Atlas references M17 replay evidence without copying replay data, queries M15 statistics, requests M12 rewards, emits M13 qualified outcomes and adapts query values to the existing M16 provider boundary. Those systems retain ownership.

## Runtime boundaries

- `zbw-atlas-api` owns immutable public case, evidence, review, privacy, repository and event contracts.
- `zbw-atlas` owns case/review workflow, eligibility, advisory verdict policy, audit and guarded staff-operation policies. It is Paper independent.
- `zbw-atlas-sql` owns checksum-locked transactional persistence and keeps the encrypted identity vault separate from community projections.
- `zbw-paper-modern` owns only asynchronous composition, exact permission checks, sanitized presentation, `/atlas` routing and owner-thread delivery.

All repository and integration calls return `CompletionStage`. Paper never joins or blocks those stages on the owner thread. Completion presentation is dispatched through the injected owner executor. Shutdown closes admission and clears the installed router.

## Controlled staff operations

`ZBW-ADDON-323..333` are represented by typed operations with exact dotted permission nodes. A request requires an opaque actor, target and reason, explicit confirmation and a non-immune target. The game-owned backend returns only opaque before/after references and a rollback token. Every accepted or denied attempt is audited; rollback is separately permissioned and confirmed. Atlas never exposes raw game mutation.

## Privacy and punishment safety

Community views contain case IDs, reference-only evidence, reviewer state, verdict labels and opaque audit references. Identity-vault content is absent. Community aggregation remains advisory and cannot apply permanent punishment; authoritative staff disposition is a separately permissioned application-port operation.

## Operational limits and handoff

The Paper provider adapter maintains only per-player cached query projections and never blocks PlaceholderAPI resolution. Distributed queues, Redis coordination, provider-specific integrations and release-scale qualification remain M19-M24 allocations. M19 is not started by this closure.
