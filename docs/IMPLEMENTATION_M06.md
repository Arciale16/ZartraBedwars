# Milestone 6 implementation evidence

## Scope

M06 completes `ZBW-ARC-002` and `ZBW-ARC-007` and delivers only the assigned
foundations of `ZBW-ARENA-005/006`, `ZBW-INT-004/005/010`,
`ZBW-COMPAT-001..009`, `ZBW-CONTENT-009` and
`ZBW-READY-001/002/003/006`. It does not implement arena CRUD, gameplay,
optional world providers, translated-client support, a legacy adapter or M07.

| Module | Boundary | Responsibility |
|---|---|---|
| `zbw-compat-api` | Java 8 / class-major 52 | Semantic keys, typed compatibility outcomes, adapter/validation SPI and last-known-good registry |
| `zbw-world` | Java 8 / class-major 52 | Typed world operations/provider plans and bounded asynchronous orchestration |
| `zbw-compat-v1_20-v1_21` | Java 21 / class-major 65 | Primary Paper 1.21.1 build 133 mapping set and compatibility adapter |
| `zbw-paper-modern` | Java 21 / class-major 65 | Primary Paper bootstrap, owner dispatcher and native world provider |

The neutral modules import no Bukkit, Paper, NMS, storage, Redis or proxy API.
The modern adapter and bootstrap are activated only in the JDK-21 Maven
profile. `zbw-compat-v1_8` and all other M22 artifacts remain absent.

## Delivered behavior

- Semantic capability keys cover materials, items, metadata, sounds,
  particles, text, entities, packet/UI and scheduler behavior. Outcomes are
  explicitly `SUPPORTED`, `UNSUPPORTED`, `FALLBACK` or `DEGRADED`.
- The immutable mapping registry rejects duplicate/malformed mappings,
  validates mandatory keys and replaces its last-known-good snapshot only
  after complete validation.
- The primary adapter exposes 19 original semantic mappings and identifies
  only Paper 1.21.1 build 133 with its exact server SHA-256.
- `WorldOperation` provides load, clone, reset and unload with typed IDs,
  correlation, source/target validation and one total deadline.
- `WorldOrchestrator` has bounded admission, one active lease per target,
  M05 worker scheduling, owner dispatch, cancellation, timeout, drain,
  accounting, health and reverse-order compensation. Admission leases are
  released before public completion, preventing same-world chain races.
- `PaperNativeWorldProvider` performs directory/copy/replace/delete work on
  workers; Paper load/unload and resource inspection run only on the primary
  thread. Clone omits `uid.dat` and `session.lock`; failed reset restores the
  original backup. Snapshots expose loaded chunks, entities and retained
  handles without returning a platform object.
- The bootstrap validates worker, queue, in-flight, tracked-world and timeout
  bounds; starts once; rejects work after stop; and drains workers off the
  Paper owner thread.

## Verification evidence

The JDK-21 reactor executes 138 tests with zero failures, errors or skips.
The Java 8/11/16/17 neutral matrix executes 121 tests per JDK; the Java 21 row
also compiles and tests both modern artifacts. Contract coverage includes all
four capability outcomes, duplicate/invalid/LKG mapping behavior, load/clone/
reset/unload, native failure restoration, thread guards, bounded admission,
cancellation, timeout, drain, rollback failure, leak accounting and the
same-world completion-chain regression.

| Module | Line coverage | Branch coverage | Required | Checkstyle | SpotBugs |
|---|---:|---:|---|---|---|
| `zbw-compat-api` | 95.14% | 90.91% | 90% / 85% | 0 | 0 |
| `zbw-world` | 95.39% | 85.62% | 90% / 85% | 0 | 0 |
| `zbw-compat-v1_20-v1_21` | 98.46% | 90.00% | 80% / 70% | 0 | 0 |
| `zbw-paper-modern` unit-testable adapter scope | 89.89% | 87.34% | 80% / 70% | 0 | 0 |

Bootstrap/runtime-only classes are explicitly excluded from the unit JaCoCo
denominator and are instead mandatory in the exact Paper E2E workflow; this is
not a coverage waiver. `tools/validation/m06_paper_e2e.py` started the
checksum-locked Paper 1.21.1 build 133 server and passed five ordered
operations: LOAD template, UNLOAD template, CLONE, RESET and UNLOAD clone.
The run recorded off-owner filesystem enforcement, owner-only world mutation,
leak-free final unload, completed worker shutdown and process exit 0 in
`build/evidence/m06-paper-primary.json`.

Binary baselines contain 296 Java-8-neutral and 7 Java-21-modern public
classes and preserve every M02-M05 signature. Paper API
`1.21.1-R0.1-20250328.161643-128` is checksum locked, `provided`, unshaded and
absent from the plugin artifact. The exact M06 Maven lock contains 212
build/runtime components and 650 JAR/POM files, with zero product-bundled
external components. Dependency/licence/SBOM/notices, strict JavaDoc,
architecture, governance and documentation coverage validators pass.

## Generated artifacts

- Four Maven module JARs and the assembled `zbw-paper-modern` plugin JAR.
- `build/api-signature-baseline-m06.txt` and
  `build/api-signature-baseline-m06-modern.txt`.
- `target/zartrabedwars-m06-neutral-javadoc.zip` and
  `target/zartrabedwars-m06-modern-javadoc.zip`.
- Surefire, JaCoCo, Checkstyle and SpotBugs reports.
- `build/evidence/m06-paper-primary.json` and CI-uploaded Paper console/runtime
  evidence.

## Deferred boundary

Full 1.8–1.21.x server/runtime certification, `zbw-compat-v1_8`, other
legacy/intermediate adapters, complete 1.20/1.21-family claims, Via/Geyser/
Floodgate, optional world providers and all feature-level fallback rendering
remain in M21/M22. M06 certification cannot be used as evidence for them.
