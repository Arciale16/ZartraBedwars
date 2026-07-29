# M20 proxy networking runbook

## Scope and authority

M20 implements proxy routing and scalable-deployment transport for `ZBW-DEPLOY-002..004`,
`ZBW-READY-013`, `ZBW-READY-014` and the proxy-coordination portions of
`ZBW-ADDON-041..060`, `ZBW-ADDON-102..107`, `ZBW-ADDON-252..259`,
`ZBW-ADDON-291..299`, `ZBW-ADDON-387` and `ZBW-ADDON-464..473`.

The proxy routes owner-approved intents. M10 owns matchmaking and selector policy; M11 owns item
rotation and resource behavior; M12-M18 own progression, objectives, profiles, statistics,
PlaceholderAPI, replay and Atlas. SQL remains durable authority and M19 Redis remains ephemeral
coordination. Neither BungeeCord nor Velocity creates a match, mutates a game, finalizes a durable
effect or treats an ephemeral reservation as business truth.

## Operating bounds

| Control | Hard bound |
|---|---:|
| Registered backend epochs | 1,024 |
| In-flight admission reservations | 5,000 |
| Retained consumed-token records | 10,000, expiring with their 15-second token |
| Transfer token lifetime | 15 seconds |
| Destination attempts | 3 |
| Authenticated payload | 64 KiB |
| Authenticated message rate | 100/s, burst 200 |
| Nonce records | 10,000 |
| Sender/receiver clock skew | ±30 seconds |
| PROXY_4 local p95/p99 | 5 ms / 15 ms |

Adapter sends return `CompletionStage` and do not wait on a proxy event loop or Minecraft owner
thread. Routing reads bounded in-memory snapshots only. Network, Redis and durable owner work must
complete through their existing asynchronous ports.

## Health and degradation

`ProxyAdapterRuntime.diagnostic` exposes only degradation state, a stable sanitized code, backend
count, pending-operation count and observation time. It never exposes endpoints, keys, nonces,
payloads, player names or domain records.

| State | Operator meaning | Safe action |
|---|---|---|
| `NORMAL` | Coordination and backend link are available | New owner-approved transfers may proceed |
| `LOCAL_ONLY` | Backend transport is unreachable | Stop remote reservations; retain safe local owner behavior |
| `RESERVATIONS_PAUSED` | Redis coordination is unavailable or the network is partitioned | Stop all new cross-node reservations |
| `DRAINING` | Proxy is intentionally leaving service | Route no new admissions; let bounded claims expire |
| `OFFLINE` | Adapter runtime is stopped | Reject messaging, routing and transfer work |

Availability callbacks update only a cached snapshot. They must not perform network work or call game
logic. Recovery changes the snapshot to `NORMAL` only after M19 health, backend reachability and
current epochs have been confirmed.

## Failure and recovery procedure

1. Stop new reservations on Redis loss, a proxy/backend partition, unknown schema, signature failure
   or stale epoch. Do not fall back to an uncoordinated cross-node write.
2. Expire backend heartbeats before every route. An expired, unhealthy, draining or offline backend
   is never selected. A replacement registers a strictly newer instance epoch.
3. Treat transfer acknowledgement loss as an unknown outcome. Retry consumption with the same token;
   the authoritative coordinator returns `DUPLICATE` without a second admission.
4. Never mint a replacement token for the same subject while its prior bounded reservation exists.
   Multiple proxies must use the same M19/backend admission authority; process-local coordinators are
   valid only for an explicitly single-proxy topology.
5. After a proxy crash, allow outstanding claims to complete at the backend or expire. A restarted
   proxy rebuilds registry state from fresh heartbeats and cannot revive an old backend epoch.
6. After a backend crash, reject its tokens as stale and route only after a newer epoch has registered
   and produced a healthy heartbeat.
7. Minor protocol versions may overlap during a rolling deployment. A major mismatch fails closed and
   must not be acknowledged. Resume only after all peers use a compatible schema.
8. Record the diagnostic transition and recovery duration. Escalate repeated partitions, nonce/rate
   violations or a recovery that exceeds the deployment objective.

## Security response

Messages are authenticated before payload bytes reach an adapter decoder. HMAC-SHA-256 covers
protocol, environment, audience/destination, key ID, nonce, issue time, deadline, payload length and
payload. Reject signature forgery, destination substitution, unknown keys, wrong audience or
environment, expired deadlines, nonce replay, ±30-second clock-window violations, oversize payloads
and peer rate excess. Rotate keys by overlapping key IDs; secrets remain external references.

A transfer token is single-use, audience-bound, epoch-bound and expires in at most 15 seconds. A token
is not a durable authorization record and must travel inside the authenticated transport envelope.

## Multi-node and performance certification

`ProxyFailureSecurityPerformanceTest` exercises two proxy runtimes against one shared admission
authority, four deterministic backend candidates, crash/restart, acknowledgement loss, duplicate
intent/token handling, heartbeat expiry, stale epochs, Redis loss, partitions and rolling minor
schemas. The adapter contract suites execute the same lifecycle, authentication and asynchronous
transfer semantics for BungeeCord and Velocity.

The deterministic `PROXY_4` local benchmark enforces p95 at most 5 ms and p99 at most 15 ms for
bounded registry selection. Before production rollout, repeat failure and latency tests against the
actual proxy/backend/Redis/TLS topology and archive p50/p95/p99, queue high-water, heap retention,
reconnect, rolling deployment and partition results. Shared-CI simulation is regression evidence;
it does not replace deployment-specific network evidence.

## Verification map

| Scenario | Automated evidence |
|---|---|
| proxy crash, acknowledgement loss, duplicate transfer and restart | `ProxyFailureSecurityPerformanceTest` |
| backend crash, stale registry/epoch, unhealthy/draining/offline routing | `ProxyFailureSecurityPerformanceTest`, `ProxyRuntimeTest` |
| Redis unavailable and proxy/backend partition | `ProxyFailureSecurityPerformanceTest` |
| forgery, nonce replay, substitution, expiry, audience, clock, rate and payload bounds | `ProxyFailureSecurityPerformanceTest`, `ProxyRuntimeTest` |
| multiple proxies/backends, rolling schema and bounded latency | `ProxyFailureSecurityPerformanceTest` |
| Velocity/Bungee semantic equivalence and nonblocking transport | both `AdapterContractTest` suites |
| private, spectate, rejoin, play-again, map and owner-version flows | `CrossServerWorkflowTest` |

No automated test grants Redis or the proxy durable authority. Duplicate durable effects remain
prevented by the owning SQL inbox/uniqueness/compare boundary.