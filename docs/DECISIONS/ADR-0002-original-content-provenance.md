# ADR-0002: Original Content and Asset Provenance

- **Status:** Accepted
- **Date:** 2026-07-14
- **Owner:** Project owner
- **Affected requirements:** `ZBW-CONTENT-001..011`, `ZBW-PROG-006..008`
- **Resolves:** RC-073

## Context

The product needs substantial balance/content catalogues and 300+ cosmetics without copying proprietary networks or addon authors. Content must also remain replaceable and extensible.

## Decision

All code, names, definitions, messages, sounds, models, textures, effects and balance presets are independently authored or approved under exact licence/provenance records. The original starter definitions in `docs/ORIGINAL_STARTER_CATALOG.md` establish stable namespaced IDs and versioned schemas for shop/mode balance, quests, achievements, battle pass, cosmetics, private presets and semantic feedback.

`docs/ASSET_PROVENANCE.md` is the release authority for creative assets. Only an approved asset ID/origin/author/licence/use/redistribution/modification row with source hash may enter packaging. Catalogues support validate-then-swap configuration and public registries; extensions add new IDs without editing core.

## Alternatives considered

| Alternative | Benefit | Rejection reason |
|---|---|---|
| Clone familiar network content | Fast familiarity | Copyright/trademark/licence risk and owner prohibition |
| Code-only hard-coded catalogue | Simple initial implementation | Blocks safe operator content and API expansion |
| Versioned original content packs with provenance | Original, auditable, extensible | Selected |

## Consequences

- The current manifest correctly has zero packaged assets.
- Asset-bearing cosmetic definitions remain non-distributable until provenance approval.
- Every content/asset change triggers validation, migration and legal/provenance scans.
- The separate 300+ cosmetic count remains mandatory.

## Acceptance evidence

ID/schema/config round trips, golden balance tests, objective/reward reference checks, 300-definition count, asset hash/provenance scan, licence review and release artifact comparison are required.

## Updated documents

PRD, architecture, milestones, traceability, risk register, starter catalogue, provenance manifest, third-party notices and coverage report.
