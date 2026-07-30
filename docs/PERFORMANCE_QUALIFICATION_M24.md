# M24 Performance Qualification

**Status:** harness and static analysis verified; normative runtime runs pending
**Requirements:** `ZBW-QA-002/004/005`, `ZBW-ARC-005/006`, `ZBW-DEPLOY-001`,
`ZBW-OPS-006`, `ZBW-READY-009/017`

`BENCHMARK_BASELINE.md` remains the sole workload and threshold authority. No local unit,
microbenchmark or simulated adapter timing is promoted to a production benchmark.

## Qualification matrix

| Profile | Local evidence | Binding result |
|---|---|---|
| `SMALL` | workload, metrics, thresholds and artifact-report procedure validated | `PENDING_NORMATIVE_ENVIRONMENT` |
| `SHARED_40` | M19 bounded coordination tests and owner-module saturation tests retained | `PENDING_NORMATIVE_ENVIRONMENT` |
| `PROXY_4` | M19/M20 deterministic multi-node, dedupe, fencing and adapter latency tests retained | `PENDING_NORMATIVE_ENVIRONMENT` |

Each binding run requires the accepted host calibration, immutable server/plugin/map/config/provider
hashes, ten-minute warm-up, five independent 30-minute samples and raw time series. It records
TPS, whole-server MSPT, Zartra tick contribution, heap after full GC, retained growth, GC pauses,
thread count, queue/pool high-water marks, SQL/Redis/proxy/replay/PlaceholderAPI timings and JFR
blocking-I/O observations. Every hard threshold must pass in all five runs.

## Blocking-I/O detector

`tools/validation/m24_qualification.py` scans every Paper production source and rejects direct SQL,
Lettuce, HTTP-client, sleep, unaudited filesystem and blocking future primitives. The two permitted
filesystem surfaces are structurally checked:

- `PaperNativeWorldProvider` marks filesystem steps `Affinity.WORKER` and rejects owner-thread
  execution;
- `PrimaryRuntimeCertification` waits and writes evidence only from its dedicated worker thread.

The synchronous compatibility bootstrap join is accepted only while the exact adapter returns an
already-completed stage without I/O. Any change to that invariant fails the validator.

Static qualification prevents known regressions but does not replace JFR/agent verification.
Runtime benchmark rows therefore remain pending and contain no fabricated `observed` metrics.

## Execution record

For each profile archive:

1. commit and artifact checksum report;
2. hardware/OS/JDK/JVM flags and calibration;
3. fixture, dataset, map, configuration and provider hashes;
4. raw TPS/MSPT/JFR/GC/heap/thread/queue/network/SQL/Redis series;
5. p50/p95/p99/max, confidence interval and regression comparison;
6. failure/saturation observations and corrective-action owner.

A threshold failure, blocking I/O call or regression of at least 10% blocks release.
