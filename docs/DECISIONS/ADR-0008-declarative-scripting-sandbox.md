# ADR-0008: Declarative scripting sandbox

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-018
**Requirement:** `ZBW-READY-004`

## Decision

“Script” is a disabled-by-default declarative capability graph under `docs/SCRIPTING_SECURITY.md`, not arbitrary JavaScript/JVM/shell execution. It runs on bounded workers with strict actions, scopes, time/instruction/memory limits and audit; application use cases perform all mutations.

## Consequences and controls

This is less expressive than general-purpose code but preserves custom logic without exposing filesystem, process, network, reflection, classloading, secrets or the server tick thread. Fuzz/escape/confused-deputy/load tests, signature/provenance checks and last-known-good activation are release gates.
