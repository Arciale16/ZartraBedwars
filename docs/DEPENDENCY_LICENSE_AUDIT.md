# Dependency and Licence Audit

**Status:** exact selection baseline accepted; immutable checksum acquisition is mandatory before first use
**Decisions:** RC-021, RC-024, RC-027, RC-076
**Requirements:** `ZBW-READY-005..008`, `ZBW-LICENSE-001..007`
**Audit date:** 2026-07-14

## Binding acquisition rule

No dependency is currently resolved because no Java build exists. The exact versions below are the only approved selections. Before an artifact enters a local/CI dependency cache, the acquisition gate downloads it from the authoritative repository, records SHA-256 and embedded licence text in the generated lock/SBOM, and compares both on every build. A missing/mismatched artifact is rejected rather than substituted.

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
| Apache Maven Wrapper | `3.9.11` | Apache Maven distribution | Apache-2.0 | Redistribution permitted with licence/notice; no modification planned | BUILD-ONLY, approved selection |
| Maven Compiler Plugin | `3.14.1` | Apache Maven Central/source | Apache-2.0 | Notice in build SBOM | BUILD-ONLY |
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

## Platform and integration selections

| Provider/API | Exact certified baseline | Source/licence fact | Scope and redistribution decision | Absence/incompatibility behavior |
|---|---|---|---|---|
| Paper API/runtime | API source commit matching Paper `1.21.1-133` primary and `1.21.11-132` upper fixture | PaperMC GitHub; GPL-3.0 and upstream components | COMPILE-ONLY immutable private mirror; runtime hash in runtime matrix; no server redistribution | Matching adapter row required; clear startup incompatibility |
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

The architecture/library/version choices for RC-021/024/027 are resolved. Artifact checksum and exact licence-text acquisition is an automated pre-resolution fact check; until it passes, that artifact cannot be downloaded by the build. It is not an open design decision and never permits a floating replacement.
