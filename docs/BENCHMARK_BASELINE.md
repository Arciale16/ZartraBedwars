# Performance Benchmark Baseline

**Status:** Accepted
**Decisions:** RC-029, RC-062
**Requirements:** `ZBW-READY-009`, `ZBW-READY-017`, `ZBW-QA-004`, `ZBW-OPS-006`

The baseline measures CPU, **memory**, storage, network, TPS/MSPT and end-to-end latency under the same immutable workload.

## Reference environment

- Dedicated Linux host: AMD Ryzen 9 7950X, eight physical cores pinned to the test workload, SMT recorded, fixed performance governor; equivalence requires single-thread SPEC-like calibration within ±5%.
- 32 GiB DDR5-6000, local PCIe 4 NVMe sustaining ≥3 GB/s sequential and ≥300k random-read IOPS, ext4, ≥100 GiB free.
- Ubuntu Server 24.04 LTS x86-64; Eclipse Temurin patch levels from the runtime matrix; G1GC; no unrelated processes.
- Database/Redis proxy profile: separate equivalent hosts, 1 Gbit/s network, round-trip p50 ≤0.5 ms and p99 ≤1.5 ms, no packet loss in the nominal run.
- Paper fixtures and plugin artifact are immutable SHA-256 inputs. JVM flags, heap, configs, map hashes, seed, bots, providers and dataset are archived with results.
- Five measured runs after 10 minutes JVM/world warm-up; each run lasts 30 minutes. Report p50/p95/p99/max and 95% confidence interval. A result passes only if all five runs meet every hard limit.

## Workload profiles

| Profile | Heap | Players | Arena/world load | Data/integrations | Purpose |
|---|---:|---:|---|---|---|
| `SMALL` | 4 GiB | 40 concurrent | 10 configured, 4 loaded, 2 active 4v4v4v4 matches | SQLite, 50k player rows, replay on, 100 cosmetics | Single community server |
| `SHARED_40` | 12 GiB | 160 concurrent | 100 configured, 40 loaded, 10 active mixed solo/doubles/3v3v3v3/4v4v4v4; two resets/minute | MariaDB, Redis cache optional, 1m players, all 300 cosmetics, replay/PAPI/NPC/holograms | Mandatory shared-server scale |
| `PROXY_4` | 8 GiB/backend | 400 concurrent across four backends + two proxies | 16 active arenas, 80 warm/configured, 20 transfers/minute, one backend drain/restart | MariaDB/Redis separate, 10m stats rows, parties/private games, replay/object store stub, PAPI/Discord outage injection | Horizontal scale and consistency |

Bots execute deterministic movement, combat, shop purchase, block placement/break, item use, death/respawn, chat-disabled replay events, cosmetics, GUI and placeholders. `SHARED_40` uses 70% active players, 20% lobby/GUI and 10% spectators/replay viewers.

## Hard thresholds

| Metric | SMALL | SHARED_40 | PROXY_4 | Measurement rule |
|---|---:|---:|---:|---|
| TPS | p99 window ≥19.9 | p99 window ≥19.8 | each backend p99 ≥19.8 | one-minute windows; no catch-up masking |
| Whole-server MSPT | p95 ≤30 ms; p99 ≤40 ms | p95 ≤40 ms; p99 ≤50 ms | p95 ≤40 ms; p99 ≤50 ms/backend | spark/JFR-compatible tick telemetry |
| Zartra tick contribution | p95 ≤1 ms; p99 ≤3 ms | p95 ≤2 ms; p99 ≤5 ms | p95 ≤2 ms; p99 ≤5 ms | instrumented handler/scheduler time, no double count |
| Main-thread blocking I/O | 0 calls | 0 | 0 | agent/static instrumentation for JDBC, Redis, HTTP and filesystem |
| Heap after full GC | ≤2.5 GiB | ≤9 GiB | ≤6 GiB/backend | after warm-up and at end |
| Retained-growth leak | <2% over final 20 min | <3% | <3% | normalized for live players/worlds; no unbounded classloader/thread growth |
| GC pause | p99 ≤25 ms; max ≤100 ms | p99 ≤35 ms; max ≤150 ms | same | JFR/GC log |
| Arena template clone (512 MiB) | p95 ≤8 s | p95 ≤12 s | p95 ≤12 s | async bytes plus owner-thread attach separately reported |
| Arena reset | p95 ≤3 s; p99 ≤5 s | p95 ≤5 s; p99 ≤8 s | p95 ≤5 s; p99 ≤8 s | ready-for-admission duration |
| World attach owner-thread work | p99 ≤10 ms/tick | p99 ≤15 ms/tick | p99 ≤15 ms/tick | split across ticks if necessary |
| Cached PlaceholderAPI call | p95 ≤0.5 ms; p99 ≤1 ms | p95 ≤1 ms; p99 ≤2 ms | same | zero DB/Redis/network calls; 100k samples |
| GUI open from cached data | p95 ≤25 ms | p95 ≤50 ms | p95 ≤75 ms | click-to-first-render, async loading state allowed |
| SQL local query/service | p95 ≤8 ms; p99 ≤20 ms | p95 ≤15 ms; p99 ≤40 ms | p95 ≤20 ms; p99 ≤60 ms | application timing; pool wait separately ≤10 ms p99 |
| Redis operation | N/A or p95 ≤3 ms | p95 ≤5 ms; p99 ≤15 ms | p95 ≤5 ms; p99 ≤15 ms | including serialization, excluding deliberate outage |
| Reward/stat business commit | p95 ≤25 ms; p99 ≤75 ms | p95 ≤50 ms; p99 ≤150 ms | p95 ≤75 ms; p99 ≤200 ms | idempotent SQL outcome acknowledged |
| Replay enqueue on tick thread | p99 ≤200 μs | p99 ≤200 μs | p99 ≤200 μs | allocation and queue offer included |
| Replay encoder throughput | ≥2× realtime for one match | ≥15 concurrent realtime streams with <60% worker saturation | ≥6/backend with <60% | no evidence drop; bounded ordinary degradation |
| Replay finalized manifest | p95 ≤5 s after match | p95 ≤10 s | p95 ≤15 s | checksum and durable metadata complete |
| Proxy reservation/transfer API | N/A | N/A | p95 ≤50 ms; p99 ≤150 ms | backend reservation acknowledgement, network RTT included |
| Message duplicate suppression | N/A | 100% | 100% | 100k replayed/duplicated envelopes, zero duplicate business effects |

No user-visible operation may wait without a configured deadline. Default deadlines: SQL 2 s, Redis 500 ms, proxy reservation 2 s, webhook 3 s, replay payload 10 s; these are failure bounds, not performance targets.

## Saturation and degradation tests

1. Fill each queue/cache/pool to 80%, 100% and overflow; confirm bounded rejection, metrics and documented user result.
2. Pause SQL for 30 seconds, Redis for 60 seconds, replay store for five minutes and Discord indefinitely. Gameplay follows `OPERATIONAL_DEFAULTS.md`; no tick-thread stall or duplicate reward occurs.
3. Inject 100 ms/1% loss between proxy components and restart one backend during transfers. Epoch fencing and reservation idempotency preserve ownership.
4. Run all 300 cosmetics at configured maximum visible density. Budgets/culling activate before the packet and tick limits; gameplay feedback is never culled.
5. Replay ordinary and protected evidence traffic compete for capacity; evidence reserve remains available and any ordinary sampling degradation is explicit/audited.

## Regression and evidence rules

- A hard-threshold failure blocks `VERIFIED`.
- A p95/p99, allocation, storage or network regression ≥10% against the last accepted baseline blocks unless the new result is better in a documented trade-off and an ADR accepts it.
- Results include commit, artifacts/checksums, hardware, OS/JDK/JVM flags, fixture hashes, configuration, datasets, random seed, raw time series, JFR/profile, SQL plans, Redis statistics and analyzer version.
- Microbenchmarks use JMH and are advisory unless a requirement names them; scenario results are authoritative.
- Performance data contains synthetic IDs only and is retained for the project lifetime as non-player engineering evidence.
