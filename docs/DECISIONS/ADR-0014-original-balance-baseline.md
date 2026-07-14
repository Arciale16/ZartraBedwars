# ADR-0014: Original balance baseline

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-059
**Requirement:** `ZBW-READY-015`

## Decision

Ship `zbw:standard-v1` from `docs/BALANCING_BASELINE.md` as the original configurable default for match timing, resources/generators, shops/upgrades, modes and rewards. Profiles are immutable-versioned and replaceable/extendable through validated configuration and APIs.

## Consequences and controls

Numbers are starting product policy, not copied network parity. Seeded golden simulations, range/reference validation, telemetry review, versioned migration/rollback and owner approval gate every change; no self-tuning production balance is allowed.
