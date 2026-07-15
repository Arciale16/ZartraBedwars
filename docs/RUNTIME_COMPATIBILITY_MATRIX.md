# Runtime and Compatibility Matrix

**Status:** Accepted pre-code baseline
**Decisions:** RC-003, RC-004, RC-022
**Requirements:** `ZBW-READY-001`, `ZBW-READY-002`, `ZBW-READY-006`, `ZBW-COMPAT-001..009`

The distribution spans **Java 8**, Java 11, Java 16, Java 17 and **Java 21** toolchains; no Java 21-only bytecode may enter a Java 8 artifact.

## Artifact and toolchain contract

| Artifact | Bytecode | Compile JDK | Runtime JDK | Platform boundary |
|---|---:|---|---|---|
| `zbw-api`, `zbw-domain`, protocol/schema contracts | 8 | Eclipse Temurin `8u442-b06` | 8+ | No Bukkit/Paper/NMS/proxy/provider imports |
| `zbw-paper-legacy` + `compat-v1_8..v1_11` | 8 | Temurin `8u442-b06` | Temurin `8u442-b06` | Spigot-compatible public API; isolated packet/material adapters |
| `zbw-paper-j11` + `compat-v1_12..v1_16_4` | 11 | Temurin `11.0.26+4` | Temurin `11.0.26+4` | Hash-locked Paper public API/runtime |
| `zbw-paper-j16` + `compat-v1_16_5` | 16 | Temurin `16.0.2+7` | Temurin `16.0.2+7` | Hash-locked Paper public API/runtime |
| `zbw-paper-j17` + `compat-v1_17..v1_19` | 17 | Temurin `17.0.14+7` | Temurin `17.0.14+7` | Hash-locked Paper public API/runtime |
| `zbw-paper-modern` + `compat-v1_20..v1_21` | 21 | Temurin `21.0.6+7` | Temurin `21.0.6+7` | Hash-locked Paper public API/runtime |
| `zbw-bungeecord` | 8 | Temurin `8u442-b06` | 8+ | Public BungeeCord proxy API only |
| `zbw-velocity` | 21 | Temurin `21.0.6+7` | 21+ | Public Velocity API only |

Maven Wrapper is fixed to Maven `3.9.11`; compiler uses `--release`, Enforcer rejects the wrong JDK, and CI inspects every class-file major version. JDK patch changes require a dependency/toolchain audit but do not change supported Minecraft rows.

## Milestone certification allocation

This is milestone ownership, not current certification evidence. M06 has not started, so every fixture remains `NOT_STARTED` in `build/private-runtime-fixtures.json` until its assigned suite actually passes.

| Runtime row | M06 certification ownership | M22 certification ownership | Current status |
|---|---|---|---|
| Paper 1.21.1 build 133, SHA-256 `39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9` | **Only M06 row:** primary bootstrap, owner-thread dispatch, native world provider and M06 semantic mappings | Full release-level lifecycle/gameplay/GUI/item/packet/replay/provider revalidation | `NOT_STARTED` |
| Every other declared row from 1.8.8 through 1.21.11 | None; M06 must not claim it | Complete row certification, including legacy fallbacks and translated-client/Bedrock dimensions where applicable | `NOT_STARTED` pending M22 |

The shared `zbw-compat-v1_20-v1_21` artifact name does not certify the complete 1.20/1.21 family. Full 1.8–1.21.x support remains an M22 release gate, and M22 revalidates the M06 primary row after all feature semantics exist.

## Mandatory server-runtime fixtures

The “fixture” is the exact certified server used for compatibility. Paper build IDs and SHA-256 values below were verified against the official PaperMC Fill API on 2026-07-14. The legacy Spigot-compatible rows are produced privately with the official BuildTools workflow for the exact `--rev`; their generated SHA-256 is added to the private CI fixture lock before execution and the binary is never committed or redistributed.

| Server version | Distribution/fixture | Required JDK | Adapter family | Certification role |
|---|---|---:|---|---|
| 1.8.8 | private BuildTools `--rev 1.8.8` | 8 | `compat-v1_8` | Mandatory lower bound and full fallback suite |
| 1.9.4 | private BuildTools `--rev 1.9.4` | 8 | `compat-v1_9` | Terminal 1.9 row |
| 1.10.2 | private BuildTools `--rev 1.10.2` | 8 | `compat-v1_10` | Terminal 1.10 row |
| 1.11.2 | private BuildTools `--rev 1.11.2` | 8 | `compat-v1_11` | Terminal 1.11 row |
| 1.12.2 | Paper build `1620`, SHA-256 `3a2041807f492dcdc34ebb324a287414946e3e05ec3df6fd03f5b5f7d9afc210` | 11 | `compat-v1_12` | Terminal 1.12 row |
| 1.13.2 | Paper `657`, `11e828d0565ab76a0a0e180c056364a95de44958cfd6a6af3f9b1dc70b03e9cd` | 11 | `compat-v1_13` | Terminal 1.13 row |
| 1.14.4 | Paper `245`, `bd8ec5cdb22370d37816a6de26798df3d2b0d6f9c7c96c88ca45a1303fea50e8` | 11 | `compat-v1_14` | Terminal 1.14 row |
| 1.15.2 | Paper `393`, `bd2dd6f2cc489cf9e2bb800cb4fb6d63e9d293945d3ac10b09dd9c6098fa9f34` | 11 | `compat-v1_15` | Terminal 1.15 row |
| 1.16.5 | Paper `794`, `e67da4851d08cde378ab2b89be58849238c303351ed2482181a99c2c2b489276` | 16 | `compat-v1_16` | Terminal 1.16 row |
| 1.17.1 | Paper `411`, `6cc1ee2f94253ce10b5374ed85fffc735a97d8f1b64db293683dfa24dd3cc05f` | 17 | `compat-v1_17` | Terminal 1.17 row |
| 1.18.2 | Paper `388`, `0578f18f4d632b494b468ec56b3b414b5b56fea087ee7d39cf6dcdf4c9d01f05` | 17 | `compat-v1_18` | Terminal 1.18 row |
| 1.19.4 | Paper `550`, `e587d78cba3e99ef8c4bc24cf20cc3bdbbe89e33b0b572070446af4eb6be5ccf` | 17 | `compat-v1_19` | Terminal 1.19 row |
| 1.20.1 | Paper `196`, `234a9b32098100c6fc116664d64e36ccdb58b5b649af0f80bcccb08b0255eaea` | 21 | `compat-v1_20` | Master Prompt primary 1.20 family entry |
| 1.20.2 | Paper `318`, `ba340a835ac40b8563aa7eda1cd6479a11a7623409c89a2c35cd9d7490ed17a7` | 21 | `compat-v1_20` | Data-component boundary regression |
| 1.20.4 | Paper `499`, `cabed3ae77cf55deba7c7d8722bc9cfd5e991201c211665f9265616d9fe5c77b` | 21 | `compat-v1_20` | Protocol regression |
| 1.20.6 | Paper `151`, `4b011f5adb5f6c72007686a223174fce82f31aeb4b34faf4652abc840b47e640` | 21 | `compat-v1_20` | Terminal 1.20 row |
| 1.21.1 | Paper `133`, `39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9` | 21 | `compat-v1_21` | Product primary baseline; sole M06 foundation-certification row, then M22 full revalidation |
| 1.21.3 | Paper `83`, `87e973e1d338e869e7fdbc4b8fadc1579d7bb0246a0e0cf6e5700ace6c8bc17e` | 21 | `compat-v1_21` | Protocol regression |
| 1.21.4 | Paper `232`, `5ee4f542f628a14c644410b08c94ea42e772ef4d29fe92973636b6813d4eaffc` | 21 | `compat-v1_21` | Protocol regression |
| 1.21.8 | Paper `60`, `8de7c52c3b02403503d16fac58003f1efef7dd7a0256786843927fa92ee57f1e` | 21 | `compat-v1_21` | Terminal Summer Drop row |
| 1.21.10 | Paper `130`, `158703f75a26f842ea656b3dc6d75bf3d1ec176b97a2c36384d0b80b3871af53` | 21 | `compat-v1_21` | Protocol regression |
| 1.21.11 | Paper `132`, `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba` | 21 | `compat-v1_21` | Mandatory upper bound at baseline date |

Support means the listed terminal/stable patch for every Minecraft minor family. An earlier patch is not advertised merely because bytecode loads; it is added only as an explicit fixture row. Later 1.21.x patches require a new hash-locked row and full certification without weakening the existing rows.

## Client protocol compatibility

| Server family | Native Java clients | Translated Java clients | Bedrock | Acceptance |
|---|---|---|---|---|
| 1.8.8 | 1.8.8 | 1.9–1.21.11 through audited ViaVersion/ViaBackwards/ViaRewind runtime | Through audited Geyser/Floodgate runtime | Join, hotbar, shop, combat, respawn, spectator, reconnect and every critical GUI/input path |
| 1.12.2 | 1.12.2 | 1.8.8–1.21.11 through audited translator | Same | Same plus legacy material/text fallback |
| 1.16.5 | 1.16.5 | 1.8.8–1.21.11 where translator declares support | Same | Same plus RGB/legacy downgrade |
| 1.19.4 | 1.19.4 | Translator-declared 1.8.8–1.21.11 | Same | Signed-chat-safe messaging and inventory paths |
| 1.20.6 | 1.20.6 | Translator-declared 1.8.8–1.21.11 | Same | Data-component and GUI compatibility |
| 1.21.1 / 1.21.11 | matching native client | Translator-declared 1.8.8–1.21.11 | Same | Full primary and upper-bound suites |

Via/Geyser absence is safe: native clients continue, translated clients receive a clear proxy rejection, and no gameplay rule changes. Translator support claims are capped by the exact runtime versions audited in `DEPENDENCY_LICENSE_AUDIT.md`.

## Mandatory certification suite

For every server row: boot/shutdown/reload rejection, arena setup/import/clone/reset, lobby/queue/team assignment, all modes, shop/generator/items/upgrades, bed destruction/elimination/respawn/spectator, rejoin/play-again, cosmetics fallback, commands/permissions/GUI, PAPI safe absence, SQL migrations, replay capture/playback compatibility and fault recovery. The 1.8.8 and 1.21.11 rows additionally run every `COMPATIBILITY_FALLBACKS.md` fixture.

Failure of one row changes that release candidate to unsupported; it never silently drops a feature or redefines “1.8–1.21.x.”
