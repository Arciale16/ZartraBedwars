# M09 confirmation framework

A confirmation intent contains a cryptographically unpredictable typed ID, authenticated actor,
stable action and target, optimistic revision, correlation ID and expiry. Issuance is bounded.
Consumption atomically removes the intent before validation, making it single-use even when the
actor, action, target, revision or authorization check fails.

Expired, replayed, wrong-actor, wrong-action, wrong-target, stale-revision and revoked-authorization
attempts return distinct verdicts and immutable sanitized audit records. Command and GUI adapters
share this service. A destructive action first returns preview/confirmation data; only a successful
consume invokes the same underlying application use case.
