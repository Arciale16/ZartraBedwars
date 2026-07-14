# ADR-0011: Network security and distributed authority

**Status:** Accepted
**Date:** 2026-07-14
**Resolves:** RC-046, RC-050
**Requirements:** `ZBW-READY-013`, `ZBW-READY-014`

## Decision

SQL owns durable state; Redis coordinates; a backend owns live match state only under an epoch-fenced lease. SQLite is single-JVM only and proxy scale requires MySQL/MariaDB. All messages follow `docs/NETWORK_SECURITY.md` authentication, integrity, version, time/nonce, dedupe, limit, TLS and secret rules. Outbox/inbox and idempotency yield exactly-once business outcomes over at-least-once delivery.

## Consequences and controls

Unsafe distributed operations pause during partitions instead of relying on eventual consistency. The design adds operation ledgers, leases and key rotation but prevents forged transfers/rewards and duplicate ownership. Forge/replay/partition/duplicate/rolling-upgrade suites are mandatory.
