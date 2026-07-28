# M19 Redis distributed-consistency runbook

## Scope and authority

M19 implements ephemeral coordination for `ZBW-DEPLOY-006`, `ZBW-DEPLOY-008`,
`ZBW-DEPLOY-009`, `ZBW-READY-013`, `ZBW-READY-014` and the Redis portion of
`ZBW-ADDON-387`. SQL and the owning M11/M15/M17/M18 services remain authoritative.
Redis never finalizes rewards, statistics, item rotations, replay state, Atlas cases or verdicts.

M20 proxy routing, Velocity/BungeeCord, CloudNet and provider integration are outside M19.

## Operating bounds

| Control | Hard bound |
|---|---:|
| Backend connections | 16 |
| Pending adapter operations | 5,000 |
| Coordination payload | 256 KiB |
| Local deduplication metadata | 250,000 entries, 24 hours |
| Version metadata | 100,000 entries |
| Atlas coordination reservations | 10,000 entries, maximum 15-minute lease |
| Stream retry budget | 3 |
| Peer rate | 100/s, burst 200 |
| Nonce replay window | 5 minutes |
| Coordination recovery objective | 5 minutes |

All Lettuce calls are asynchronous. Completion callbacks are moved to the bounded Redis executor;
Paper or domain behavior must be scheduled separately by its owner and never run on Lettuce event
loops. No Redis operation may block a Minecraft owner/tick thread.

## Health and diagnostics

`RedisHealth` and `RedisDiagnostics` expose only availability, degradation mode, circuit state,
sanitized diagnostic code, queue depth and bounded metadata counts. They never expose URI,
credentials, HMAC keys, nonce values, payloads or player/case/replay identities.

Operator interpretation:

- `AVAILABLE/NORMAL`: coordination may proceed.
- `DEGRADED/CROSS_NODE_PAUSED`: stop new cross-node reservations and finalization that cannot be
  proven safe; safe local operation may continue.
- `UNAVAILABLE/CROSS_NODE_PAUSED`: Redis is absent or not started. Treat caches as lost and rebuild
  from their durable owners.
- `OPEN` circuit: do not manually retry in a tight loop. Wait for the bounded half-open probe.

## Failure and recovery procedure

1. Confirm SQL health before taking any recovery action. Never reconstruct durable state from Redis.
2. Pause unsafe cross-node Atlas/admission reservations. Existing durable SQL records remain valid;
   an ephemeral lease alone proves nothing.
3. Restore Redis connectivity or restart the client. Reconnect uses finite exponential jitter and
   the circuit breaker; there is no infinite retry loop.
4. Recreate consumer groups if absent and resume from the last durably acknowledged stream cursor.
   Duplicate delivery is expected and removed by operation IDs plus the SQL inbox/uniqueness boundary.
5. Rebuild statistics, leaderboard, rotation, replay-metadata and availability caches from their
   owning SQL/service APIs. A missing local version always requests a rebuild.
6. Verify schema version. Unknown schemas fail closed and must not be acknowledged.
7. Resume cross-node reservations only after health is `AVAILABLE`, cache rebuild has completed and
   the newest fencing epoch is known.
8. Record elapsed recovery time through `RedisRecoveryTracker`. Escalate if it exceeds five minutes.

### Redis flush or complete cache loss

A flush is treated exactly like a new empty Redis instance. Do not restore Redis snapshots as
business truth. Rebuild metadata, recreate consumer groups, retain SQL inbox/outbox positions and
allow duplicate stream delivery. Bounded cache eviction follows the same path.

### Partition and stale lease

During a partition, new unsafe cross-node reservations return `CROSS_NODE_PAUSED`. Renewal accepts
only the exact latest holder/fencing epoch. Expired leases must be reacquired and receive a newer
token; repeated or stale tokens are rejected. Redis locks never protect money, rewards, statistics,
rotation state, replay lifecycle or Atlas verdicts without the owning SQL compare/idempotency check.

## Performance certification

`RedisCoordinationPerformanceTest` runs deterministic adapter-side equivalents for `SHARED_40` and
`PROXY_4`, asserting p95 at most 5 ms, p99 at most 15 ms and bounded metadata. This test contains no
network I/O and therefore detects local allocation/algorithm regressions without becoming dependent
on shared CI network noise.

Before production rollout, run the same limits against the deployment Redis/TLS topology on the
binding benchmark hardware and archive p50/p95/p99, throughput, connection count, queue high-water,
heap retention and reconnect/partition results. A network benchmark failure blocks rollout; it is
not permission to weaken the automated threshold.

## Verification map

| Scenario | Automated evidence |
|---|---|
| unavailable Redis, timeout, reconnect and circuit opening | `RedisAdapterTest`, `RedisFailureRecoveryTest` |
| duplicate and out-of-order delivery, bounded retry and cursor stop | `RedisAdapterTest`, `CoordinationBridgeTest` |
| schema mismatch and authentication failure | `RedisAdapterTest`, `RedisSecurityTest`, `CoordinationBridgeTest` |
| stale fencing, reservation conflict/race and partition pause | `RedisContractsTest`, `CoordinationBridgeTest` |
| flush, restart, complete cache loss and five-minute recovery | `RedisFailureRecoveryTest` |
| cache rebuild and item-rotation version synchronization | `CoordinationBridgeTest` |
| bounded SHARED_40/PROXY_4 equivalent coordination | `RedisCoordinationPerformanceTest` |

No test substitutes Redis delivery for the SQL idempotency/uniqueness boundary. Consequently,
duplicate transport cannot create a duplicate durable effect or an invalid finalization.
