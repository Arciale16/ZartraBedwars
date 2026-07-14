# ADR-0004: Mandatory Minecraft 1.8 Fallbacks

- **Status:** Accepted
- **Date:** 2026-07-14
- **Owner:** Project owner
- **Affected requirements:** `ZBW-COMPAT-001..009`, `ZBW-INT-010`
- **Resolves:** RC-075

## Context

Minecraft 1.8 lacks modern materials, data components, particles, sounds, text components, entities, packets and GUI/input features. Loading modern types on legacy runtime can crash before a fallback is possible.

## Decision

Treat 1.8 server runtime as mandatory. Use separate toolchain/bootstrap artifacts where required and isolate every platform type behind `zbw-compat-api`/`zbw-compat-v1_8`. Apply the complete mapping and degradation rules in `docs/COMPATIBILITY_FALLBACKS.md`.

Every unavailable feature receives a safe equivalent preserving gameplay and actionable information. Only a purely decorative sub-effect may be suppressed when no safe equivalent exists; suppression is configured, reported and tested.

## Alternatives considered

| Alternative | Benefit | Rejection reason |
|---|---|---|
| Via-only old client support | Lower maintenance | Does not satisfy mandatory 1.8 server-runtime support |
| Single Java 21/modern API JAR | Simple packaging | Cannot safely load on legacy runtime/JVM |
| Separate adapters/toolchains and semantic fallbacks | Safe and verifiable | Selected |

## Consequences

- No version-specific platform type may enter core signatures or configuration models.
- Every new semantic material/effect/UI/input needs a fallback row and fixture first.
- Some decorative fidelity is lower on 1.8, but functionality remains.

## Acceptance evidence

1.8 build/startup/gameplay/GUI/item/packet/cleanup/migration matrix, forbidden dependency tests, mapping completeness, unsupported-value mutation tests and task-equivalence checks.

## Updated documents

PRD, architecture, fallback matrix, milestones, traceability, risks and coverage report.
