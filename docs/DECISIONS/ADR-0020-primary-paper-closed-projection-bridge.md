# ADR-0020 — Primary Paper closed projection bridge

**Status:** Accepted technical refinement
**Date:** 2026-07-15
**Requirements:** `ZBW-GAME-001..003`, `ZBW-GAME-006`, `ZBW-GAME-008`,
`ZBW-GAME-010`, `ZBW-DEPLOY-001`

## Context

The approved checksum-locked Paper API mirror is deliberately non-transitive. Direct
references to Paper player signatures cause javac to require unapproved Adventure
artifacts even though Paper supplies them at runtime. Adding or bundling those artifacts
would violate the pre-resolution dependency and redistribution policy.

## Decision

Keep game rules and semantic values in Java-8 `zbw-game`. The Java-21 primary adapter
uses a package-private, allow-listed reflection bridge only for server-owned Paper value
construction and calls. Every class, method, argument type, material and enum value is
validated; failures are explicit and never converted to unsupported fallbacks.

The bridge is closed implementation detail, is not public API and cannot be used by
extensions. Exact checksum-locked Paper E2E is mandatory. New direct dependencies remain
default-denied until separately licensed, locked and approved.

## Consequences

- No unapproved dependency or proprietary binary is introduced.
- Core/application code remains platform-neutral Java 8.
- Compile-time signature drift is supplemented by exact-runtime certification.
- M22 remains responsible for legacy/intermediate adapters and complete compatibility.
