# ADR-0009: Performance and quality gates

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-029, RC-062
**Requirements:** `ZBW-READY-009`, `ZBW-READY-017`

## Decision

Use `docs/BENCHMARK_BASELINE.md` and `docs/QUALITY_GATES.md` as hard milestone/release gates with fixed hardware, three deployment profiles, percentiles, module-aware coverage, mutation/static/vulnerability/API/architecture thresholds and archived evidence.

## Consequences and controls

Benchmarks require dedicated infrastructure and deterministic data but replace vague “minimal TPS impact” claims. Every hard failure or ≥10% regression blocks verification; only time-bounded owner-approved vulnerability exceptions exist. Microbenchmarks do not override scenario failures.
