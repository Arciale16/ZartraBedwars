# M17 replay architecture and closure

**Requirements:** `ZBW-REPLAY-001..010`, `ZBW-READY-009/010/011/016/017/018`

## Boundaries

M17 consumes immutable M08 match, M11 settlement and M12 projection facts. It does not
own or mutate those lifecycles. `zbw-replay-api` owns Java 8 identities, metadata,
events, timelines, sessions, access policy and asynchronous repository ports.
`zbw-replay` owns deterministic ingestion and playback. `zbw-replay-sql` is the only
replay JDBC adapter. Paper depends one way on the API and engine and performs no
synchronous persistence.

The SQL adapter preserves replay isolation, ordered append, duplicate protection,
compare-state transitions and restart-safe loading. Completed and archived sessions
are playable; failed or malformed sessions fail closed. Staff removal accepts failed
sessions only, while archival uses a compare-state repository transition.

## Runtime budgets

- Viewer admission is atomic and capped at 256 concurrent or opening sessions.
- Each visual scene is capped at 128 entities and 256 retained important events.
- Menu projections retain at most 128 participants and 64 important events.
- Staff queries accept limits from 1 through 100 and reject a provider response above
  100 rows before allocating the normalized result.
- Visual reconciliation runs at a two-tick cadence; first render and backward seek
  bypass cadence so deterministic state is never traded for throttling.
- The immutable persisted timeline remains subject to the normative 1 GiB per-match
  quota and 256 KiB payload-chunk target in `OPERATIONAL_DEFAULTS.md`. M17 does not
  introduce an in-memory payload cache.

These structural bounds complement, but do not replace, the three-profile replay
throughput, heap-growth and tick-thread enqueue certification owned by M24.

## Threading and determinism

Repository calls return `CompletionStage` and JDBC work runs only on the injected
executor. Paper applies completion on its owner-thread boundary before entering or
restoring spectator state. Playback and rendering consume the same ordered immutable
timeline; neither renderer nor viewer advances or rewrites replay state. Search
ordering is creation instant then replay ID, and event ordering is sequence then
non-regressing offset with duplicate event IDs treated idempotently.

## Cleanup and retention lifecycle

Stop, disconnect and plugin shutdown remove viewer/menu/visual projections and restore
the captured spectator state. A failed presentation or render tears down the runtime
session before returning a sanitized failure. Admission capacity is released after
load failure, stop and shutdown. Visual entity cleanup is idempotent even when the
platform entity has already disappeared.

Archival preserves immutable replay data and remains playable. Failed recordings are
not playable and may be removed only through the audited admin action. Ordinary
30-day retention, evidence retention, protected capacity, legal hold precedence and
hold-release deletion remain governed by `PRIVACY_AND_RETENTION.md`; concrete
deployment scheduling and release-scale restore/performance evidence remain M24
qualification work.

## Certification evidence

M17 tests cover immutable models, ordering, duplicates, malformed data, transactional
rollback, restart loading, playback/seek/snapshot behavior, access rejection, viewer
isolation, concurrent admission, visual bounds, archive/failed lifecycle, audit and
cleanup. Maven quality gates enforce compilation, tests, Checkstyle, SpotBugs, JaCoCo
and JavaDoc; governance validates the module graph, dependency lock, traceability,
dashboard and API compatibility.

No web viewer, Redis transport, external replay hosting, packet/NPC provider,
cinematic editor or M18 behavior is introduced by M17.
