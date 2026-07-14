# ADR-0006: Runtime artifacts and compatibility matrix

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-003, RC-004, RC-022
**Requirements:** `ZBW-READY-001`, `ZBW-READY-002`, `ZBW-READY-006`

## Decision

Use a platform-independent Java 8 core and separately compiled legacy/transitional/intermediate/modern server artifacts plus BungeeCord and Velocity artifacts. Certify only the exact server/JDK/client rows in `docs/RUNTIME_COMPATIBILITY_MATRIX.md`; server-runtime and translated-client claims are independent.

## Alternatives and consequences

A Java 21 universal JAR cannot start on legacy JVMs; a reflection-heavy single adapter would mix version logic into core. Multiple artifacts increase CI/storage but preserve mandatory 1.8 support, type safety and explicit bytecode/runtime testing. Shared schemas/domain source prevent rule divergence.

## Controls

Forbidden-import/class-version gates, hash-locked fixtures, full per-row E2E, no server binary redistribution and complete fallback fixtures. Adding a runtime requires a new exact row; failure never silently removes gameplay.
