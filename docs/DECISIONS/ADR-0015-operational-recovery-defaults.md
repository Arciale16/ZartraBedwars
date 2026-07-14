# ADR-0015: Operational recovery and degradation defaults

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-061
**Requirement:** `ZBW-READY-016`

## Decision

Adopt the per-data-class RPO/RTO, encryption, backup retention, quotas, alarms, drill cadence and safe provider degradation in `docs/OPERATIONAL_DEFAULTS.md`. Gameplay never depends on Discord; unsafe financial/admission/finalization work pauses when authority is unavailable.

## Consequences and controls

Reserved evidence capacity and frequent backups consume resources, while bounded degradation prevents silent loss or duplicates. Monthly checks, quarterly restores, pre-release chaos and annual disaster/privacy drills must meet recorded objectives.
