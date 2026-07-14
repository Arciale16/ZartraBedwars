# Milestone 1 Implementation — Project Foundation

**Milestone:** M01 — Materialize the accepted build governance baseline<br>
**Implementation date:** 2026-07-14<br>
**Baseline commit:** `7b80ad5aec3310033e46eddaa4f4f617571cd683`<br>
**Baseline comparison:** no file difference from approved pre-code commit `28870e1`<br>
**Functional Java implementation:** not started; repository contains zero `.java` files

## Scope implemented

M01 implements the build-governance slice of these 37 requirements:

- `ZBW-GOV-003..011`
- `ZBW-ARC-001`, `ZBW-ARC-002`, `ZBW-ARC-008`
- `ZBW-OPS-008`
- `ZBW-QA-001`, `ZBW-QA-005`, `ZBW-QA-007`
- `ZBW-CONTENT-001`, `ZBW-CONTENT-011`
- `ZBW-COMPAT-001`, `ZBW-COMPAT-009`
- `ZBW-LICENSE-001..007`
- `ZBW-READY-001`, `ZBW-READY-002`, `ZBW-READY-005`, `ZBW-READY-006`, `ZBW-READY-008`, `ZBW-READY-009`, `ZBW-READY-012`, `ZBW-READY-017`, `ZBW-READY-019`, `ZBW-READY-020`

Requirements that continue into later milestones are not marked product-complete here. M01 supplies only their explicitly allocated build, policy, manifest and verification foundations.

M01 deliberately does not create public API/service contracts (M02), runtime configuration/localization/authorization (M03), storage (M04), operational logging/error/scheduler services (M05), compatibility adapters (M06/M22), or any gameplay/arena/shop/replay/Atlas/progression/cosmetic implementation. No empty feature module is present.

## Materialized build

The Maven 3.9.11 reactor contains exactly three POM artifacts:

1. `zartrabedwars-parent` — reactor and approved plug-in version governance.
2. `zbw-bom` — exact versions approved by the dependency baseline. A BOM declaration is not permission to resolve an artifact.
3. `zbw-build-tools` — build-only governance descriptor.

The complete future production graph is a validated, non-materialized plan in `build/module-graph.json`. Its Java 8/11/16/17/21 boundaries, layer dependencies and first owning milestones are checked without creating fake modules or support claims.

The original project wrapper verifies Python, Java vendor/version, Maven archive SHA-256/SHA-512 and an offline/default-deny repository before invoking Maven. Direct plug-in goals and Maven `clean` are rejected because no clean plug-in was approved. `tools/build/clean_build.py` safely removes only repository-owned `target` directories and then runs the offline reactor.

## Toolchains and dependency governance

`build/dependency-lock.json` contains 14 verified build/CI artifacts:

- Maven 3.9.11;
- Eclipse Temurin 8u442-b06, 11.0.26+4, 16.0.2+7, 17.0.14+7 and 21.0.6+7 for Linux x64;
- the same five exact Temurin versions for Windows x64;
- CPython 3.12.13 for Ubuntu 22.04 CI;
- immutable commits of `actions/checkout` and `actions/setup-python`.

Every row records exact source, integrity, licence text hash, scope, product redistribution, shading, modification, attribution and commercial-use decisions. The generator at `tools/dependencies/lock_dependencies.py` performs archive/git/licence acquisition checks and deterministically emits:

- `build/dependency-lock.json`;
- `build/sbom.cdx.json` (CycloneDX 1.6);
- `build/THIRD_PARTY_BUILD_NOTICES.md`.

The Maven reactor resolves no plug-in or Java library. All selected dependency versions in the BOM and parent remain unavailable to a build until a verified lock row exists. No third-party or server binary is committed or included in a product artifact.

## Validation and CI

`tools/validation/run_m01_validation.py` is the single deterministic local governance entry point. It runs:

- nine Python standard-library unit/structural tests;
- module graph, cycle, layer, class-major and zero-Java checks;
- exact runtime fixture inventory and no-certification-claim checks;
- asset provenance and prohibited binary scans;
- quality/static-analysis configuration checks;
- immutable CI action and toolchain matrix checks;
- dependency lock, licence, rights, SBOM and notices checks;
- all three existing documentation/coverage validators.

CI is split into two least-privilege, SHA-pinned workflows:

- `m01-governance.yml` runs tests, static governance, dependency/licence/provenance and all documentation validators on Python 3.12.13.
- `m01-toolchain-matrix.yml` downloads each locked Linux Temurin archive, verifies its checksum and runs the empty clean reactor on Java 8, 11, 16, 17 and 21.

## Local verification evidence

| Gate | Result |
|---|---|
| Java 8 / Maven 3.9.11 clean empty reactor | PASS — 3/3 reactor projects; 0.132 s |
| Java 11 / Maven 3.9.11 clean empty reactor | PASS — 3/3; 0.110 s |
| Java 16 / Maven 3.9.11 clean empty reactor | PASS — 3/3; 0.112 s |
| Java 17 / Maven 3.9.11 clean empty reactor | PASS — 3/3; 0.101 s |
| Java 21 / Maven 3.9.11 clean empty reactor | PASS — 3/3; 0.143 s |
| Governance unit tests | PASS — 9/9 |
| Foundation/static gate | PASS |
| Dependency/licence/SBOM gate | PASS — 14/14 build/CI artifacts; zero product binaries |
| Addon catalogue | PASS — 49 addons, 473 requirements, 100% |
| Master Prompt coverage | PASS — 6,438 assertions, 672 requirements, 9 annexes, 100% |
| Pre-code decisions | PASS — 25 decisions, 55 decision IDs, 199 Part I IDs, 473 addon IDs |

## Entry and exit criteria

| Criterion | Evidence | Status |
|---|---|---|
| M00 PRE-CODE READY approved | main at `7b80ad5`; byte-identical documentation delta versus `28870e1`; three baseline validators pass | SATISFIED |
| Exact selections and architecture not reopened | ADR-0001..0016 unchanged; manifests implement accepted choices | SATISFIED |
| Clean empty multi-module build on all compile JDKs | five local verified builds and CI matrix | SATISFIED |
| Documentation/licence/provenance/dependency-lock gates pass | deterministic M1 runner evidence above | SATISFIED |
| Every resolved build artifact has exact checksum/licence evidence | 14 verified lock rows; zero resolved Maven plug-ins/libraries | SATISFIED |
| Runtime/provider matrix immutable | versioned toolchain/module/22-fixture manifests; drift tests | SATISFIED |
| No functional production path claimed | zero `.java`; zero feature modules; every fixture certification `NOT_STARTED` | SATISFIED |

## Remaining execution evidence

No unresolved decision blocks or belongs to M01. The following remain intentionally owned by later milestones and are not support claims:

- privately generated legacy server fixture SHA-256 values and all server boot/game certification belong to M22;
- Java/plugin dependencies are acquired only when their owning milestone introduces a real use and supplies the complete transitive lock;
- actual Checkstyle, SpotBugs, coverage and mutation reports begin when Java modules exist; M01 verifies their approved versions, configuration and thresholds without resolving them;
- public/commercial legal terms and product release qualification remain M24 evidence.

Milestone 2 has not started.
