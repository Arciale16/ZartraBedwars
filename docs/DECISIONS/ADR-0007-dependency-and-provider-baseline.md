# ADR-0007: Dependency, framework and provider baseline

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-021, RC-024, RC-027
**Requirements:** `ZBW-READY-005`, `ZBW-READY-007`, `ZBW-READY-008`

## Decision

Adopt the exact selection, scopes and immutable acquisition policy in `docs/DEPENDENCY_LICENSE_AUDIT.md`. Use manual constructor DI and project-owned command/GUI DSLs. External plugins are compile-only/provided; proprietary Vulcan is operator-installed behind the neutral anticheat SPI; Grim uses the audited public baseline.

## Alternatives and consequences

Floating upstream versions are unreproducible; bundling integrations creates licence/classpath risk; framework-heavy core harms Java 8 compatibility. Exact pins and neutral ports add maintenance but make absence, upgrade and redistribution testable. Official snapshot-only APIs are privately commit/checksum mirrored, never floated or redistributed.

## Controls

Pre-resolution checksum/licence gate, locked SBOM, no dynamic/range/SNAPSHOT release coordinates, artifact scan, safe provider absence and full contract tests. A blocked provider does not block core gameplay or remove its eventual adapter scope.
