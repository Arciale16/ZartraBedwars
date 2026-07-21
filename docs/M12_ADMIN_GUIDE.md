# M12 administration guide

Use the generated command and permission inventories for the exact labels and nodes. Administrative
surfaces inspect progression, rewards, currency accounts, immutable ledger entries and pending or
failed reward delivery without exposing secrets. Mutations require a granular M03 permission,
confirmation, correlation ID and an application-generated audit record.

Grant XP/reward and adjustment requests must carry a fresh idempotency key. Recalculation uses the
versioned level/prestige definitions and optimistic account revision. Duplicate execution returns
the earlier committed result; stale revisions fail closed. Recovery diagnostics are read-only and
retry requests preserve the original reward idempotency identity.

Never perform database work from the Paper owner thread. Operators are not implicitly authorized.
Statistics, PlaceholderAPI and provider integrations are not M12 administration surfaces.
