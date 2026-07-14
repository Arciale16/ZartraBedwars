# Milestone 2 Implementation — Public API, Domain Primitives and Extension Metadata

**Milestone:** M02<br>
**Implementation date:** 2026-07-14<br>
**Foundation:** approved M01 baseline at `8ee45f16c010860495aaeb206782971fea85606b`<br>
**Runtime/gameplay implementation:** not started

## Implemented allocation

M02 completes its direct allocation of `ZBW-ARC-003`, `ZBW-ARC-004`, `ZBW-ARC-009`, `ZBW-ARC-010`, `ZBW-ECO-002`, `ZBW-ECO-003`, `ZBW-CONTENT-010`, `ZBW-DISCORD-001`, `ZBW-DISCORD-002` and `ZBW-DISCORD-005`.

It also supplies only the explicitly assigned foundations of `ZBW-ADDON-464..473`, `ZBW-CONTENT-001..011`, `ZBW-DISCORD-001..008`, `ZBW-READY-003`, `ZBW-READY-004`, `ZBW-READY-005`, `ZBW-READY-007` and `ZBW-READY-008`. Their content production, configuration, security engine, provider adapters and runtime behavior remain in their owning later milestones.

## Materialized modules

| Module | M02 responsibility |
|---|---|
| `api/zbw-api` | typed IDs, semantic API versions, typed results/errors, clocks, events, capabilities, provider/extension/content and anticheat contracts |
| `domain/zbw-domain` | generator/resource profile and private-game RESOURCE SCARCITY domain contracts |
| `application/zbw-application` | deterministic content-registry assembly and capability evaluation |
| `sdk/zbw-sdk` | restricted extension metadata reader and deterministic validator |
| `integrations/discord/zbw-integration-discord-api` | provider-neutral secure Discord API/SPI contracts |

All compile to Java 8 bytecode and are free of platform, storage and runtime-configuration dependencies. No empty future module was created.

## Verification assets

- six extension metadata fixtures: two valid and four invalid;
- `build/api-signature-baseline.txt` for 107 public JVM classes, including public nested contract types;
- `build/maven-dependency-lock.json` for 170 exact Maven build/test components and 535 JAR/POM files;
- `build/maven-build-sbom.cdx.json` and `build/M02_MAVEN_BUILD_NOTICES.md`;
- `tools/validation/m02_architecture.py`, `api_compatibility.py`, `api_docs.py` and `run_m02_validation.py`;
- M02 CI governance and exact five-JDK matrix workflows.

## Exit evidence

| Gate | Result |
|---|---|
| Clean Java 21 reactor | PASS — 8/8 projects, 24 tests, 5 JARs |
| Checkstyle | PASS — zero violations |
| SpotBugs | PASS — zero findings/errors |
| Domain coverage | PASS — 92.31% line / 100% branch; minimum 90% / 85% enforced by JaCoCo |
| Application coverage | PASS — 93.10% line / 90% branch; minimum 90% / 85% enforced by JaCoCo |
| Binary/API compatibility | PASS — 107 public classes, class-major 52, exact committed baseline |
| Architecture | PASS — five bounded modules; no platform/storage/filesystem imports in core |
| Dependency/licence | PASS — 14 M01 build/CI artifacts plus 170 Maven components; zero product-bundled dependency |
| JavaDoc | PASS — 71 production source files; strict Java 8 generation |
| Java 8/11/16/17/21 matrix | PASS — five clean 8-project builds; 24/24 tests on every JDK |
| Documentation/governance | PASS — 13 governance tests; 49/473 addon, 6,438 assertion/672 requirement and 25-decision gates |

## Deliberately excluded

M02 contains no Bukkit/Paper/proxy adapter, runtime event bus, extension discovery/class loading, configuration parser, permission/command/GUI implementation, content migration engine, generator scheduler, Private Games host behavior, Discord provider implementation, secret handling, retry/outbox engine, gameplay, arena, shop, replay, Atlas, progression or cosmetics. Those exclusions preserve the milestone plan and do not reduce their requirements.

Milestone 3 was not started.
