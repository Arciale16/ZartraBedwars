# ADR-0010: Privacy, retention and visibility

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-040, RC-041, RC-065
**Requirements:** `ZBW-READY-010`, `ZBW-READY-011`, `ZBW-READY-018`

## Decision

Apply `docs/PRIVACY_AND_RETENTION.md`: chat off, metadata-only replay default, fixed retention classes, participant/staff purpose access, private profiles/history, aggregate public leaderboards and anonymized Atlas. Legal hold overrides destruction only for scoped evidence; unrelated data is deleted and identity separated.

## Consequences and controls

Some operators must publish stricter/shorter jurisdictional policies before enabling capture. Encryption, export/deletion within 30 days, hold/release audit, cache tombstones, permission E2E and privacy golden tests are mandatory. No placeholder or general GUI exposes private case/hold data.
