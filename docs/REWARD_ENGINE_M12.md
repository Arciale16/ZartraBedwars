# M12 reward engine

## Processing model

1. Build a versioned immutable reward definition and delivery plan.
2. Atomically claim its idempotency key in the durable M04 transaction boundary.
3. Return the recorded outcome for a duplicate claim.
4. Queue an offline recipient without executing effects.
5. Deliver through a capability-specific adapter when available.
6. Record delivery or a sanitized failure record transactionally.
7. Retry within the configured finite attempt and expiration limits.
8. Apply the configured compensation after retry exhaustion.

The atomic claim and adapter-level idempotency requirement prevent crash/retry double grants. The
store is responsible for persisting plan, attempts, outcomes and audit evidence using the Phase 2
reward tables and M04 inbox/outbox facilities. Delivery adapters never own reward policy.

## Generic outputs and ownership

M12 can plan experience, persistent currency, generic entitlement, item, permission, allowlisted
command, title, badge, pass-point, token, loot, booster and extension-defined outputs. This is an
intent vocabulary, not implementation ownership: quests/achievements/pass are M13, cosmetics are
M14, statistics are M15, placeholders are M16, distributed transport is M19/M20, external providers
are M21 and compatibility is M22.

## Failure and recovery guarantees

- An unavailable recipient remains `PENDING` and can be delivered after reconnect/restart.
- A provider exception creates `FAILED` evidence without exposing its message or credentials.
- Expired plans become `EXPIRED` without delivery.
- Retry exhaustion becomes `COMPENSATED` after the compensation adapter succeeds.
- A `DELIVERED` outcome is terminal and later retries return the original evidence.
