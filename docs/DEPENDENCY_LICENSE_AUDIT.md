# Dependency and Licence Audit

**Status:** complete current-state pre-implementation audit; default deny<br>
**Decision:** RC-076<br>
**Requirements:** `ZBW-LICENSE-001..007`<br>
**Audit date:** 2026-07-14<br>
**Selected Java dependency set:** empty — no build or Java implementation exists

## 1. Binding decision

An external artifact is approved only after its exact name, coordinates/file, immutable version, source, checksum and licence text have been reviewed in this document. Project-level or moving-branch licence information does not authorize an arbitrary artifact version. Changing the version, source, classifier, shading, patches or packaging invalidates approval and requires a new row/review.

`UNSELECTED` is an explicit blocking version value, not a wildcard. A row with status other than `APPROVED` cannot enter a POM, lockfile, repository, test fixture, shaded JAR, Docker image or release artifact. This is the technical refinement needed while RC-021/024/027 still govern exact dependency/version selection.

No dependency may be bundled or redistributed unless the exact licence/contract explicitly grants it. Official external-plugin APIs are preferred as compile-only/provided dependencies; proprietary plugin binaries are never stored or redistributed by ZartraBedWars.

## 2. Required record schema

Every selected dependency record must contain:

- name and immutable version;
- authoritative source and checksum;
- exact licence/contract version;
- redistribution rights;
- shading permission;
- modification permission;
- required attribution/notices/source-offer obligations;
- runtime-only/provided or bundled classification;
- commercial-use compatibility;
- reviewer, review date and final `APPROVED`/`REJECTED` decision.

## 3. Complete candidate inventory

The following table covers every dependency/integration family currently named or necessarily implied by the PRD/architecture. None is selected or authorized yet. `NO` in a rights column means “not authorized by this audit,” even if a future exact-version review may approve it.

| Name | Version | Authoritative source | Licence | Redistribution rights | Shading permission | Modification permission | Required attribution | Runtime-only or bundled | Commercial-use compatibility | Audit decision |
|---|---|---|---|---|---|---|---|---|---|---|
| Paper API/runtime | `1.21.1 target; exact artifact UNSELECTED` | [PaperMC Paper](https://github.com/PaperMC/Paper) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided runtime candidate | UNDETERMINED | BLOCKED — RC-021 |
| Legacy 1.8 server/API distribution | `UNSELECTED` | Distribution must be chosen under RC-022 | Not selected | NO | NO | NO | Pending | Compile-only/provided runtime candidate | UNDETERMINED | BLOCKED — RC-003/022 |
| Velocity API/runtime | `UNSELECTED` | [PaperMC Velocity](https://github.com/PaperMC/Velocity) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided runtime candidate | UNDETERMINED | BLOCKED — RC-021 |
| BungeeCord API/runtime | `UNSELECTED` | [SpigotMC BungeeCord](https://github.com/SpigotMC/BungeeCord) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided runtime candidate | UNDETERMINED | BLOCKED — RC-021 |
| CloudNet API/runtime | `UNSELECTED` | [CloudNetService CloudNet](https://github.com/CloudNetService/CloudNet-v3) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| PlaceholderAPI API/runtime | `UNSELECTED` | [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| VaultAPI/runtime | `UNSELECTED` | [MilkBowl VaultAPI](https://github.com/MilkBowl/VaultAPI) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| LuckPerms API/runtime | `UNSELECTED` | [LuckPerms](https://github.com/LuckPerms/LuckPerms) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| ProtocolLib API/runtime | `UNSELECTED` | [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| WorldEdit API/runtime | `UNSELECTED` | [EngineHub WorldEdit](https://github.com/EngineHub/WorldEdit) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| FastAsyncWorldEdit API/runtime | `UNSELECTED` | [IntellectualSites FAWE](https://github.com/IntellectualSites/FastAsyncWorldEdit) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| WorldGuard API/runtime | `UNSELECTED` | [EngineHub WorldGuard](https://github.com/EngineHub/WorldGuard) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| Slime world provider API/runtime | `UNSELECTED` | Maintained implementation/coordinates unresolved under RC-023 | Not selected | NO | NO | NO | Pending | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021/023 |
| Multiverse-Core API/runtime | `UNSELECTED` | [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| Citizens API/runtime | `UNSELECTED` | [Citizens2](https://github.com/CitizensDev/Citizens2) | Exact artifact/vendor terms not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| ZNPCsPlus API/runtime | `UNSELECTED` | [ZNPCsPlus](https://github.com/Pyrbu/ZNPCsPlus) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| DecentHolograms API/runtime | `UNSELECTED` | [DecentHolograms](https://github.com/DecentSoftware-eu/DecentHolograms) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| AlessioDP Parties API/runtime | `UNSELECTED` | [Parties](https://github.com/AlessioDP/Parties) | Exact artifact/vendor terms not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021/028 |
| Grim API/runtime | `UNSELECTED` | [GrimAC](https://github.com/GrimAnticheat/Grim) | Exact artifact/API terms not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-024 |
| Vulcan API/runtime | `UNSELECTED` | Official vendor distribution/API required | Proprietary/contractual terms unapproved | NO | NO | NO | Vendor terms pending | Operator-installed runtime only | UNDETERMINED | BLOCKED — RC-024; never repository-bundled |
| ViaVersion API/runtime | `UNSELECTED` | [ViaVersion](https://github.com/ViaVersion/ViaVersion) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| Geyser API/runtime | `UNSELECTED` | [Geyser](https://github.com/GeyserMC/Geyser) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| Floodgate API/runtime | `UNSELECTED` | [Floodgate](https://github.com/GeyserMC/Floodgate) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021 |
| DiscordSRV API/runtime | `UNSELECTED` | [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Compile-only/provided optional runtime | UNDETERMINED | BLOCKED — RC-021/074 |
| SQLite JDBC driver | `UNSELECTED` | Coordinates/project to be selected | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-021/027 |
| MySQL Connector/J | `UNSELECTED` | [MySQL Connector/J](https://github.com/mysql/mysql-connector-j) | Exact artifact/FOSS-exception terms not approved | NO | NO | NO | Pending exact-version review | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-021 |
| MariaDB Connector/J | `UNSELECTED` | [MariaDB Connector/J](https://github.com/mariadb-corporation/mariadb-connector-j) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-021 |
| HikariCP | `UNSELECTED` | [HikariCP](https://github.com/brettwooldridge/HikariCP) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-021 |
| Redis client | `UNSELECTED` | Jedis/Lettuce/other selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Configuration/serialization library | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Database migration library | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Cache library | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Metrics library/exporter | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Compression/codec library | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Potential bundled runtime library | UNDETERMINED | BLOCKED — RC-027 |
| Adventure/MiniMessage API | `UNSELECTED` | [Kyori Adventure](https://github.com/KyoriPowered/adventure) | Exact artifact licence not approved | NO | NO | NO | Pending exact-version review | Prefer platform-provided/compile-only | UNDETERMINED | BLOCKED — RC-021/027 |
| Discord HTTP/IPC client | `UNSELECTED` | Selection required by external/webhook provider | Not selected | NO | NO | NO | Pending | External bot or isolated bundled adapter candidate | UNDETERMINED | BLOCKED — RC-026/027 |
| Maven core/plugins | `UNSELECTED` | [Apache Maven](https://github.com/apache/maven) and selected plugin sources | Exact tool/plugin licences not approved | NO redistribution in product | NO | NO | Pending per plugin | Build-time only | UNDETERMINED | BLOCKED — RC-021/027 |
| JUnit/test assertion stack | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Test-only | UNDETERMINED | BLOCKED — RC-027 |
| Container/integration test libraries | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Test-only | UNDETERMINED | BLOCKED — RC-027 |
| Paper/proxy test harness | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Test-only | UNDETERMINED | BLOCKED — RC-027 |
| JMH/benchmark stack | `UNSELECTED` | Selection required | Not selected | NO | NO | NO | Pending | Test/benchmark only | UNDETERMINED | BLOCKED — RC-027 |
| Checkstyle/SpotBugs/JaCoCo/security scanners | `UNSELECTED` | Each tool/plugin source required | Not selected | NO redistribution in product | NO | NO | Pending per tool/plugin | Build-time only | UNDETERMINED | BLOCKED — RC-027/062 |

## 4. Approval states

- `BLOCKED`: candidate is known but exact artifact/legal facts are incomplete; it cannot be used.
- `REJECTED`: reviewed and incompatible with project policy; it cannot be used.
- `APPROVED-COMPILE-ONLY`: exact API artifact may be used to compile but is not bundled.
- `APPROVED-RUNTIME-ONLY`: integration expects an operator-installed dependency; no binary is redistributed.
- `APPROVED-BUNDLED`: exact artifact may be included/shaded under recorded obligations.

Only the last three are usable. Approval records the checksum, licence file copy, notices, source-offer or attribution obligations and reviewer.

## 5. Automated gates

M01 must generate an SBOM and compare build resolution against approved name/version/checksum rows. CI fails on dynamic/range/SNAPSHOT versions for release, undeclared transitive dependencies, licence-file drift, prohibited/unknown licences, an unapproved shaded package, missing notice/source obligations or proprietary binaries. M24 repeats the audit on the reproducible release artifact.

`THIRD_PARTY_NOTICES.md` is generated from approved bundled/runtime rows. At this baseline it correctly declares that no third-party dependency is bundled because the selected dependency set is empty.

## 6. Resolution and remaining gate

RC-076 is resolved: redistribution, shading, modification, attribution, commercial-use and proprietary-binary policy are explicit, default-deny and traceable. This resolution does **not** approve unselected artifacts. RC-021, RC-024 and RC-027 remain blocking until exact versions are chosen and their rows become approved before any Java implementation begins.
