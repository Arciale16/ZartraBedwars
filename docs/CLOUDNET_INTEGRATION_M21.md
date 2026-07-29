# M21 CloudNet service integration

Requirement coverage: `ZBW-DEPLOY-003`, `ZBW-DEPLOY-005`, `ZBW-OPS-006` and
`ZBW-ADDON-226..235`.

## Ownership boundary

`zbw-cloudnet` is an optional service-lifecycle adapter. It discovers service metadata, starts
configured templates, drains/stops services, replaces crashed instances and projects
capacity/health into the M20 backend registry port. It does not select destinations, reserve
players, perform matchmaking or own a match. M19 remains the coordination and fencing owner;
M20 remains the proxy routing owner; SQL and the existing domain modules remain authoritative.

The reactor has no CloudNet binary dependency. `CloudNetGateway` is implemented at deployment
composition time against the operator-installed, licence-approved CloudNet API. Absence or API
incompatibility yields the standard optional-provider disabled/unavailable lifecycle and does not
prevent startup.

## Concurrency and scaling

All gateway calls enter `BoundedCloudExecutor`, whose queue and worker counts are validated and
bounded. Its rejection policy never executes work on the caller, so a CloudNet callback cannot
silently become Paper owner-thread work.

Scaling uses minimum/maximum service bounds, warm free capacity, distinct scale-up/down thresholds,
consecutive-observation hysteresis, cooldown and a maximum action count per reconciliation. A
distributed lifecycle action proceeds only in M19 `NORMAL` degradation mode and after acquiring a
monotonic fencing token. During Redis failure or partition, unsafe cross-node scaling and crash
replacement stop; already running local gameplay is not mutated.

Service observations use positive instance epochs and metadata revisions. Duplicate or stale
callbacks are rejected. Discovery is sorted by canonical service ID, duplicate IDs fail closed,
and operation IDs make starts duplicate-safe within a bounded 4,096-entry window.

## Ephemeral services and recovery

The existing neutral `PRIVATE_GAME` and `REPLAY_VIEWER` service kinds use the same lifecycle request
path as arena services. Only service capacity/lifecycle is transported; private-game ownership
remains with M20 and replay data/lifecycle remains with M17.

On crash, a fresh offline observation removes the exact backend epoch from proxy eligibility and a
fenced start requests a replacement from the same template. Stale or repeated crash callbacks are
rejected. CloudNet unavailability returns retryable typed failures; malformed or duplicate
metadata returns a permanent validation failure.

## Operations and security

Provider health exposes stable diagnostic codes only. Metadata is restricted to namespaced IDs,
safe backend tokens, lifecycle, capacity, epoch, revision and timestamps. Credentials, endpoints,
player identities and replay/moderation payloads are not part of the adapter contracts.

Standalone fallback is the operator's static M20 backend registry with autoscaling disabled.
Operators must acquire and audit the exact supported CloudNet runtime independently before
installing a gateway binding; no CloudNet or server binary is resolved, stored, shaded or
redistributed by this repository.
