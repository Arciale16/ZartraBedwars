# Dependency and Licence Audit

**Status:** exact M04 build/test dependency, CI action and external database image selections are checksum/licence/provenance locked; product bundling remains disabled
**Decisions:** RC-021, RC-024, RC-027, RC-076
**Requirements:** `ZBW-READY-005..008`, `ZBW-LICENSE-001..007`
**Audit date:** 2026-07-14

## Binding acquisition rule

The exact versions below are the only approved selections. M01 acquired 14 Maven, Temurin, Python and SHA-pinned GitHub Actions artifacts. M04 adds the immutable `actions/upload-artifact` action used solely to retain certified external-database evidence, bringing the current build/CI lock to 15 entries in `build/dependency-lock.json`. Before an artifact enters a local/CI dependency cache, the acquisition gate downloads it from the authoritative repository, records SHA-256 or immutable Git commit plus exact licence-text hash in the generated lock/SBOM, and compares both. A missing/mismatched artifact is rejected rather than substituted.

Dynamic versions, version ranges, `LATEST`, `RELEASE` and mutable `SNAPSHOT` release coordinates are prohibited. When an official API is only published as a snapshot, the gate resolves one unique timestamped artifact tied to the recorded upstream tag/commit, verifies its checksum, mirrors it privately under `io.zartra.thirdparty-lock:<name>:<upstream-version>-<short-commit>`, and never redistributes the upstream binary. This is an immutable coordinate, not a floating snapshot.

`COMPILE-ONLY` and `RUNTIME-ONLY` artifacts are absent from Zartra release JARs. `BUNDLED` is allowed only where the recorded licence permits commercial redistribution and notices/source obligations are generated. Proprietary products are always `OPERATOR-RUNTIME`; their binary is never acquired by public CI, committed, cached as a fixture or redistributed.

## Required record fields and rights interpretation

Every row in this file is a complete dependency record containing name, exact version, authoritative source, licence, redistribution rights, shading permission, modification permission, required attribution, runtime-only/bundled classification and commercial-use compatibility. The condensed table cells below apply these binding interpretations:

- `BUILD-ONLY`/`TEST-ONLY`/`BENCHMARK-ONLY`: redistribution in the product **NO**; shading into the product **NO**; modification **NO** unless a row explicitly approves it; attribution appears in build SBOM/tooling notices; no runtime/bundled product status; commercial project use is permitted only after the exact licence-text acquisition check.
- `COMPILE-ONLY`/`PLATFORM-PROVIDED`/`PROVIDED`: redistribution in the product **NO**; shading **NO**; modification **NO**; exact API copyright/licence appears in development SBOM; runtime is supplied separately by platform/operator; commercial interoperability is permitted only when the recorded licence/vendor terms and acquisition gate confirm it.
- `OPERATOR-RUNTIME`/`SEPARATE-RUNTIME`: Zartra redistribution **NO**; shading **NO**; modification **NO**; operator/vendor owns installation and notices; commercial use requires the operator's licence/entitlement.
- `BUNDLED`: redistribution, shading/relocation, modification and commercial use are permitted only as explicitly stated in that row's “Rights and obligations” cell; required copyright/licence/notice/source offer is generated into `THIRD_PARTY_NOTICES.md`. Absence of an explicit permission means **NO**.
- `BUNDLED/relocated`: redistribution **YES**, shading **YES**, unmodified relocation **YES** and source modification **NO** unless the row expressly says otherwise; attribution/licence notice is mandatory; commercial compatibility remains subject to the exact acquired licence.

The generated immutable lock expands these fields into separate machine-readable columns for every artifact and transitive dependency. An empty field, `UNKNOWN`, conflicting licence or missing permission is a hard rejection.

## Build, runtime and bundled-library selections

| Name / coordinates | Exact version | Authoritative source | Licence | Rights and obligations | Packaging / commercial decision |
|---|---|---|---|---|---|
| Apache Maven distribution + original Zartra launcher | `3.9.11` | Apache Maven distribution | Apache-2.0 | Archive SHA-256/SHA-512 and embedded LICENSE/NOTICE hashes locked; not product-distributed | BUILD-ONLY, acquired and verified in M01 |
| Eclipse Temurin compile JDKs | `8u442-b06`, `11.0.26+4`, `16.0.2+7`, `17.0.14+7`, `21.0.6+7` | Adoptium release API/GitHub binaries | GPL-2.0 with Classpath Exception | Linux and Windows x64 archive and licence/assembly-exception hashes locked; never product-bundled | BUILD-ONLY, acquired and verified in M01 |
| CPython validator runtime | `3.12.13` | CPython/actions python-versions release | Python-2.0 | Ubuntu 22.04 x64 asset digest and CPython licence hash locked | BUILD-ONLY, acquired and verified in M01 |
| `actions/upload-artifact` | immutable commit `ea165f8d65b6e75b540449e92b4886f43607fa02` | Official GitHub Actions repository | MIT | Commit identity and exact licence hash locked; least-privilege evidence upload only | CI-ONLY, acquired and verified in M04 |
| `actions/checkout`, `actions/setup-python` | immutable commits `34e1148…`, `a26af69…` | Official GitHub Actions repositories | MIT | Commit identity and exact licence hash locked; least-privilege CI only | CI-ONLY, acquired and verified in M01 |
| Maven Compiler Plugin | `3.14.1` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
| Maven Resources Plugin | `3.3.1` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
| Maven JAR Plugin | `3.4.1` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
| Maven Surefire/Failsafe | `3.5.4` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
| Maven Enforcer Plugin | `3.6.1` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
| Maven Shade Plugin | `3.6.1` | Apache Maven Central/source | Apache-2.0 | Relocation report and notices required | BUILD-ONLY |
| Jackson BOM/databind/core/annotations | `2.17.2` | FasterXML Maven Central/source | Apache-2.0 | Redistribution/modification allowed with licence/notice | BUNDLED/relocated; commercial compatible |
| SnakeYAML Engine | `2.9` | SnakeYAML Maven Central/source | Apache-2.0 | Redistribution/modification allowed with licence/notice | BUNDLED/relocated |
| Flyway Core | `10.20.1` | Redgate/Flyway Maven Central/source | Apache-2.0 community core | Bundle only community-core modules; retain notices; no Teams code | BUNDLED/relocated |
| HikariCP | `4.0.3` | Brett Wooldridge Maven Central/source | Apache-2.0 | Redistribution/modification allowed with notice | BUNDLED/relocated; Java 8-compatible baseline |
| Caffeine | `2.9.3` | Ben Manes Maven Central/source | Apache-2.0 | Redistribution/modification allowed with notice | BUNDLED/relocated; Java 8-compatible baseline |
| Lettuce Core | `6.3.2.RELEASE` | Redis/Lettuce Maven Central/source | Apache-2.0 | Include licence/notice and Netty/reactive transitives in SBOM | BUNDLED/relocated after transitive audit |
| Micrometer Core | `1.12.5` | Micrometer Maven Central/source | Apache-2.0 | Include licence/notice and transitive audit | BUNDLED/relocated |
| zstd-jni | `1.5.6-9` | Luben Maven Central/source | BSD-2-Clause | Preserve copyright/licence; native classifiers audited per OS | BUNDLED; commercial compatible |
| SQLite JDBC | `3.46.0.0` | Xerial Maven Central/source | Apache-2.0 | Preserve licence/notice; native classifiers audited | BUNDLED only in shared-server distribution |
| MariaDB Connector/J | `3.4.1` | MariaDB Maven Central/source | LGPL-2.1-or-later | Unmodified separate JAR preferred; licence/source offer and relinkability preserved | RUNTIME-LIB distribution only after legal gate; supports network DB profile |
| MySQL Connector/J | `8.4.0` | Oracle Maven Central/source | GPL-2.0 with Universal FOSS Exception | Do not shade/modify; separate optional driver; retain licence/exception/source notices | OPERATOR/SEPARATE-RUNTIME unless owner legal approves distribution |
| Adventure BOM/MiniMessage | `4.17.0` | Kyori Maven Central/source | MIT | Preserve copyright/licence | PLATFORM-PROVIDED modern; bundled Java-8 bridge only after duplicate-class check |

Manual constructor injection and project-owned command/GUI DSLs are selected; no DI, command or GUI framework dependency is permitted without a new ADR.

### M04 acquired SQL graph

M04 resolved the approved coordinates and the union of their JDK-specific transitive graphs into an isolated acquisition repository, inherited every transitive POM licence, normalized the MySQL Universal FOSS Exception declaration, hashed every JAR/POM and generated `build/maven-dependency-lock.json`, `build/maven-build-sbom.cdx.json` and `build/M04_MAVEN_BUILD_NOTICES.md`. The exact graph is 192 binary components and 604 files. The six-component/13-file increase over the initial M04 graph records the Surefire JUnit Platform provider and its exact 1.11.4 project/1.12.1 provider boundaries required to execute tests from a clean offline cache; it is not production scope. Every component is `RUNTIME_OR_BUILD_NOT_BUNDLED`; redistribution, shading and modification rights are disabled in this milestone's thin artifacts. The union includes HikariCP's pinned Java 8 `slf4j-api:1.7.30` branch so every approved JDK can build offline from the same lock.

HikariCP/Caffeine are compile dependencies; Flyway and the three JDBC drivers are runtime dependencies; JUnit/Testcontainers are test dependencies. Runtime scope does not authorize release bundling. SQLite may be packaged only by a later approved shared-server distribution gate; MariaDB remains a separate runtime library after its LGPL obligations gate; MySQL remains operator-provided/separate-runtime unless legal approval is recorded. No database driver is shaded.

M06 expands the exact build repository to 212 binary components and 650 JAR/POM files. The increase is the Maven Shade Plugin 3.6.1 build-only graph needed to assemble the internal Paper plugin modules plus the compile-only Paper API mirror; no external component is authorized for product bundling. The JDOM 2.0.6.1 transitive licence is recorded as `LicenseRef-JDOM` against the immutable `JDOM-2.0.6.1` release licence text and SHA-256 evidence, rather than treating its non-standard POM label as Apache-2.0.

Testcontainers image acquisition is separately default-denied. `build/m04-database-container-lock.json` is the machine-readable authority and `tools/ci/m04_database_images.py` verifies its schema, exact version, OCI index and Linux/amd64 manifest digests, configuration digest, immutable packaging-source commit, source-manifest hash and exact GPL-2.0-only licence-text hash before either image is pulled. CI then pulls only the approved `tag@sha256:index-digest` reference and verifies the resolved local platform digest. No image is stored, bundled or redistributed by this repository.

| CI-only database image | Exact reference | Linux/amd64 manifest | Immutable packaging source | Licence / rights |
|---|---|---|---|---|
| MySQL Community Server `8.4.0` | `mysql:8.4.0@sha256:dab7049abafe3a0e12cbe5e49050cf149881c0cd9665c289e5808b9dad39c9e0` | `sha256:3e5649c69e6d75cf88fc6f8f39f877453faa4e5167b5e648007e45f54bb17f6b` | `docker-library/mysql@c857c9c091e6194c9fb9c91e83d343b186e103cd` | GPL-2.0-only packaging; CI test runtime only; no redistribution, shading or modification |
| MariaDB Server `11.4.2` | `mariadb:11.4.2@sha256:e59ba8783bf7bc02a4779f103bb0d8751ac0e10f9471089709608377eded7aa8` | `sha256:fdc72e8f2960d758aa77ebac9ea65028ca195d4cba854a14e4afae703f5f6a22` | `MariaDB/mariadb-docker@43ea329ad4639e43c6b8662cbaa460d4fb22d9fc` | GPL-2.0-only packaging; CI test runtime only; no redistribution, shading or modification |

## Platform and integration selections

| Provider/API | Exact certified baseline | Source/licence fact | Scope and redistribution decision | Absence/incompatibility behavior |
|---|---|---|---|---|
| Paper API/runtime | M06 API `1.21.1-R0.1-20250328.161643-128`, mirror `io.zartra.mirror.paper:paper-api:1.21.1-build133`, SHA-512 `0474a57eade57b959e84e2c1b7955f7c995893de70f73558061c4d10e33a29c10a3e4a2aae328b48ed4f5483a6a51d0ef079f5dfeff6a64d32cb879b53f0899e`; runtime Paper `1.21.1-133`, commit `3cb8529bdbeeec9ff51df2c94e93dbe7e41bb811`, SHA-256 `39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9` | PaperMC official repository/Fill service; GPL-3.0-only plus upstream components; provenance locked in `build/m06-paper-runtime-lock.json` | API PROVIDED/compile-only and not shaded; server ephemeral certification input, never committed/bundled/redistributed | Missing/mismatched hash fails before resolution/start; only exact Paper 1.21.1 build 133 is M06-certified |
| Legacy Spigot-compatible API/runtime | private BuildTools exact revs `1.8.8`, `1.9.4`, `1.10.2`, `1.11.2` | Official Spigot BuildTools/licence process | COMPILE-ONLY/private test runtime; generated hash lock; never redistribute server/API binary | Legacy row unsupported until private fixture lock passes |
| Velocity API/runtime | API `3.5.0-SNAPSHOT` resolved to one upstream commit and privately immutable-mirrored | PaperMC GitHub; GPL-3.0 | COMPILE-ONLY; operator installs hash-locked runtime | Bungee alternative or proxy feature unavailable; game backend unaffected |
| BungeeCord API/runtime | `1.21-R0.3` | SpigotMC repository/source; BSD-style project licence | COMPILE-ONLY/provided; no runtime bundling | Velocity alternative or clear proxy-unavailable result |
| CloudNet API/runtime | `4.0.0-RC14` | CloudNetService source/repository; Apache-2.0 | COMPILE-ONLY/provided | Static proxy registry remains; no automatic scaling |
| PlaceholderAPI | `2.12.2` | Official PlaceholderAPI repository; GPL-3.0 | COMPILE-ONLY/provided | Placeholders disabled; gameplay/API continue |
| VaultAPI | `1.7.1` | MilkBowl/VaultAPI; LGPL-3.0 | COMPILE-ONLY/provided | Native economy/permission paths or configured unavailable response |
| LuckPerms API | `5.4` | LuckPerms official repository; MIT | COMPILE-ONLY/provided | Native permission adapter remains |
| ProtocolLib | `5.4.0` | Official repository; GPL-2.0 | COMPILE-ONLY/provided | Internal narrow compatibility adapter where lawful/safe, otherwise capability disabled only if decorative |
| WorldEdit API | `7.3.16` | EngineHub repository; GPL-3.0 | COMPILE-ONLY/provided | Built-in WorldProvider remains |
| FastAsyncWorldEdit API | `2.15.1` | IntellectualSites repository; GPL-3.0 | COMPILE-ONLY/provided | WorldEdit/built-in provider remains |
| WorldGuard API | `7.0.17` | EngineHub repository; GPL-3.0 | COMPILE-ONLY/provided | Built-in region policy remains |
| AdvancedSlimePaper provider contract | `5.1.0` | Official project repository; exact licence rechecked at acquisition | COMPILE-ONLY private immutable API mirror or runtime-reflective SPI | Built-in snapshot/world provider remains |
| Multiverse-Core API | `5.3.3` | Multiverse official repository; BSD-3-Clause | COMPILE-ONLY/provided | Built-in world registry remains |
| Citizens API | `2.0.39-SNAPSHOT` fixed to certified upstream commit/private mirror | CitizensDev official repository; compatible open-source terms rechecked | COMPILE-ONLY immutable private mirror; no runtime bundling | Packet/built-in NPC provider remains |
| ZNPCsPlus API | `2.0.0-SNAPSHOT` fixed to certified upstream commit/private mirror | Pyrbu official repository; GPL-3.0 | COMPILE-ONLY immutable private mirror | Packet/built-in NPC provider remains |
| DecentHolograms API | `2.9.6` | DecentSoftware official repository; GPL-3.0 | COMPILE-ONLY/provided | Built-in packet/text hologram provider remains |
| Parties API | `3.2.17` | AlessioDP official repository; MIT | COMPILE-ONLY/provided | Native Zartra party authority remains; provider switch is explicit migration |
| Grim API/runtime | `2.3.72` certified provider baseline | Grim official repository; GPL-3.0 | COMPILE-ONLY/provided; operator runtime | Normalized no-anticheat telemetry; gameplay/replay continue |
| Vulcan runtime/API | Vendor contract adapter `v1`; certified target runtime `2.9.0` only after operator entitlement verification | Proprietary vendor terms; no assumed redistribution/modification right | OPERATOR-RUNTIME; no binary/API copied or stored; reflective/event adapter only if vendor terms permit | Normalized no-anticheat telemetry; no fabricated alerts |
| ViaVersion | `5.4.2` | ViaVersion official repository; GPL-3.0 | COMPILE-ONLY/provided | Native clients only |
| ViaBackwards | `5.4.2` | ViaVersion official repository; GPL-3.0 | COMPILE-ONLY/provided | Native clients only |
| ViaRewind | `4.0.6` | ViaVersion official repository; GPL-3.0 | COMPILE-ONLY/provided | Native clients only |
| Geyser API/runtime | `2.7.0` certified baseline | GeyserMC official repository; MIT | COMPILE-ONLY/provided | Java clients continue; Bedrock unavailable |
| Floodgate API/runtime | `2.2.4` certified baseline | GeyserMC official repository; MIT | COMPILE-ONLY/provided | No Bedrock identity mapping; reject translated join safely |
| DiscordSRV | NOT SELECTED BY DESIGN | External product not required by provider architecture | No dependency. Custom provider may integrate under separate audit. | Embedded webhook/external-bot providers remain |

Provider versions are certification baselines, not automatic compatibility ranges. If a coordinate above is unavailable or its exact licence differs at immutable acquisition, the gate blocks that adapter and the neutral SPI/safe fallback remains; it never substitutes another version or weakens the feature.

## Test, benchmark and quality selections

| Tool | Exact version | Licence | Scope / obligations |
|---|---|---|---|
| JUnit Jupiter | `5.11.4` | EPL-2.0 | TEST-ONLY; notices in build SBOM |
| AssertJ Core | `3.27.3` | Apache-2.0 | TEST-ONLY |
| Mockito Core | `4.11.0` | MIT | TEST-ONLY; Java 8-compatible baseline |
| Testcontainers | `1.20.4` | MIT | TEST-ONLY; container image licences separately pinned/audited |
| ArchUnit | `1.3.0` | Apache-2.0 | TEST-ONLY |
| JMH | `1.37` | GPL-2.0 with Classpath Exception | BENCHMARK-ONLY; never product runtime |
| JaCoCo Maven Plugin | `0.8.13` | EPL-2.0 | BUILD/TEST-ONLY |
| Checkstyle Maven Plugin / Checkstyle | `3.6.0` / `10.21.2` | Apache-2.0 / LGPL-2.1 | BUILD-ONLY; no product bundling |
| SpotBugs Maven Plugin / SpotBugs | `4.9.2.0` / `4.9.2` | Apache-2.0 / LGPL-2.1 | BUILD-ONLY; no product bundling |
| OWASP Dependency-Check Maven | `12.1.0` | Apache-2.0 | BUILD-ONLY; NVD data terms recorded; no product bundling |

## Approval and release gates

1. Generate the immutable lock with coordinate/source URL, upstream tag/commit, SHA-256, PGP/signature where available, embedded licence hash, scope and transitives.
2. Reject any resolved artifact or transitive absent from this selection baseline and generated lock.
3. Produce CycloneDX SBOM; scan exact binaries; enforce `QUALITY_GATES.md` vulnerability policy.
4. Scan release JARs for provided/proprietary/server classes and licence files. Any match blocks release.
5. Generate `THIRD_PARTY_NOTICES.md` from actual bundled selections and preserve source-offer/notice obligations.
6. Re-audit on every version, source, classifier, patch, relocation or packaging change.

M01 materializes these gates in `tools/dependencies/lock_dependencies.py`. Its deterministic outputs are `build/dependency-lock.json`, `build/sbom.cdx.json` and `build/THIRD_PARTY_BUILD_NOTICES.md`; the empty reactor runs offline and rejects direct plug-in goals, so BOM/plugin-version declarations cannot trigger unverified resolution.

M02 activates only the approved JUnit and Maven build/quality selections needed by its five Java modules. `tools/dependencies/maven_lock.py` captures the complete resolved graph as 170 binary components and 535 exact JAR/POM files in `build/maven-dependency-lock.json`, with Maven Central source URLs, SHA-256, size, inherited or immutable-source licence evidence, SPDX expressions and explicit build/test-only rights. `build/maven-build-sbom.cdx.json` and `build/M02_MAVEN_BUILD_NOTICES.md` are generated from that lock. Normal builds restore/seed the exact repository and run offline; no Maven component is shaded, modified, redistributed or bundled in a ZartraBedWars product JAR.

One legacy SpotBugs-reporting transitive, `org.codehaus.plexus:plexus-i18n:1.0-beta-10`, has no effective licence element in its published POM chain. Its exact official release-tag commit `d5aaf49970fc3e95408f4f9cd1b856fa72be130f` contains an Apache-2.0 source header; the lock records the immutable source URL and hash plus the canonical Apache-2.0 text hash. It remains build-only and is never redistributed. Legacy `Public Domain` and `Plexus` POM declarations are retained verbatim with declaration evidence and no product rights.

The architecture/library/version choices for RC-021/024/027 are resolved. Artifact checksum and exact licence-text acquisition is an automated pre-resolution fact check; until it passes, that artifact cannot be downloaded by the build. It is not an open design decision and never permits a floating replacement.

M03 adds no external production, test or build dependency. `zbw-config` compiles only against the internal `zbw-api` and `zbw-application` artifacts and reuses the exact M02 Maven lock, SBOM and notices. The M03 dependency gate rejects graph drift before any online resolution; no dependency is shaded, bundled, modified or redistributed by an M03 artifact.

## M21 Phase 2 adapter dependency result

The eight isolated provider adapter modules add no Maven component. They compile against internal
neutral SPIs and accept operator-supplied gateways, so the exact M02 Maven lock, build SBOM and
notices remain unchanged. Vault, LuckPerms, Citizens, ZNPCsPlus, DecentHolograms, AlessioDP
Parties, Grim and Vulcan runtime artifacts remain operator-side acquisition gates under the exact
selection rows above; no proprietary, snapshot or server binary is stored, resolved, shaded or
redistributed by this reactor. (ZBW-INT-002/003/006/007/008/009; ZBW-LICENSE-005)

## M21 Phase 3 CloudNet dependency result

`zbw-cloudnet` adds no Maven component. It compiles against internal neutral contracts and accepts an operator-supplied `CloudNetGateway`, so the immutable Maven lock, build SBOM and notices remain unchanged. CloudNet `4.0.0-RC14` remains an operator-side provided runtime selection under the approved row above; no CloudNet, proprietary or server binary is stored, resolved, shaded or redistributed by this reactor. Standalone fallback uses the static M20 backend registry with autoscaling disabled. (`ZBW-DEPLOY-005`, `ZBW-ADDON-226..235`, `ZBW-LICENSE-005`)

## M21 Phase 5 world-provider dependency result

`zbw-integration-world-providers` adds no Maven component. It compiles only against internal `zbw-api` and `zbw-world` contracts and accepts operator-supplied gateways. WorldEdit `7.3.16`, FAWE `2.15.1`, WorldGuard `7.0.17`, AdvancedSlimePaper/SlimeWorldManager `5.1.0` and Multiverse-Core `5.3.3` remain provided operator runtime selections under the approved rows above. No vendor/server binary is stored, resolved, shaded or redistributed; the immutable Maven lock, build SBOM and notices remain unchanged. Native M06 fallback remains available. (`ZBW-INT-005`, `ZBW-LICENSE-005`)
## M22 Phase 1 provider acquisition gate

`build/m22-provider-lock-requirements.json` records the approved exact provider baselines ProtocolLib `5.4.0`, ViaVersion/ViaBackwards `5.4.2`, ViaRewind `4.0.6`, Geyser `2.7.0` and Floodgate `2.2.4`, their provided-only scope, SPDX licence and official-project provenance. Artifact and exact licence-text SHA-256 fields remain deliberately unset with state `REQUIRED_BEFORE_RESOLUTION`; therefore Maven declaration, acquisition, cache seeding, packaging and support certification are all blocked. Phase 2 may resolve an artifact only after exact coordinates/source, both digests and the complete transitive graph are captured by the existing immutable Maven lock/SBOM process. (`ZBW-INT-004`, `ZBW-INT-010`, `ZBW-READY-005`, `ZBW-LICENSE-001/002/005`)
