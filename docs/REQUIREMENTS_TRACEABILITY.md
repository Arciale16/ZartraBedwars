# ZartraBedWars Requirements Traceability Matrix

## Rules and legend

This baseline contains 672 stable semantic requirement IDs. Part I contains exactly one row for each of the 199 PRD IDs (144 original core IDs plus 55 accepted owner-decision IDs). Part II is the normative atomic matrix in `docs/MASTER_PROMPT_COVERAGE.md`, containing one `MP-L####` row for every non-empty source assertion in `MASTER_PROMPT.md`. Part III is `docs/ADDON_FEATURE_CATALOG.md`, containing one complete mapping row for each of the 473 owner-supplied native-addon requirements. Locations are planned until implementation; replace plans with exact package/class/table/key/file/test links as each milestone starts. A dash means the surface has no direct user-facing behavior and must include a reason; it never omits documentation or verification.

- **Cmd/Perm:** command family and canonical permission family; exact subcommands/nodes are frozen before the implementation milestone.
- **API/Evt:** public API service/provider and event family.
- **PH:** PlaceholderAPI family; `health` means sanitized public/operator status only.
- **Tests:** required suites in addition to the PRD impact profile.
- All rows inherit the global config/documentation/quality rules in ZBW-OPS-001/009 and ZBW-QA-001/006/007.

## Part I â€” Requirement-level matrix

## Governance (11)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-GOV-001 | PRD/change-control checks; docs only | document metadata | â€” | â€” | requirement metadata schema | â€” | docs lint | PRD governance; M00/M01 |
| ZBW-GOV-002 | scope-change approval policy | â€” | â€” | â€” | â€” | â€” | traceability diff check | contribution/governance; M00 |
| ZBW-GOV-003 | `build/module-graph.json`; PRD/trace registries; `tools/validation/foundation.py` dependency/orphan checks | versioned JSON governance manifests | M1 validator / maintainer | â€” (build governance) | machine-readable module/requirement manifests; runtime API remains M02+ | â€” | graph and coverage-ID tests | `docs/IMPLEMENTATION_M01.md`; M00/M01 |
| ZBW-GOV-004 | accepted ADR repository plus decision-document validator | â€” | pre-code decision validator / maintainer | â€” | ADR metadata in Markdown | â€” | ADR/document completeness gate | `docs/DECISIONS`; M00/M01 |
| ZBW-GOV-005 | `build/module-graph.json`, `build/toolchains.json`, `tools/validation/foundation.py` | machine-readable layer/bytecode policy | M1 validator / maintainer | â€” | â€” (public architecture API is later) | â€” | cycle, forbidden-layer, reactor and class-major checks | architecture + implementation guide; M01 |
| ZBW-GOV-006 | `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`, M1 static source policy | `build/quality-policy.json` | M1 validator / maintainer | â€” | â€” | â€” | static config/forbidden-marker/bytecode tests; Java analyzers activate with Java modules | quality gates + implementation guide; M01 |
| ZBW-GOV-007 | deterministic M1 evidence runner; final cross-feature compliance remains M24 | `build/milestone-state.json`, quality/release policy | M1 validator / maintainer | â€” in M01 | generated lock/SBOM/report schemas; runtime API M24 | â€” in M01 | M1 exit-gate suite; final 672-ID compliance M24 | implementation guide; M01/M24 |
| ZBW-GOV-008 | two pinned CI workflows and clean build orchestrator | `.github/workflows/*.yml` | `python tools/build/clean_build.py --jdk <id>` | â€” | â€” | â€” | five-JDK reactor matrix + deterministic governance job | milestones + implementation guide; all |
| ZBW-GOV-009 | `lock_dependencies.py`, asset provenance scan, generated SBOM/notices | dependency acquisition and asset manifests | dependency `validate` / maintainer | â€” (reports are files in M01) | CycloneDX/lock schemas | â€” | hash/licence/rights/binary/asset drift gates | dependency audit, asset provenance, notices; M01/M24 |
| ZBW-GOV-010 | PRD change procedure, `AGENTS.md`, exact M1 scope guard | â€” | M1 validator / maintainer | â€” | â€” | â€” | source/trace diff and milestone-scope review | PRD governance + implementation guide; all |
| ZBW-GOV-011 | three coverage tools orchestrated by `run_m01_validation.py`; Java work controlled by milestone state | `.python-version` 3.12.13; coverage metadata | M1 validator / maintainer | â€” (generated reports) | coverage report contracts; runtime API later | coverage counts in reports | 6,438 Master + 473 addon + 55 decision assertions | Master coverage + implementation guide; M00/M01/M24 |

## Architecture foundations (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ARC-001 | M1 parent/BOM/governance plus M02 five-module API/domain/application/SDK/Discord reactor and machine graph | Maven/module manifests | deterministic validators / maintainer | runtime health later | M02 provider/extension lifecycle contracts | â€” | exact reactor/dependency/cycle/forbidden-import checks | architecture + M01/M02 implementation guides |
| ZBW-ARC-002 | VERIFIED M06 Java-8 `zbw-compat-api`/`zbw-world` and Java-21 primary adapter/bootstrap boundaries in `build/module-graph.json`; legacy/intermediate artifacts remain M22 | toolchain/module manifests | graph validator / maintainer | â€” | semantic compatibility/world contracts | â€” | exact class-major 52/65, forbidden-layer, graph/order and API CT pass | runtime/compatibility guides; M06 VERIFIED / M22 completion |
| ZBW-ARC-003 | M02 `zbw-api`, `zbw-sdk`, five Java-8 artifacts and `api-signature-baseline.txt` | semantic API/version-range policy in contracts; runtime config later | developer tooling later | API browser later | `VersionedApi`, `ApiVersions`, immutable public contracts | api version later | binary descriptor/class-major/unit contracts | `API_M02.md`, generated JavaDoc, ADR-0017; M02 VERIFIED |
| ZBW-ARC-004 | M02 immutable event identity/metadata/cancellation/listener primitives; runtime bus/queues later | queue limits remain M05 | event debug later | event diagnostics later | `EventMetadata`, `ApiEvent`, thread context and schema/order fields | safe metrics later | event equality/order/schema/thread/cancel/null tests | `API_M02.md`; M02 contracts VERIFIED / M05 runtime |
| ZBW-ARC-005 | M05 `SchedulerPort`, owner-dispatch contract and `StrictThreadGuard`; no Minecraft adapter claimed | worker/queue/default deadline keys | command surface remains M09/M24 | dashboard remains M24 | typed task/context/handle/outcome/dispatcher/guard API | scheduler accounting + bounded metric contracts | illegal owner/worker calls, named worker, pre/post deadline and five-JDK architecture tests VERIFIED | `API_M05.md`, `SCHEDULER_M05.md`; M05 VERIFIED |
| ZBW-ARC-006 | M05 fixed workers, `ArrayBlockingQueue`, explicit rejection, cooperative cancel, lifecycle drain/force and loss accounting | scheduler/lifecycle hard limits | admin surface remains M09/M24 | queue dashboard remains M24 | scheduler snapshot + lifecycle component/report API | accepted/completed/failed/rejected/cancelled/queue state | saturation, queued/running cancel, graceful/forced shutdown, failure isolation and coverage gates VERIFIED | `SCHEDULER_M05.md`, `LIFECYCLE_M05.md`; M05 VERIFIED |
| ZBW-ARC-007 | M02 generic provider contracts plus VERIFIED M06 compatibility registry and native primary world-provider composition; optional registries/adapters later | bounded primary provider settings; optional selection later | runtime status/reload later | integration manager later | `Provider`, compatibility/world SPI and typed health | typed capability/world accounting | M02 metadata plus M06 LKG/provider/failure/E2E suites pass | `API_M02.md`, `API_M06.md`; M06 VERIFIED / optional adapters later |
| ZBW-ARC-008 | Versioned M1 manifests plus M04 contiguous SQL schema plan/history, SHA-256 drift rejection and Flyway bridge | manifest/database schema versions | database migrate/validate remains M09 command surface | â€” in M04 | `Migration`, `MigrationPlan`, `SchemaMigrator` | schema health later | ordering/checksum/restart/Flyway CT plus certified MySQL/MariaDB migration/checksum contracts | `MIGRATIONS_M04.md`; M01/M04 VERIFIED |
| ZBW-ARC-009 | M02 UUID and namespaced immutable typed-ID families; runtime registry/mapping later | canonical ID grammar | inspect later | inspectors later | 18 typed public IDs | public IDs later | type separation, canonical parse/round-trip/equality/hash/malformed tests | `API_M02.md`; M02 VERIFIED / M07 registry |
| ZBW-ARC-010 | M02 extension metadata/lifecycle/catalogue/point contracts plus restricted reader and deterministic validator | schema-1 metadata keys; runtime extension settings later | validate tool contract; commands later | extension manager later | `Extension`, `ExtensionMetadata`, `ExtensionValidation` | extension health later | six fixtures, duplicate/self/missing/version/unknown/malformed tests | `API_M02.md`, ADR-0017; M02 VERIFIED / M23 runtime |

## Core game and modes (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-GAME-001 | M08 VERIFIED state machine/use cases and closed Paper translation; M08.1 VERIFIED version-fenced `ArenaMatchAssembler`, shared limits and arena-derived immutable team identity/display/color/capacity | arenas/performance plus typed default/timing profiles | M09 join/start/forcestart adapters / play,force; M08 use cases enforce authorization | M09 waiting/admin arena surfaces | M08 Game/Arena APIs; M08.1 assembler/team contracts | M16 arena state/countdown | M08 state/property/Paper E2E; M08.1 2/4/8/12/64-team assembly, assignment, admission, reconnect/reset/recovery matrix PASS; M09 surface E2E | `IMPLEMENTATION_M08.md`, `IMPLEMENTATION_M08_1.md`; M08/M08.1 VERIFIED / M09 presentation |
| ZBW-GAME-002 | M08 VERIFIED elimination/phase policies and Paper translation; M08.1 VERIFIED generic surviving-eligible-team evaluator and typed completion intent; mode-specific replacement remains M10 | modes/events/items | M09 game-event controls / force | M09/M10 spectator/admin presentation | M08 combat/phase events; M08.1 `VictoryEvaluator`/`VictoryEvaluation` | M16 bed/team/time state | M08 core/Paper E2E; M08.1 automatic/retry/override/exactly-once lifecycle PASS; M10 gameplay matrix | `GAME_ENGINE_M08.md`, `API_M08_1.md`; M08/M08.1 VERIFIED / M09 presentation / M10 modes |
| ZBW-GAME-003 | M08 VERIFIED: completion orchestration, atomic port coordination and player-state restore; later reward/stat/replay consumers retain ownership | rewards/restore | M09 end/force adapters | M09/M12 reward/end summary | M08 completion contract/events; M12/M15/M17 consumers | M16 result/rewards | M08 retry/crash/exactly-once/restore E2E PASS; later consumer suites | `SESSION_RECOVERY_M08.md`; M08 core/primary Paper VERIFIED / M09 presentation / M12+ consumers |
| ZBW-GAME-004 | M08.1 VERIFIED foundational representation of standard/custom team layouts and overrideable neutral victory contract; M10 owns mode registration/selection and deferred bindings; all named-mode mechanics, including Swappage, remain M11 | `modes.yml`; arena definitions carry limits/teams | M10 mode select/manage / use,manage | M10 mode selector/editor | M08.1 neutral team/victory contracts; Mode API/events M10 | M16 mode values/statistics | M08.1 layout matrix; M10 framework/deferred-binding E2E; M11 named-mode matrix | `LAYOUT_COMPATIBILITY_M08_1.md`; M08.1 foundation / M10 framework / M11 mode mechanics |
| ZBW-GAME-005 | M10 immutable mode registry, compatibility/configuration/event contracts and deferred provider bindings; M11 shop/generator/upgrade and balance components; M15 per-mode statistics | M10 `modes.yml` schema/selection metadata; M11 feature files | M10 mode validate/manage / use,manage | M10 mode selector/editor | M10 mode extension SPI; later component providers retain ownership | M16 dynamic mode values | M10 isolation/version/extension CT; M11 component E2E; M15/M16 provider tests | mode extension guide; M10 framework / M11 mechanics / M15 statistics / M16 placeholders |
| ZBW-GAME-006 | M08 VERIFIED: lobby services/state/rules plus closed primary Paper player effects, direct feedback, scoreboard/tab/boss-bar/hotbar projections and cleanup | config/messages/gui/npcs/holograms | M09 lobby adapters / use,manage | M09 lobby menus/admin; M10 selectors; M21 NPC/hologram providers | M08 Lobby API/events | M16 lobby/player counts | M08 protection/interaction/projection E2E PASS; M09 surfaces; M21 provider CT; M22 compatibility | `PAPER_PROJECTIONS_M08.md`; M08 core/primary Paper VERIFIED / M09 presentation / M10/M16/M21/M22 completion |
| ZBW-GAME-007 | M10 shared-server matchmaking, selectors, bounded queues and arena reservations over M08 use cases; M20 proxy-wide routing | typed M10 selector/matchmaking policy plus M20 proxy config | M10 join/quick/random/queue / play; M20 network diagnostics | M10 arena/map/mode/layout/team selectors | M10 Matchmaking API/events; M20 routing provider | M16 queue/arena availability | M10 deterministic capacity/reservation/concurrency E2E; M20 distributed routing PT | matchmaking guide; M10 shared server / M20 proxy scale |
| ZBW-GAME-008 | M08 VERIFIED: state-specific hotbar definitions, precedence, action intents and transition policy; `zbw-paper-modern` applies/restores the closed primary hotbar | gui/items | M09 hotbar reload/manage adapters | M09 unified hotbar editor/preview/confirmation | M08 Hotbar API/events | M16 visible hotbar state | M08 transition/leak/primary-Paper E2E PASS; M09 editor/command E2E; M22 compatibility | `IMPLEMENTATION_M08.md`; M08 core/primary Paper VERIFIED / M09 presentation / M16/M22 completion |
| ZBW-GAME-009 | M10 spectator lifecycle/options/navigation/restriction use cases and audit; M17 moderation/evidence staff extensions | typed spectator policy, security/messages | M10 spectator actions; M17 staff/freeze/evidence / granular nodes | M10 spectator panels; M17 staff panels | M10 Spectator API/events; M17 Staff APIs | M16 safe spectator/ping/state values | M10 lifecycle/privacy/cleanup E2E; M17 moderation/evidence ST | spectator guide M10; staff guide M17 |
| ZBW-GAME-010 | M05 verified recovery substrate; M08 VERIFIED match/player recovery, safe routing and primary Paper state restoration | scheduler deadlines/lifecycle budgets; feature recovery config M08 | M09 recover/health/admin.force adapters; M08 use cases enforce authorization | M09 recovery GUI | M08 recovery use cases/events over verified M05 marker/store/step/report contracts | M16 typed recovery state | M05 substrate VERIFIED; M08 ordered/runtime/crash/restore E2E PASS; M09 surface E2E | `SESSION_RECOVERY_M08.md`; M05 foundation + M08 runtime VERIFIED / M09 presentation |

## Arena, map, world and setup (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ARENA-001 | M07 presentation-neutral repositories, policies and use cases for every arena lifecycle action | arenas/maps; M07 binding/migration | M09 command adapters / arena.*; M07 use cases enforce authorization | M09 arena manager | M07 Arena API/events | M16 arena fields | M07 lifecycle/MT; M09 surface E2E | arena guide; M07 core / M09 presentation |
| ZBW-ARENA-002 | M07 immutable map aggregate/rename policy; M08.1 VERIFIED shared team-count/capacity/player limits and typed arena default profiles | maps; typed replaceable arena profile | M09 map edit/rename/inspect / map.* | M09 map manager | M07 Map API; M08.1 `TeamLayoutLimits`/`ArenaDefaultProfile` | M16 map fields | M07 rename UT; M08.1 bounds/profile/layout matrix; M09 surface E2E | `TEAM_CONFIGURATION_M08_1.md`; M07 core / M08.1 hardening / M09 presentation |
| ZBW-ARENA-003 | M07 ID registry, import mapper, collision policy and duplicate identity allocation | maps/migration | M09 map import/duplicate / import,duplicate | M09 import/duplicate flow | M07 identity/migration events | M16 map_id | M07 collision/mapping MT; M09 confirmation E2E | map/migration guide; M07 core / M09 presentation |
| ZBW-ARENA-004 | M07 deep-duplicate plan/use case and independently owned copied definitions | maps/arenas/world | M09 map duplicate / duplicate | M09 duplicate map GUI | M07 duplicate API/event | M16 source/new map IDs | M07 deep-copy/rollback E2E; M09 interaction E2E | duplicate guide; M07 core / M09 presentation |
| ZBW-ARENA-005 | M06 Java-8 world SPI/orchestration plus native Paper 1.21.1 provider; M07 arena lifecycle use cases consume only that port; optional WorldEdit/FAWE/WorldGuard/Slime/Multiverse adapters remain M21 | integrations/world | M09 world command adapters / world.* | M09 world manager | World API/provider events | M16 world/provider health | reusable provider CT; primary E2E M06/M07; optional-provider CT M21 | world provider guide; M06 foundation / M07 core / M09 presentation / M21 completion |
| ZBW-ARENA-006 | M06 bounded native-primary reset pipeline foundation; M07 owns arena cleanup, reset admission and recovery use cases | performance/world | M09 world reset / reset,force | M09 reset progress | Reset API/events | M16 reset state/time | owner-thread/I/O split M06; concurrent arena reset/recovery PT M07; M09 interaction E2E | reset/tuning guide; M06 foundation / M07 core / M09 presentation |
| ZBW-ARENA-007 | M07 presentation-neutral setup session, draft, steps, progress, preview/apply, undo/redo, save/rollback and enable-gating use cases | arenas/gui/messages; M07 domain binding, M09 presentation definitions | M09 setup wizard/save/cancel / setup.*; M07 use cases enforce authorization | M09 setup wizard/editor/confirmation | M07 Setup API/events | M16 completion percent | M07 complete typed-harness workflow E2E; M09 GUI/command E2E | setup guide; M07 core / M09 presentation / M22 compatibility completion |
| ZBW-ARENA-008 | M07 validation/report/block-enable policy; M08.1 VERIFIED exact typed generator prerequisites plus map/arena group, mode, team-size and capacity consistency | typed standard/custom validation profiles | M09 setup/arena validate / validate | M09 validator GUI | M07 Validation API; M08.1 `ArenaValidationProfile` | M16 validation state | M07 validation E2E; M08.1 exact-ID/lookalike/custom-profile/stale/inconsistent tests; M09 surface E2E | `ARENA_VALIDATION_PROFILES_M08_1.md`; M07 core / M08.1 hardening / M09 presentation |
| ZBW-ARENA-009 | M07 presentation-neutral administration use cases, authorization intents, two-phase destructive-operation contracts and audit facts | permissions/gui; M07 action metadata, M09 layouts | M09 all arena/map/world commands / granular nodes; M07 enforces nodes | M09 arena/map/world panels and common confirmations | M07 administration APIs/events | â€” no direct public value | M07 auth/audit/stale-revision ST; M09 surface/auth ST | admin reference; M07 core / M09 presentation |

## Shop, upgrades, generators and items (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-SHOP-001 | M11 VERIFIED: immutable scoped catalog, categories, Quick Buy/favourite/history plus cycle-free SQL recovery | shops | verified M09-bound actions / shop.use | verified M09 parity pages | `ShopCatalog`, `ShopIds`, `ShopUserData` | M16 shop/category state | catalog, recovery, presentation and Paper suites PASS; placeholder CT M16 | `IMPLEMENTATION_M11.md`, `IMPLEMENTATION_M11_1.md`; M11 verified / M16 PH / M22 compatibility |
| ZBW-SHOP-002 | M11 VERIFIED: typed presentation metadata, conditions, configuration and purchase history using the unchanged M09 framework | shops/gui | verified manage actions / shop.manage | verified shop/editor parity bindings | category/item presentation metadata and extension-safe IDs | M16 visible price/state | configuration, GUI parity and Paper suites PASS; PH CT M16 | `API_M11.md`; M11 verified / M16 PH / M22 compatibility |
| ZBW-SHOP-003 | M11 VERIFIED portion: quoted match-resource validation and atomic owner-thread purchase with authorization, limits, cooldowns, rollback and idempotency | shops/items | verified purchase/admin bindings | verified confirmation/history bindings | `PurchaseService` and typed transaction contracts | M16 purchase limits | atomic inventory, race, rollback, malformed and Paper suites PASS; persistent-provider CT M12 | `IMPLEMENTATION_M11_1.md`; M11 match transactions verified / M12 persistent currency |
| ZBW-SHOP-004 | M11 VERIFIED portion: native/custom/multiple match tenders and concrete inventory adapter; M12 retains persistent/virtual ledger and M21 Vault | shops/integrations | verified local diagnostics; provider diagnostics M12/M21 | verified local price/tender views; currency audit M12 | `TenderRegistry` and atomic multi-resource commit boundary | M16 tender values | adapter atomicity/Paper PASS; ledger CT M12; Vault CT M21 | `API_M11.md`; M11 verified / M12/M21 retained / M16 PH |
| ZBW-SHOP-005 | M11 VERIFIED: upgrades, forge, traps, effects, recovery, M09 presentation and primary Paper projection | upgrades | verified upgrade actions / upgrade.* | verified upgrade parity bindings | typed upgrade/forge/effect contracts | M16 levels/traps | unit, lifecycle, recovery, presentation and Paper suites PASS; legacy matrix M22 | `IMPLEMENTATION_M11_1.md`; M11 verified / M16 PH / M22 compatibility |
| ZBW-SHOP-006 | M11 VERIFIED: deterministic generators, custom resources, overrides, split, recovery, presentation and primary Paper projection; M21 retains hologram provider | generators; typed per-arena overrides | verified generator actions / generator.* | verified generator parity bindings; provider manager M21 | typed generator/runtime/delivery contracts; HologramProvider M21 | M16 state/countdown/level | timing, concurrency, recovery, presentation and Paper suites PASS; provider/legacy later | `IMPLEMENTATION_M11_1.md`; M11 verified / M16 PH / M21 providers / M22 compatibility |
| ZBW-SHOP-007 | M11 VERIFIED: utility action lifecycle, authorization, match-resource/inventory atomicity, addon intents, M09 presentation and primary Paper mapping | items/security | verified item actions / item.* | verified item parity bindings | typed item/action/effect contracts | M16 item cooldown/state | action, security, cleanup, presentation and Paper suites PASS; legacy matrix M22 | `IMPLEMENTATION_M11_1.md`; M11 verified / M16 PH / M22 compatibility |

## Progression and rewards (14)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-PROG-001 | projection coordinator + inbox | progression/rewards | progression debug / admin.debug | progression inspector | Progression events/API | projection health | duplicate/fanout IT | progression architecture; M12 |
| ZBW-PROG-002 | XP ledger/rules | rewards/levels | experience add/set/remove / admin.* | XP summary/admin | Experience API/events | XP/progress | formula/farming ST | leveling guide; M12 |
| ZBW-PROG-003 | level state/formulas/history migration | levels/rewards | level/experience admin / level.* | level-up/formula editor | Level API/events | level fields | formula/recalc MT | level guide; M12 |
| ZBW-PROG-004 | M12 persistent/virtual currency ledger, providers, transactions and migration consuming the stable M11 tender SPI | currencies/database | coins/currency admin / currency.* | currency/audit | Currency API/events; M11 TenderProvider binding | M16 currency_<id> | atomic/race/MT/ST M12; M11 provider-contract CT | currency guide; M11 tender contract/M12 ledger/M16 PH/M21 Vault |
| ZBW-PROG-005 | prestige definitions/history migration | prestiges/messages | prestige admin / prestige.* | editor/preview | Prestige API/events | prestige fields | render/version/MT | prestige guide; M12/M22 |
| ZBW-PROG-006 | cosmetic catalog + licensed assets | cosmetics | cosmetics admin / cosmetics.manage | cosmetic/editor | Cosmetic registry/API | catalog/count | 300-count/license/PT | cosmetics guide; M14 |
| ZBW-PROG-007 | ownership/equipment/preset repositories | cosmetics/database | grant/revoke/equip / cosmetics.* | ownership/preset/admin | Ownership API/events | owned/equipped | expiry/auth/MT | cosmetics admin guide; M14 |
| ZBW-PROG-008 | effect runtime/budgets | cosmetics/performance | cosmetics disable/profile / admin | visibility/performance | Effect API/events | visibility setting | packet/entity PT | cosmetics tuning; M14 |
| ZBW-PROG-009 | quest definitions/assignments/history | quests | quest/admin CRUDâ€¦ / quest.* | quest/editor/inspector | Quest API/events | quest values | lifecycle/E2E | quest guide; M13 |
| ZBW-PROG-010 | objective registry/progress repository | quests/achievements/challenges | progress admin / progression.admin | objective editor/debug | Objective SPI/events | objective progress | objective matrix/MT | objective SDK; M13 |
| ZBW-PROG-011 | reward ledger/outbox/failure queue | rewards | rewards grant/retry / rewards.* | summary/editor/queue | Reward API/events | reward summary | idempotency/failure ST | reward guide; M12 |
| ZBW-PROG-012 | achievement definitions/progress/history | achievements | achievement admin / achievement.* | achievement/editor | Achievement API/events | achievement values | shared-objective E2E | achievement guide; M13 |
| ZBW-PROG-013 | challenge/pass/season/claim data + migration | challenges/battlepass | challenge/pass admin / battlepass.* | challenge/pass/editors | Challenge/BattlePass API/events | pass/challenge/season | claim/rollover/MT/ST | battle pass guide; M13 |
| ZBW-PROG-014 | profile/settings/calendar data; privacy deletion | profile/rewards/security | profile/settings/admin repair / profile.* | profile/settings/calendar admin | Profile/Calendar APIs | all profile settings | privacy/calendar/MT | profile/privacy guide; M14 |

### M12 Phase 1 progression-foundation evidence

| Allocation | Implemented Phase 1 portion | Verification / remaining owner |
|---|---|---|
| `ZBW-PROG-001` | typed M08 event input, M04-style inbox idempotency port, projection result/checkpoint and bounded recovery state | projection contract tests; durable projectors and adapters completed |
| `ZBW-PROG-002/003/005` | immutable account, XP, level and prestige definitions/states with typed identity, revision and audit metadata | model/validation tests; deterministic formulas; migration/recalculation persistence |
| `ZBW-PROG-004` | immutable persistent-currency definition/account and append-only ledger entry, explicitly separate from M11 match tenders | model/validation tests; atomic persistence and M11 tender provider binding |
| `ZBW-PROG-011` | immutable reward registration identity/record and repository model | idempotency contract tests; reward planning/delivery/retry/compensation |
| `ZBW-PROG-001/002/003/005/011` â€” M12 Phase 3 | configurable M08/M11 projection adapter; versioned XP/anti-farming policy; level preview/recalculation; atomic prestige intent; generic unlock outputs; atomically claimed offline/retry/expiry/compensation reward engine; M03-authorized application ports | `ProgressionEngineTest`, `ProgressionIntegrationTest`; `IMPLEMENTATION_M12_PHASE3.md`, `API_M12_PHASE3.md`, `REWARD_ENGINE_M12.md` |
| `ZBW-PROG-001..005`, `ZBW-PROG-011` â€” M12 Phase 4 | 17 additive M09 command/GUI parity actions; granular M03 permissions; confirmation-marked mutations; async page bindings; Java 21 owner-thread feedback, inventory and cleanup projection | `M12PresentationCatalogTest`, `M12PresentationBindingsTest`, `M12GuiPagesTest`, `M12PaperProjectionTest`; generated through-M12 inventories; `IMPLEMENTATION_M12_PHASE4.md`, `API_M12_PHASE4.md`, `M12_COMMANDS.md`, `M12_GUI.md`, `M12_ADMIN_GUIDE.md` |
| `ZBW-PROG-001..005`, `ZBW-PROG-011` â€” M12 Phase 5 closure | XP/level/prestige formulas+revisions, currency/ledger safety, retry/failure/reward projection, recovery, admin/cfg/presentation integration and Paper certification evidence | `Progression*` integration suite; `M12StorageValidationTest`; M12 dashboard and exit evidence |
| M12 storage boundary | eight Java-8-neutral repository interfaces accept caller-owned M04 `UnitOfWork` and typed revisions/results | architecture and reflection tests; no JDBC, SQL or migration code in Phase 1 |

### M13 Phase 1 objective/content-foundation evidence

| Allocation | Implemented Phase 1 portion | Verification / remaining owner |
|---|---|---|
| `ZBW-PROG-009`, `ZBW-ADDON-081..091` | typed immutable quest identities, schedules, objective/reward references, claim policy, cooldown and assignment lifecycle snapshots plus neutral repository ports | `M13FoundationTest`; runtime assignment, chains, claims, persistence, admin and M09 presentation remain in later M13 phases |
| `ZBW-PROG-010` | typed objective/event/filter/scope/composition definitions, monotonic bounded progress snapshots and optimistic persistence port | catalogue/reference and idempotency tests; full event projection, composite runtime and durable storage remain in later M13 phases |
| `ZBW-PROG-012` | typed tiered achievement definitions with monotonic targets, points and M12 reward references | tier/validation tests; progress history, notifications and presentation remain in later M13 phases |
| `ZBW-PROG-013` | typed challenge variants and versioned battle-pass season/free/premium tier definitions with validated windows | challenge/season/catalogue tests; claims, rollover, migration, persistence and presentation remain in later M13 phases |
| `ZBW-CONTENT-004..006` | immutable cross-reference catalogue capable of validating objective, quest, achievement, challenge and season definition graphs | starter catalogue activation remains in a later M13 phase; no proprietary content or assets introduced |

M13 Phase 1 deliberately introduces no statistics, PlaceholderAPI, replay, Atlas, distributed
transport, external provider or legacy compatibility implementation. Those owners remain M15 through M22.

### M13 Phase 2 objective-runtime and persistence evidence

| Allocation | Implemented Phase 2 portion | Verification / remaining owner |
|---|---|---|
| `ZBW-PROG-010` | deterministic filtered evaluation, repeatable/non-repeatable accumulation, completion/expiration, version/revision/audit evidence, durable duplicate claim and recovery-safe M04 transaction port | `M13RuntimeTest`, `JdbcM13StateRepositoryTest`; composite catalogue activation/editors and presentation remain later M13 |
| `ZBW-PROG-009`, `ZBW-ADDON-081..091` | daily/weekly/custom-capable assignment runtime, activation, progress linkage, completion, expiration/reset, chain advancement and exactly-once M12 reward intents | quest lifecycle/duplicate/restart tests; reroll/abandon/history/admin/UI/notification surfaces remain later M13 |
| `ZBW-PROG-012` | hidden discovery, one-time monotonic tier unlock, reward references and durable achievement snapshots | achievement runtime and SQL restart tests; M15 statistics remain explicitly excluded |
| `ZBW-PROG-013` | timed challenge activation/completion/expiration; active season XP, tier and free-claim state; durable challenge/season snapshots | challenge/pass/claim/SQLite recovery tests; premium purchase, rollover and presentation remain later M13 |
| `ZBW-CONTENT-004..006` | runtime consumes the immutable versioned Phase 1 catalogue and rejects definition/state version mismatch | starter content activation and golden catalogue simulation remain later M13 |

Phase 2 adds no command, GUI, Paper, PlaceholderAPI, statistics, replay, Atlas, distributed,
provider or compatibility implementation. M15â€“M22 ownership remains unchanged.

### M13 Phase 3 presentation, runtime-integration and closure evidence

| Allocation | Completed M13 portion | Verification / retained owner |
|---|---|---|
| `ZBW-PROG-009/010/012/013`, `ZBW-ADDON-081..091` | existing M09 command and GUI frameworks expose player/admin objective, quest, achievement, challenge, battle-pass and reward-claim flows with M03 execution authorization, audit context, validation, pagination/filtering, stale-view rejection and confirmation-protected mutation | `M13PresentationCatalogTest`, `M13PresentationTest`, `m13_inventories.py`; M15 statistics, M16 PlaceholderAPI and M17 replay remain separate owners |
| `ZBW-PROG-009/010/012/013` runtime closure | M08 event adaptation, durable M13 objective projection/completion, stable M12 reward intent/delivery and player feedback form the validated end-to-end path; duplicate, reconnect, restart, expiry, invalid-definition and reward-failure paths retain idempotent recovery | `M13RuntimeTest`, `JdbcM13StateRepositoryTest`, Java 8/21 reactors and quality gates; M19/M20 distributed transport and M22 compatibility remain later owners |
| `ZBW-CONTENT-004..006` | immutable validated quest, achievement and season definition graphs are consumed by the completed runtime and presentation surfaces | catalogue/reference tests and strict JavaDoc/API compatibility; M14 cosmetic content is not started |
| M13 primary Paper projection | Java 21 owner-thread feedback uses titles, action bars, sounds and particles without business logic or synchronous database access | `M13PresentationTest`; M22 retains legacy/runtime compatibility certification |

M13 is completed by checkpoints `162b62cdfe64d0f9979d19cb78fe7057154c9529`,
`8915963f47907dfd27903040381fa60d9eca174f` and
`2b22e83bb22102719003c2cd1485165b4a21958f`. The implementation references are
`IMPLEMENTATION_M13_PHASE1.md`, `IMPLEMENTATION_M13_PHASE2.md`,
`IMPLEMENTATION_M13_PHASE3.md`, `API_M13_PHASE3.md`, `M13_COMMANDS.md` and `M13_GUI.md`.
M16 is the next active milestone. M16 PlaceholderAPI, M17 replay and every later milestone boundary remain unchanged and unclaimed; M15 is complete in checkpoints `965291c`, `9758c71`, `51af49b`, `0cee0b3`, `badd504` and `586797f`.

### M14 Phase 1 cosmetic/profile/calendar foundation evidence

| Allocation | Implemented Phase 1 portion | Verification / remaining owner |
|---|---|---|
| `ZBW-PROG-006..008`, `ZBW-ADDON-026..040`, `ZBW-ADDON-369..378` | typed cosmetic/category/rarity identities, immutable versioned definitions, revisioned/audited loadouts, production-count gate and bounded effect/entity configuration contracts | `M14FoundationTest`; concrete ownership/equipment/runtime effects, approved 300-definition catalogue, M09 presentation and Paper projection remain later M14 phases |
| `ZBW-PROG-014`, M14 portion of `ZBW-ADDON-274..282` | private profile/effect settings and versioned calendar campaign windows that reference existing M12 rewards | invalid-window/privacy/revision tests; application/persistence/claim/calendar presentation remain later M14 phases |
| M12/M13 dependency boundary | cosmetic definitions reuse M12 entitlement IDs and M13 quest/achievement IDs; campaigns reuse M12 reward IDs | additive 129-class API checkpoint preserves every immutable M13 signature |
| M14 persistence boundary | `CosmeticStateRepository`, `ProfileSettingsRepository` and `M14Service` accept caller-owned M04/M12 `UnitOfWork`, optimistic revisions, idempotency keys and audit metadata | contract/API review; concrete SQL remains a later M14 phase, with no duplicate storage system |

### M14 runtime and primary-presentation implementation evidence

| Allocation | Implemented M14 portion | Verification / retained owner |
|---|---|---|
| `ZBW-PROG-006..008` | versioned catalogue lookup, M12 entitlement ownership check, duplicate-safe revisioned loadout persistence, disabled/unknown fallback, M09 command/GUI registration and owner-thread budgeted Paper feedback | Java 8/21 reactor evidence; `M14Runtime`, `M14PresentationBindings`, `M14GuiPages`, `M14PaperProjection`; 300 approved original definitions and production PT remain the M14 content/release gate |
| `ZBW-PROG-014`, `ZBW-ADDON-274..282` | immutable profile updates/visibility, revision/audit persistence port use, calendar active/not-started/expired decisions and M12 reward-reference handoff | `M14ProfileRuntime`, `M14CampaignRuntime`, M09 presentation catalogue; no second reward or storage system |

M14 implementation does not allocate statistics (M15), PlaceholderAPI (M16), replay (M17), Atlas/moderation (M18), distributed systems (M19/M20), external providers (M21), or compatibility (M22).

Phase 1 adds no M09 presentation or Paper behavior and does not implement M15 statistics, M16
PlaceholderAPI, M17 replay, M18 Atlas/moderation, M19/M20 distributed systems, M21 external
providers or M22 compatibility.

## Replay (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-REPLAY-001 | replay metadata/manifest identities | replay/database | replay info/list / replay.use | replay browser/details | Replay API/events | count/recent ID | lifecycle/identity | replay guide; M17 |
| ZBW-REPLAY-002 | capture buffer/codec/chunks | replay/performance | recording admin / manage | recording status | capture/event SPI | recording state | ordering/load PT | replay format guide; M17 |
| ZBW-REPLAY-003 | fidelity classification/source metadata | replay | telemetry debug / staff | telemetry detail | Telemetry API | safe availability | semantic/golden tests | evidence semantics; M17 |
| ZBW-REPLAY-004 | playback scene/session/render adapter | replay/gui | replay view controls / view.* | viewer controls | Viewer API/events | playback state | speed/timing/CT | viewer guide; M17/M22 |
| ZBW-REPLAY-005 | timeline index/search | replay | replay search / view | timeline | Timeline API | â€” | filter/jump correctness | timeline guide; M17 |
| ZBW-REPLAY-006 | telemetry index/annotations/case links | replay/anticheat/security | annotation/case/export / staff.* | staff investigation | Evidence API/events | restricted none | auth/source/ST | staff evidence guide; M17 |
| ZBW-REPLAY-007 | replay access/favourite/share data | replay/security | replay player/map/favoriteâ€¦ / granular | browser/history/settings | Access API/events | safe history | privacy/permission ST | player replay guide; M17 |
| ZBW-REPLAY-008 | payload store/retention/hold/migrations | replay/security/integrations | protect/archive/delete/restore/migrate / admin.* | storage/retention admin | Store SPI/events | storage health | provider/hold/MT/ST | storage/retention guide; M17 |
| ZBW-REPLAY-009 | recovery/quarantine/backpressure | replay/performance | repair/cleanup / repair,force | repair/queue health | Integrity API/events | queue/integrity health | corruption/crash/PT | recovery runbook; M17 |
| ZBW-REPLAY-010 | privacy/chat policy + full surface aliases | replay/security/messages | all `/replay` / replay.* | privacy/access panels | privacy/access APIs | allowed replay values | deletion/access ST | privacy/command reference; M17 |

## Atlas and anticheat evidence (13)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ATLAS-001 | Atlas orchestration/policy | atlas | atlas/review / atlas.use,review | Atlas main/review | Atlas API/events | eligibility/state | workflow E2E | Atlas guide; M18 |
| ZBW-ATLAS-002 | eligibility policy/profile reads | atlas/permissions | eligibility/approve / bypass.* | eligibility | Eligibility SPI | required/eligible | bypass/auth ST | eligibility guide; M18 |
| ZBW-ATLAS-003 | case repository/source adapters | atlas/database | case create/manage / manage.cases | case manager | Case API/events | open cases | source/dedupe E2E | case guide; M18 |
| ZBW-ATLAS-004 | anonymized projection/identity vault | atlas/security | reveal / view.identity | restricted reveal | Identity API/event | no real identity | privacy ST | anonymization policy; M18 |
| ZBW-ATLAS-005 | reservation/review session records | atlas/redis | review/skip / review,skip | reserved review | Reservation/Review API | queue/reserved | race/conflict/reconnect | reviewer guide; M18/M19 |
| ZBW-ATLAS-006 | verdict/reason registries | atlas/messages | verdict/reason admin / manage | selection/editors | Verdict/Reason SPIs | verdict safe values | catalog/validation | verdict guide; M18 |
| ZBW-ATLAS-007 | reviewer profile/reputation ledger | atlas/database | reviewer/reputation / manage.reviewers | profile/stats | Reviewer API/events | reviewer stats | scoring/history | reviewer admin guide; M18 |
| ZBW-ATLAS-008 | accuracy calculators/outcome links | atlas | accuracy debug / debug | accuracy detail | Accuracy SPI/events | accuracy values | formula/trusted outcome | accuracy guide; M18 |
| ZBW-ATLAS-009 | abuse signals/actions/appeals/audit | atlas/security | warn/suspend/invalidate / admin.* | abuse/appeal | Abuse API/events | suspension only | adversarial ST/PT | anti-abuse guide; M18 |
| ZBW-ATLAS-010 | interaction gates + reward integration | atlas/rewards | rewards / atlas.reward | interaction/rewards | Interaction/Reward events | streak/rewards | farming/idempotency ST | reward policy; M18 |
| ZBW-ATLAS-011 | canonical surface registry + aliases | atlas/permissions/gui | all `/atlas` / atlas.* | all Atlas GUIs | full Atlas API | all listed Atlas PH | surface inventory | command/permission/PH ref; M18 |
| ZBW-ATLAS-012 | staff final decision/punishment policy | atlas/security/integrations | override/close/punish / staff.* | staff final review | Moderation SPI/events | restricted none | no-auto-ban ST | staff policy; M18/M21 |
| ZBW-ATLAS-013 | caches/queue service/metrics | atlas/redis/performance | performance/diagnostics / admin | performance dashboard | Atlas health API | queue/health | combined-load PT | Atlas tuning; M18/M19 |

## Statistics and leaderboards (8)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-STATS-001 | statistic definitions/projections/tables | statistics/database | stats / stats.view | stats profile | Statistics API/events | all lifetime stats | event matrix | statistics guide; M15 |
| ZBW-STATS-002 | dimensional aggregates/partitions | statistics | stats mode/map/group/time / view | dimension selectors | Dimension API | dynamic dimensions | query/separation | statistics dimensions; M15 |
| ZBW-STATS-003 | ratio calculators | statistics/placeholders | stats ratios / view | ratios | Ratio API | KDR/FKDR/WLR/etc. | formula/zero tests | ratio reference; M15 |
| ZBW-STATS-004 | streak state/history/reward links | statistics/rewards | streak/admin / streak.* | streak views | Winstreak API/events | streak variants | reset/private/MT | winstreak guide; M15 |
| ZBW-STATS-005 | inbox/idempotent projection/repair | statistics/database/redis | stats repair/debug / admin | consistency view | Projection API/events | health only | duplicate/partition | consistency guide; M15/M19 |
| ZBW-STATS-006 | admin correction/audit/import/export | statistics/migration | stats admin all / admin.* | inspector/repair | Admin Statistics API | â€” | dry-run/rollback ST/MT | admin stats guide; M15 |
| ZBW-STATS-007 | leaderboard definitions/presenters | statistics/gui/integrations | leaderboard / use,manage | leaderboard | Leaderboard API | top/nth/rank | ranking/privacy | leaderboard guide; M15/M21 |
| ZBW-STATS-008 | rank materialization/caches | statistics/redis/performance | leaderboard refresh / manage | cache status | Ranking API/events | cached ranks | ties/large-data PT | leaderboard tuning; M15/M19 |

## PlaceholderAPI (6)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-PAPI-001 | native expansion/identifier registry/aliases | placeholders | placeholder list/reload / manage | placeholder browser | Placeholder API | namespace/version | registration/alias CT | placeholder reference; M16 |
| ZBW-PAPI-002 | statistic resolvers | placeholders/statistics | test/search / use | browser/test | resolver SPI | statistic families | matrix/dimensions | generated reference; M16 |
| ZBW-PAPI-003 | progression resolvers | placeholders/progression | test/search / use | browser/test | resolver SPI | progression families | matrix/custom currency | generated reference; M16 |
| ZBW-PAPI-004 | arena/team/network/replay/Atlas/rank resolvers | placeholders | test/search / scoped view | browser/test | context resolver SPI | all listed families | context/privacy CT | generated reference; M16 |
| ZBW-PAPI-005 | bounded cache/deduper/metrics | placeholders/performance | performance/debug / debug | cache/performance | cache metrics API/events | health metrics | zero-I/O latency PT | tuning guide; M16 |
| ZBW-PAPI-006 | formatter/admin/dev registries | placeholders/messages | all placeholder admin / manage | browser/debug | formatter/family APIs | formatted variants | parser/locale/fallback | developer guide; M16 |

## Deployment, database, Redis, proxy and CloudNet (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-DEPLOY-001 | M08 VERIFIED shared-server game/session/arena assembly and bounded primary Paper runtime foundation; optional providers and release qualification remain later | deployment/performance | M09 deployment/health adapters / admin | M09 health/world/arena presentation; M21 provider panels | M08 Deployment API/events foundation | M16 deployment counts | M08 primary shared-server E2E PASS; M21 providers; M24 40-world/10-active PT | `IMPLEMENTATION_M08.md`; M08 foundation VERIFIED / M09 presentation / M21/M24 completion |
| ZBW-DEPLOY-002 | distributed application flows/state | deployment/proxy/redis | network status / admin | network manager | Network API/events | server/backend/proxy | cross-server E2E | proxy network guide; M20 |
| ZBW-DEPLOY-003 | deployment validator/safe state | deployment/integrations | validate / admin.validate | validation GUI | validation API/events | provider status | missing-service tests | deployment guide; M03/M20 |
| ZBW-DEPLOY-004 | proxy protocol + Velocity/Bungee adapters | proxy/security | proxy commands / proxy.* | proxy manager | Proxy API/events | proxy/backend | equivalence/signature ST | Velocity/Bungee guides; M20 |
| ZBW-DEPLOY-005 | CloudNet provider/scaling state | cloudnet | cloudnet commands / cloudnet.* | CloudNet manager | ServiceDiscovery API/events | cloudnet status | scale/drain/crash | CloudNet guide; M21 |
| ZBW-DEPLOY-006 | Redis keys/streams/locks/invalidation | redis/security | redis manage/diagnostics / redis.* | Redis manager | Coordination API/events | redis status | partition/load/ST | Redis guide; M19 |
| ZBW-DEPLOY-007 | M04 typed repositories/UoW; Hikari SQLite/MySQL/MariaDB adapters; migration/cache/recovery foundation | database/security | database manage/migrate / database.* remains M09 | database manager remains M09 | Storage/Migration/Recovery APIs | sanitized pool/schema status | SQLite DB/MT plus certified zero-skip MySQL/MariaDB CT, query plans, pool and backup evidence; PT/ST later | M04 database/migration/backup/query-plan guides; M04 VERIFIED |
| ZBW-DEPLOY-008 | M04 versioned envelope plus atomic inbox/outbox/idempotency/order store; Redis/proxy transport remains M19/M20 | deployment/redis/database | message debug / debug later | protocol health later | `MessageEnvelope`/`MessageRepository` | protocol versions later | order/duplicate/retry/crash recovery pass on SQLite, MySQL and MariaDB; partition/rolling later | M04 API/implementation; M04 VERIFIED / protocol guide M19 |
| ZBW-DEPLOY-009 | degradation/recovery coordinator | deployment/performance | maintenance/recover / force | network health | Recovery API/events | degraded state | chaos/MT | failure runbook; M19/M20 |

## Integrations and compatibility (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-INT-001 | PlaceholderAPI adapter | integrations/placeholders | integration status/reload / manage | integration manager | provider adapter | native | absence/version CT | Placeholder install guide; M16 |
| ZBW-INT-002 | Vault economy/permission/chat adapters | integrations | vault status / manage | integration manager | provider SPIs | provider status | provider/transaction CT | Vault guide; M21 |
| ZBW-INT-003 | LuckPerms context/meta adapter/cache | integrations/permissions | luckperms status / manage | integration manager | Permission/Meta provider | safe rank meta | invalidation/context CT | LuckPerms guide; M21 |
| ZBW-INT-004 | M06 packet/capability boundary and primary-safe mapping contract only; ProtocolLib/direct multi-version adapters remain M22 | integrations/performance | packet diagnostics / debug later | packet health later | Packet SPI/events | packet health | primary capability CT M06; version/rate ST/PT M22 | packet guide; M06 foundation/M22 completion |
| ZBW-INT-005 | M06 WorldProvider SPI/native primary adapter; optional provider adapters and intermediate migration remain M21 | integrations/world | provider migrate/status / world.* later | world/integration manager later | WorldProvider SPI | provider status | native primary CT/E2E M06; provider CT/MT M21 | provider guides; M06 foundation/M21 completion |
| ZBW-INT-006 | internal/Citizens/ZNPC NPC adapters/data migration | npcs/integrations | npc CRUD/import/export / npc.* | NPC manager | NpcProvider API/events | NPC state if safe | provider/switch CT | NPC guide; M21 |
| ZBW-INT-007 | internal/Decent hologram adapters | holograms/integrations | hologram manage / hologram.* | hologram manager | HologramProvider | content values | rate/provider CT/PT | hologram guide; M21 |
| ZBW-INT-008 | Grim/Vulcan normalized alert adapters | anticheat/integrations | alerts/manage / anticheat.* | alert manager | AntiCheatProvider/events | safe violation values | dual/dedupe/PT/ST | Grim/Vulcan guides; M21 |
| ZBW-INT-009 | native/AlessioDP party provider + migration | parties/integrations | party + admin / party.* | party/admin | Party API/events | party fields | provider/cross-server MT | party guide; M20/M21 |
| ZBW-INT-010 | M06 neutral capability contracts and Paper 1.21.1 primary modern adapter only; M22 completes legacy/intermediate adapters, all remaining runtime rows, Via and Bedrock alternatives | integrations/compatibility | compat diagnostics / debug later | compatibility/alt controls later | Compatibility API/events foundation M06 | server/client version later | primary-foundation CT M06; complete 1.8â€“1.21/translated/Bedrock matrix M22 | compatibility guide; M06 foundation/M22 completion |

## GUI, commands, permissions and localization (6)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-UX-001 | UI page model/Paper renderer/state | gui/messages/performance | gui reload / use,manage | all required GUIs | GUI API/events | rendered safe values | inventory/accessibility PT | GUI guide; M09 |
| ZBW-UX-002 | GUI definition editor/schema migration | gui | gui edit/import/export / gui.* | GUI editor | GUI registry API/events | preview values | undo/validation/MT/ST | GUI editor guide; M09 |
| ZBW-UX-003 | command tree/adapters/help/audit | commands/messages | all required commands / per action | command inspector/help | Command API/events | â€” | command inventory/contexts | command reference; M09+ |
| ZBW-UX-004 | M03 `PermissionNode`/request/decision/service plus sole exact-grant implementation, target scoping, 33 actions, one-hop aliases and audit port | `permissions.yml`, security metadata | command inventory remains M09 | inspector remains M09 | authorization API; decision audit port | â€” | exact/target/alias/deny/null/centralization tests VERIFIED | `PERMISSIONS_M03.md`; M03 VERIFIED / M09 surfaces |
| ZBW-UX-005 | M03 locale/message/typed-parameter API plus immutable catalogs, fallback, completeness and deterministic import/export; component renderers remain M22 | `config.yml`, `messages.yml` | language/reload remain M09/M22 | language/editor/report remain M09/M22 | localization API | locale-aware contract; PAPI remains M16 | fallback/completeness/switch/escape/malformed tests VERIFIED | `LOCALIZATION_M03.md`; M03 VERIFIED / M22 rendering |
| ZBW-UX-006 | accessibility/capability patterns | gui/messages | settings / player | accessible alternatives | capability API | settings | task usability/Bedrock E2E | accessibility matrix; M09/M22 |

## Operations, security and delivery (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-OPS-001 | M03 immutable versioned configuration; M08.1 VERIFIED typed replaceable arena defaults and validation profiles remove anonymous layout/generator heuristics | all 36 files plus arena/default profile mapping | config command remains M09 | editor remains M09 | typed configuration/profile models | config version later | M03 schema tests; M08.1 profile copy/bounds/exact-identity tests | `CONFIGURATION_REFERENCE_M03.md`, `TEAM_CONFIGURATION_M08_1.md`; M03/M08.1 VERIFIED |
| ZBW-OPS-002 | M03 `SecretRef`, injected provider/environment/protected-file sources, zeroizable lease, exact redactor and export allowlist | `security.yml` plus provider credential refs | security diagnose remains M09/M24 | security status remains later | secret API/services | none | priority/missing/zeroize/redaction/allowlist tests VERIFIED | `API_M03.md`; M03 VERIFIED / M24 operations |
| ZBW-OPS-003 | M03 validates all 36 documents, external-check ports, cross-document dependencies, duplicate/missing files and bounded option counts | all 36 schemas | validate/doctor remains M09/M23 | dashboard remains later | typed validation reports | validation status remains later | malformed/unknown/dependency/secret/cross-document/fixture tests VERIFIED | `CONFIGURATION_REFERENCE_M03.md`; M03 VERIFIED / platform checks later |
| ZBW-OPS-004 | M03 prepare/apply/reverse-rollback plans, restart reporting, targets and last-known-good publication | every option declares target/restart metadata | reload command remains M09 | reload controls remain M09 | transactional reload service | reload state remains later | success/prepare/apply/rollback/restart tests VERIFIED | `CONFIGURATION_REFERENCE_M03.md`; M03 VERIFIED |
| ZBW-OPS-005 | M05 stable failure taxonomy/report/sink, bounded retry policy, single-probe circuit breaker, task/lifecycle/recovery isolation and correlation | retry/circuit caller policy plus scheduler deadlines; no secrets in messages | diagnostics/recover commands remain M09/M24 | health/error panels remain M24 | `FailureKind`, `FailureReport`, `FailureSink`, recovery event | only stable sanitized codes/state | exception/sink/fault injection, retry caps, circuit transitions, rollback/force and recovery conflict tests VERIFIED | `OBSERVABILITY_M05.md`, `LIFECYCLE_M05.md`; M05 VERIFIED |
| ZBW-OPS-006 | M05 bounded health/metric registries, allowlist-only diagnostics, seeded redactor and bounded failure-isolating Plugin Doctor engine | max series/sources/contributors/fields/checks and per-check timeout | health/performance/doctor commands remain M24 | provider dashboards remain M24 | `Health`, `Diagnostics`, `PluginDoctor` extension contracts | safe bounded health families; PAPI wiring later | cardinality/capacity/duplicate/source-failure/classification/redaction/seed-leak/Doctor failure tests VERIFIED | `OBSERVABILITY_M05.md`, `PLUGIN_DOCTOR_M05.md`; M05 substrate VERIFIED / M24 surfaces |
| ZBW-OPS-007 | external stats/Discord providers | integrations/security | external-api manage / api.* | integration manager | ExternalStats SPI/API | only scoped public | auth/rate/privacy ST/PT | external API guide; M16/M21 |
| ZBW-OPS-008 | checksum-locked Maven, eight-project reactor, two M02 CI workflows, dual locks/SBOMs/notices and API baseline | `.mvn/`, POMs, `build/*`, workflows | clean build and M01/M02 validators / maintainer | â€” | five M02 Java 8 artifacts | build/API versions in manifests | five-JDK offline builds, quality and governance CI | M01/M02 implementation guides; M01/M02/M24 |
| ZBW-OPS-009 | documentation inventory/generators | docs config | docs generate / maintainer | doc links | generated API docs | generated PH docs | link/inventory lint | all guides; continuous |

## Quality (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-QA-001 | Python governance plus verified Java suites; M08.1 adds deterministic shared-limit, profile, assembler, victory and layout tests | quality/toolchain manifests | governance runner | â€” | test-only helpers | â€” | M08.1 zero-skip neutral/Paper regression and quality gates | `IMPLEMENTATION_M08_1.md`; all |
| ZBW-QA-002 | M08.1 VERIFIED foundational 8x1, 8x2, 4x3, 4x4, 2x4, 12x3 and 64x4 lifecycle/layout matrix; full feature/mode E2E remains later | deterministic arena fixtures | â€” | tested GUIs M09+ | M08.1 typed APIs; later events | tested mode PH M16 | layout foundation M08.1; full gameplay matrix M10/M24 | `LAYOUT_COMPATIBILITY_M08_1.md`; M08.1 foundation / M10/M24 completion |
| ZBW-QA-003 | replay/Atlas/stat/rank/PH suites | test fixtures | â€” | tested surfaces | tested APIs | full PH test | mandated cases | test report; M16â€“M18 |
| ZBW-QA-004 | benchmark/load harness + result store | benchmark manifest | benchmark run / developer | performance dashboard | benchmark API | performance metrics | all workload PT | benchmark report; M24 |
| ZBW-QA-005 | M1 governance plus M02 Checkstyle/SpotBugs/JaCoCo, architecture and binary API gates; release/mutation/vulnerability evidence continues M24 | quality policy, POM profiles, CI | M02 validator / maintainer | â€” | exact binary signature baseline active | â€” | zero static findings, enforced 90/85 domain/application, forbidden imports and binary drift | quality gates + M02 implementation; M01/M02/M24 |
| ZBW-QA-006 | compliance report generator | release metadata | compliance / maintainer | compliance dashboard | compliance schema | status counts | 672-semantic-ID audit plus atomic annex evidence | final compliance; M24 |
| ZBW-QA-007 | all three coverage verifiers orchestrated by `run_m01_validation.py` | Python 3.12.13 + normative manifests | M1 validator / maintainer | generated combined reports | coverage report schema | coverage percentage/counts | 6,438 Master + 473 addon + 55 decision rows and document/ADR gate | coverage audits + implementation guide; M00/M01/M24 |

## Ecosystem and migration (5)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ECO-001 | migration framework/adapters/maps/reports | migration/integrations | migrate/import/export / migrate.* | migration manager | Migration API/events | migration progress | fixture dry-run/rollback MT | migration guide; M23 |
| ZBW-ECO-002 | M02 immutable extension metadata and deterministic target/catalogue validator | schema-1 fixtures | validator library; CLI later | module manager later | marketplace-compatible metadata/validation contracts | extension status later | valid/invalid/duplicate/unsupported-version fixture tests | `API_M02.md`, ADR-0017; M02 VERIFIED / M23 tooling |
| ZBW-ECO-003 | M02 SemVer/range/public lifecycle contracts and binary signature baseline; example extension/tooling later | API compatibility/deprecation policy | report command later | API browser later | SDK/public API | api version later | unsupported API and exact binary compatibility checks | `API_M02.md`; M02 VERIFIED / M23 example/tooling |
| ZBW-ECO-004 | migration/Doctor provider registries | migration/doctor/performance | doctor/migrate / admin | Doctor/migration | Doctor/Migration SPIs | health | provider extension CT | operations SDK; M23 |
| ZBW-ECO-005 | AI suggestion-only ports/policy | security/integrations (disabled) | future provider manage / admin | reviewed suggestion surface | AI suggestion SPI | none | threat/design CT before enable | future API policy; post-M24 MAY |

## Original content and provenance (11)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-CONTENT-001 | provenance/binary scan plus M02 generic typed content contracts; no creative asset or balance is packaged | exact provenance/rights manifest; runtime content later | validators / maintainer | â€” | content registry/provider/pack metadata | â€” | clean-room asset/hash/repository and registry tests | asset provenance + M01/M02 implementation; M01/M02/M24 |
| ZBW-CONTENT-002 | shop balance definition pack | `content.yml`, `shops.yml` | content shop validate/preview / content.manage | shop profile editor | BalanceProfile API/events | active shop profile | schema/golden/E2E | original starter catalogue; M11 |
| ZBW-CONTENT-003 | M10 versioned mode-profile identity/schema/selection and deferred binding; M11 supplies and activates original per-mode balance definition packs | `content.yml`, `modes.yml` | M10 validate/inspect selection metadata; M11 balance preview/activate / content.manage | M10 mode metadata editor; M11 balance editor | M10 mode-profile contract; M11 ModeBalance runtime/events | M16 active mode profile | M10 schema/version/deferred-binding tests; M11 golden mode matrix/PT | original starter catalogue; M10 framework / M11 balance completion |
| ZBW-CONTENT-004 | quest seed definitions | `quests.yml`, `content.yml` | quest validate/import / quest.manage | quest editor | Quest registry/events | quest ID/progress | objective/config E2E | original starter catalogue; M13 |
| ZBW-CONTENT-005 | achievement seed definitions | `achievements.yml`, `content.yml` | achievement validate/import / achievement.manage | achievement editor | Achievement registry/events | achievement ID/tier | tier/MT/E2E | original starter catalogue; M13 |
| ZBW-CONTENT-006 | starter season/tier definitions | `battlepass.yml`, `content.yml` | pass validate/import / battlepass.manage | pass editor | BattlePass registry/events | season/tier/track | claim/rollover/MT | original starter catalogue; M13 |
| ZBW-CONTENT-007 | cosmetic seed + 300-definition gate | `cosmetics.yml`, content packs | cosmetics validate/import / cosmetics.manage | cosmetics/provenance editor | Cosmetic registry/events | catalogue/provenance counts | 300-count/hash/PT | starter/provenance guides; M14 |
| ZBW-CONTENT-008 | private preset definitions | private-games/content config | private preset validate/preview / private.manage | host/admin preset editor | PrivatePreset API/events | active preset/multipliers | GUI/generator E2E | Resource Scarcity catalogue; M20 |
| ZBW-CONTENT-009 | M06 semantic feedback/effect IDs and Paper 1.21.1 mappings; M14 starter definitions; M22 mandatory legacy mappings/fallback certification | content/compatibility/messages | effect validate/preview / content.manage later | effect preview later | SemanticEffect API/events | selected effect/fallback later | primary adapter CT M06; catalogue/usability M14; legacy usability CT M22 | starter + fallback matrix; M06/M14/M22 |
| ZBW-CONTENT-010 | M02 typed content definition/provider/pack/version contracts and immutable duplicate-safe registry; parsing/migration later | pack metadata contract; `content.yml` remains M03 | runtime validate/reload/migrate later | manager later | `ContentRegistry` and provider contracts | pack version/health later | ordering, immutability, lookup, duplicate ID and metadata tests | `API_M02.md`; M02 VERIFIED / M03 runtime |
| ZBW-CONTENT-011 | machine registry `build/asset-provenance.json`; zero approved/packaged assets; deterministic scanner | provenance manifest | M1 validator / maintainer | â€” (file report in M01) | provenance schema; runtime API later | zero approved/packaged in manifest | path/hash/licence/status drift CI | `docs/ASSET_PROVENANCE.md`; implementation guide; M01/M14/M24 |

## Discord provider topology (8)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-DISCORD-001 | M02 secure read-only integration and immutable outbound-envelope contracts; gateway/outbox runtime later | typed scope/sensitivity/deadline contract | runtime status/debug later | diagnostics later | `DiscordIntegrationApi`, `DiscordEventEnvelope` | delivery health later | identity/idempotency/sensitivity/query/null/malformed tests | Discord architecture + `API_M02.md`; M02 contracts VERIFIED / M16 runtime |
| ZBW-DISCORD-002 | M02 asynchronous provider lifecycle/delivery/capability/metadata SPI; registry/selection runtime later | provider capability contract | runtime list/reload/drain later | provider manager later | `DiscordProvider`, `DiscordCapabilities` | runtime health later | capability, lifecycle shape, delivery classification tests | Discord architecture + `API_M02.md`; M02 contracts VERIFIED / M16 runtime |
| ZBW-DISCORD-003 | embedded webhook adapter | destination/event/rate + SecretRef | webhook validate/test / admin.discord.webhook | webhook settings/status | webhook delivery events | webhook health only | timeout/rate/allowlist ST | webhook guide; M16 |
| ZBW-DISCORD-004 | external bot transport + link records | transport/scopes/link policy + secrets | link/unlink + bot diagnose / discord.* | link/privacy/bot health | external protocol/link events | consented link state | auth/replay/link CT/ST | external bot protocol; M16 |
| ZBW-DISCORD-005 | M02 custom provider SPI integrated with extension metadata/API ranges; sample/runtime later | extension metadata schema | validator contract; commands later | extension manager later | custom `DiscordProvider` SPI | provider health contract | metadata/capability/provider contract tests | `API_M02.md`; M02 contracts VERIFIED / M16/M23 sample |
| ZBW-DISCORD-006 | M03 `DisabledDiscordProvider`: no I/O/thread/capability, stopped lifecycle, disabled health and typed rejection | `integrations/discord.yml` enabled false | discord status remains M16 | disabled state remains M16 | provider-neutral no-op provider | disabled | provider lifecycle/capability/delivery contract tests VERIFIED; gameplay E2E M16 | `API_M03.md`; M03 VERIFIED / M16 E2E |
| ZBW-DISCORD-007 | bounded outbox/retry/circuit/dead letter | queue/deadline/backoff/rate budgets | discord retry/drain / admin.discord | queue/circuit dashboard | delivery/failure events | queue/circuit health | outage/saturation/PT | failure runbook; M05/M16 |
| ZBW-DISCORD-008 | M03 provider-neutral `SecretRef`, injected resolution/zeroization/redaction foundation; provider rotation remains M16 | credential references only; no token values | diagnose/rotate remain M16/M24 | redacted status remains later | secret API integrated with config foundation | â€” (secrets prohibited) | seeded priority/missing/zeroize/redaction/export tests VERIFIED | `API_M03.md`; M03 VERIFIED / M16 rotation |

## Mandatory Minecraft 1.8 fallbacks (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-COMPAT-001 | M01 artifact/toolchain boundaries; M06 neutral compatibility contract and Paper 1.21.1 primary boundary; M22 implements/certifies the mandatory 1.8 and complete runtime matrix | toolchain/module manifests | validator / maintainer; runtime status later | â€” before runtime surfaces | capability API M06 | â€” before runtime surfaces | five-JDK/class-major M01; primary foundation M06; startup/gameplay matrix M22 | runtime/fallback matrices; M01/M06 foundation/M22 completion |
| ZBW-COMPAT-002 | M06 `zbw-compat-api` plus `zbw-compat-v1_20-v1_21` primary boundary only; the narrow `zbw-compat-v1_8` adapter is delivered exclusively in M22 | adapter selection | compat inspect / debug later | capability diagnostics later | Compatibility SPI M06 | capability health later | forbidden imports/primary startup M06; 1.8 startup/class-leak CT M22 | compatibility architecture; M06 foundation/M22 completion |
| ZBW-COMPAT-003 | M06 semantic material/item/metadata contract and primary mappings; M22 legacy material/data/NBT registry and anti-forgery equivalence | material fallbacks | compat material test / debug later | mapping inspector later | material/item capability API M06 | fallback material ID later | primary round-trip M06; legacy round-trip/tamper/MT CT M22 | fallback matrix; M06 foundation/M22 completion |
| ZBW-COMPAT-004 | M06 semantic particle contract/primary mappings and bounded fallback outcome; M22 legacy renderer and complete usability matrix | particle fallbacks/budgets | compat particle preview / debug later | effect preview later | particle capability API M06 | fallback/suppression state later | primary mapping/budget M06; legacy packet/budget/usability CT M22 | fallback matrix; M06 foundation/M22 completion |
| ZBW-COMPAT-005 | M06 semantic sound contract/primary mappings and visible-cue outcome; M22 legacy renderer and complete usability matrix | sound fallbacks/volume | compat sound preview / debug later | sound preview later | sound capability API M06 | fallback/suppression state later | primary mapping M06; legacy mapping/usability CT M22 | fallback matrix; M06 foundation/M22 completion |
| ZBW-COMPAT-006 | M06 semantic text/action contract and primary renderer mapping; M22 legacy format/action-equivalence renderer | text/action fallback rules | compat text preview / debug later | rendered preview later | text capability API M06 | renderer class later | primary render CT M06; legacy task/format/Unicode E2E M22 | fallback matrix; M06 foundation/M22 completion |
| ZBW-COMPAT-007 | M06 entity/packet/UI/input capability contracts and primary safe mappings; M22 legacy fallback providers and cleanup equivalence | entity/packet/gui mappings | compat component inspect / debug later | component diagnostics later | packet/entity/UI capabilities M06 | capability/fallback later | primary capability/cleanup CT M06; legacy packet/cleanup/input E2E M22 | fallback matrix; M06 foundation/M22 completion |
| ZBW-COMPAT-008 | M06 typed degradation, unsupported and last-known-good decision policy; M22 proves the policy on every legacy/full-feature path | suppression/fallback order | compat validate / admin.validate later | validation report later | fallback decision events M06 | suppression reason later | primary mutation/fault CT M06; full matrix fault CT M22 | compatibility policy; M06 foundation/M22 completion |
| ZBW-COMPAT-009 | M01 normative fallback/fixture inventories; M06 validates primary mappings and records only Paper 1.21.1 foundation evidence; M22 completes every legacy/full-feature row and generated report | compatibility/fixture manifests | validator / maintainer | â€” before runtime diagnostics | fixture/report schema plus M06 capability report | fixture/coverage counts | row/hash/no-claim M01; primary mapping gate M06; complete fixture matrix M22 | fallback/runtime matrices; M01/M06 foundation/M22/M24 completion |

## Dependency licensing and redistribution (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-LICENSE-001 | M01 14-artifact tool registry plus M02 170-component/535-file exact Maven registry and two CycloneDX SBOMs | acquisition/lock policy JSON | dependency validators / maintainer | â€” | CycloneDX/lock schemas | â€” | inventory/rights/licence/source/hash scan | dependency audit + M01/M02 implementation; M01/M02/M24 |
| ZBW-LICENSE-002 | immutable tool lock plus Maven Central URL/SHA-256/size lock; wrapper and restore verify before offline build | default-deny lock/update policy | lock `generate|capture|validate|seed|restore` / maintainer | â€” | dependency metadata files | build versions only | hash/source/dynamic-version/staleness/offline CI | dependency audit; M01/M02 |
| ZBW-LICENSE-003 | explicit product-right booleans; all activated M02 Maven components are build/test-only and non-bundled | rights records per artifact/component | dependency `validate` / maintainer | â€” | governance records only | â€” | redistribution/product-binary rejection | root/build notices; M01/M02/M24 |
| ZBW-LICENSE-004 | per-component SPDX/declaration/source evidence and generated M02 build notices; product shading/modification disabled | rights/obligation records | dependency `validate` / maintainer | â€” | SBOM properties | â€” | missing-rights/evidence and product-packaging scan | dependency audit/build notices; M01/M02/M24 |
| ZBW-LICENSE-005 | Only approved exact M02 JUnit/build/quality selections resolve; complete graph locked before normal offline build | BOM + scope policy | dependency and Maven lock validators / maintainer | â€” | M02 neutral provider SPIs | â€” | direct dependency/plugin lock and architecture checks | dependency/integration guides; M01/M02/M21 |
| ZBW-LICENSE-006 | repository scan rejects JAR/class/native/server/proprietary binaries outside ignored verified cache | prohibited suffix/name policy | dependency `validate` / maintainer | â€” | â€” | â€” | repository binary scan | contributor/legal guide; M01/M24 |
| ZBW-LICENSE-007 | deterministic M01/M02 CycloneDX SBOMs, build notices and asset provenance cross-gates | notice/provenance policy | M02 validator / maintainer | â€” | notice/SBOM schemas | approved/blocked counts | regeneration/staleness/licence CI | root/build notices + M02 implementation; M01/M02/M24 |

## Consolidated pre-code readiness decisions (20)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-READY-001 | M01 exact multi-JDK/class-major policy; M06 Java-8 neutral and Java-21 primary artifact boundary; M22 remaining artifact families | toolchain/module manifests | validator / maintainer; runtime command later | â€” before runtime surfaces | RuntimeCapability API M06 | â€” | five clean builds + graph/order/bytecode tests; primary boot CT M06; all-family boot CT M22 | runtime matrix + ADR-0006; M01/M06 foundation/M22 completion |
| ZBW-READY-002 | 22-row registry remains unclaimed; M06 may certify only the Paper 1.21.1 foundation scope, while M22 owns full lifecycle/gameplay certification for every declared row | private fixture/fallback manifests | validator / maintainer; certify later | â€” before runtime surfaces | capability/report schema M06 | â€” | inventory/hash/no-claim; primary foundation E2E M06; complete matrix E2E M22 | runtime/fallback matrices; M01/M06 foundation/M22 completion |
| ZBW-READY-003 | M02 generic content contracts; M06 semantic compatibility-renderer contract foundation only; 300 cosmetic definitions/runtime remain M14 | typed content/provenance metadata foundation | audit command later | catalogue/provenance/preview later | registry/provider and semantic renderer base contracts | none before M14 | generic registry M02; renderer capability CT M06; full count/visual/PT M14/M24 | `API_M02.md`; M02/M06 foundations / cosmetic plan M14 |
| ZBW-READY-004 | M02 capability IDs/sets and extension capability declarations only; DSL/schema/auth/workers remain M03/M05/M11 | capability metadata only | scripts commands later | script review later | capability contract foundation | none | capability duplicate/containment/metadata tests | `API_M02.md`; M02 foundation / scripting security later |
| ZBW-READY-005 | M01 14 tool artifacts plus M02 exact lock of 170 Maven build/test components and 535 files, generated SBOM/notices, offline restore/build | acquisition/lock policy | Maven lock `capture|validate|seed|restore` / maintainer | â€” | CycloneDX/lock schemas | build versions only | exact hash/licence/source/rights/staleness/offline gates | dependency audit + M02 implementation; M01/M02 VERIFIED |
| ZBW-READY-006 | exact public fixture hashes plus explicit private legacy lock states; M06 consumes only the Paper 1.21.1 build 133 fixture and M22 consumes every full row | `private-runtime-fixtures.json` | validator / maintainer; runtime verify later | â€” before runtime surfaces | fixture report schema/API M06 | no support placeholder before certification | hash/registry/no-binary; primary fixture CT M06; boot/game CT all rows M22 | runtime matrix; M01/M06 foundation/M22 completion |
| ZBW-READY-007 | M02 vendor-neutral event-push `AntiCheatProvider`, normalized alert/severity/subscription contracts; Grim/Vulcan/no-provider adapters remain M21 | no runtime provider/secret config in M02 | diagnose later | health GUI later | neutral provider and alert contracts | sanitized health later | alert bounds/null/type/thread metadata tests; adapter CT M21 | `API_M02.md`; M02 SPI foundation / M21 adapters |
| ZBW-READY-008 | exact BOM/plugin properties plus M02 five-module manual-DI graph, capability/provider SPIs, locked Maven graph and architecture checks | parent/BOM/quality/module/lock manifests | deterministic validators / maintainer | â€” | provider/extension/content SPIs; command/GUI later | â€” | dependency graph, forbidden imports, no globals/service locator, licence and binary tests | dependency audit + ADR-0007 + M02 implementation; M01/M02 VERIFIED |
| ZBW-READY-009 | benchmark thresholds remain immutable; M1 supplies deterministic result/gate schema only, not feature benchmarks | `quality-policy.json`, benchmark specification | M1 validator / maintainer; benchmark commands M24 | â€” in M01 | BenchmarkResult SPI remains later | â€” | deterministic gate/schema tests; workload PT at feature/M24 milestones | benchmark baseline + ADR-0009 + implementation guide; M01/M24 |
| ZBW-READY-010 | purpose-classified replay schema/encryption/access | replay privacy profile, chat off | replay privacy/export / `.privacy.replay.*` | replay privacy/export | ReplayPrivacy API/events | none â€” private | no-chat/access/export/encryption ST | privacy policy + ADR-0010; M03/M17 |
| ZBW-READY-011 | M04 retention/hold/release/tombstone schema and repository; scheduler/encrypted feature stores remain M17/M18 | retention classes | privacy commands remain M09/M17 | workflow remains M17/M18 | `RetentionPolicy`/`RetentionRepository` | none â€” private | duplicate/transaction/privacy storage IT plus certified MySQL/MariaDB restart persistence; clock/delete workflow later | privacy policy + M04 API; M04 VERIFIED / M17/M18 |
| ZBW-READY-012 | zero-asset provenance manifest, repository binary/resource scan and 49/473 clean-room coverage preservation | asset/dependency provenance manifests | M1 validator / maintainer | â€” (file report) | provenance machine schema; API later | approved/blocked counts in manifest | repository/content/hash/coverage scan | project licensing/provenance + implementation guide; M01/M23/M24 |
| ZBW-READY-013 | authenticated envelope/key/dedupe services | network security/SecretRefs | network peer/key rotate/diagnose / `.network.*` | peer/key/security report | SecureEnvelope API/events | none â€” security | forge/replay/rotation/fuzz ST | network security + ADR-0011; M03/M19/M20 |
| ZBW-READY-014 | M04 SQL authority, atomic outbox/inbox and SQLite single-JVM/single-writer topology guard; leases/Redis remain M19/M20 | deployment/database/redis | diagnose commands remain M09/M19 | sanitized pool health foundation | transaction/message contracts | health only | duplicate/retry/concurrency IT and seven plans/engine pass on certified MySQL/MariaDB; partition/lease later | network security + M04 pool/query docs; M04 VERIFIED |
| ZBW-READY-015 | immutable balance profile registry/migration | balance/content/modes/shops | balance validate/activate / `.content.balance.*` | balance editor/preview | BalanceProfile API/events | active version | schema/golden/simulation/PT | balance baseline + ADR-0014; M03/M10/M11 |
| ZBW-READY-016 | M04 encrypted/validated backup evidence coordinator, zero-loss/15-minute objectives and pool health; quota/degradation runtime remains M05/M24 | operations/backup/quotas | backup/restore drill commands remain M09/M24 | recovery/pool health foundation | `RecoveryService`/`SqlRecoveryCoordinator` | sanitized health only | encrypted/checksummed restore contract passes on certified MySQL/MariaDB; production RPO/RTO drills later | operational defaults + M04 runbook; M04 VERIFIED / M05/M24 |
| ZBW-READY-017 | two M02 CI workflows; exact quality manifest; static, coverage, binary, architecture, dependency and docs gates | quality policy, POM profiles, pinned workflows | M02 validator / maintainer | â€” | binary/API and lock/SBOM reports | â€” | 24 Java tests on five JDKs, 13 governance tests, zero static findings, enforced coverage; mutation later | quality gates + ADR-0009 + M02 implementation; M01/M02/M24 |
| ZBW-READY-018 | visibility policy/tombstone/consent migration | privacy visibility defaults | privacy visibility/export / `.privacy.*` | player privacy + staff restricted views | PrivacyPreference/events | consented public fields only | permission/privacy E2E/ST | privacy policy; M03/M15/M17/M18 |
| ZBW-READY-019 | M1 build/CI rights classifier and proprietary product-release posture enforced; executed public terms remain release evidence | licensing/acquisition policy | dependency/M1 validators / maintainer | â€” | build rights metadata; SDK metadata M23 | â€” | artifact/notice/binary/legal-policy checklist | licensing recommendation + ADR-0016 + implementation guide; M01/M23/M24 |
| ZBW-READY-020 | addon catalogue validator plus zero-asset/binary provenance gates retain all 49 references and 473 clean-room rows | addon/asset provenance manifests | M1 validator / maintainer | generated addon report | addon metadata report | 49/473 coverage counts | exact addon/provenance/legal CI | addon catalogue + ADR-0013 + implementation guide; M01/M24 |

## M02 foundational allocation for continuing requirements

These rows record only the contracts delivered by M02. They do not mark later configuration, GUI, command, permission, placeholder, content-production or runtime behavior complete.

| Requirement | M02 implementation | Configuration / GUI / commands / permissions / placeholders | Tests and evidence | Remaining owner |
|---|---|---|---|---|
| ZBW-CONTENT-001..011 | Stable content/pack/resource/definition IDs, generic registry/provider/version metadata and immutable assembly policy support every listed content family without embedding assets or balances | All runtime content files and product surfaces remain their assigned M03/M06/M10â€“M14/M20 work | Registry duplicate/order/version/immutability and zero-product-bundle gates | Per-family milestones and M24 provenance/release gate |
| ZBW-DISCORD-003..004 | Capability IDs and provider/event/query envelope contracts can represent webhook and external-bot providers | Provider config, webhook/bot GUI/commands/permissions/link placeholders remain M16 | Capability/sensitivity/idempotency/provider contract tests | M16 |
| ZBW-DISCORD-006..008 | Optional provider lifecycle, typed failure/delivery outcomes and prohibited sensitivity create the no-required-bot/failure boundary | Disabled provider runtime is M03/M16; queues/circuits M05/M16; secret references M03 | No Discord dependency, null/malformed/capability and architecture tests | M03/M05/M16 |
| M02 / ZBW-ADDON-464 | `ResourceScarcity.definition()` reserves original stable eleventh-modifier identity and custom-resource capability | Host surfaces remain M20 | Stable ID and capability unit test | M20 |
| M02 / ZBW-ADDON-465 | `ResourceGenerationProfile` accepts an independent iron `ResourceId` multiplier | Runtime config/GUI/command/permission/placeholder remain M03/M20 | Profile lookup/serialization/bounds tests | M03/M20 |
| M02 / ZBW-ADDON-466 | The same typed profile independently represents gold | Runtime surfaces remain M03/M20 | Native/custom independent-resource contract tests | M03/M20 |
| M02 / ZBW-ADDON-467 | The same typed profile independently represents diamond | Runtime surfaces remain M03/M20 | Native/custom independent-resource contract tests | M03/M20 |
| M02 / ZBW-ADDON-468 | The same typed profile independently represents emerald | Runtime surfaces remain M03/M20 | Native/custom independent-resource contract tests | M03/M20 |
| M02 / ZBW-ADDON-469 | Any namespaced custom `ResourceId` is supported identically | Runtime custom-generator application remains M20 | Custom-resource lookup/duplicate tests | M20 |
| M02 / ZBW-ADDON-470 | Stable `SCARCE`, `REDUCED`, `NORMAL`, `ABUNDANT`, `EXTREME` preset identities are exposed; numeric versioned balance is deliberately not hard-coded | Preset values/config/editor remain M03/M20 | Exact five-value and stable-ID tests | M03/M20 |
| M02 / ZBW-ADDON-471 | Immutable preset/custom `ResourceScarcity.Settings` contract is ready for the host editor | GUI, authorization, preview/reset/lock feedback remain M20 | Preset/custom settings tests | M20 |
| M02 / ZBW-ADDON-472 | Modifier/resource IDs and public extension/event primitives provide the API type foundation only | Config, commands, granular permission, applied events and PlaceholderAPI remain M20 | Binary/API and identity tests | M20 |
| M02 / ZBW-ADDON-473 | Generator definition, typed multiplier and native/custom profile contracts define deterministic input | Scheduler, mutation policy, item-loss and E2E runtime remain M20 | Generator/profile invariant tests | M20 |

## M03 foundational allocation for continuing requirements

These rows close only the M03 schema/configuration allocation. They preserve all later runtime, content, command, GUI, permission-integration, PlaceholderAPI and compatibility owners.

| Requirement | M03 implementation | Tests and evidence | Remaining owner |
|---|---|---|---|
| M03 / ZBW-CONTENT-001/011 | Approved-provenance and proprietary-copy gates are typed options; no asset or content pack is bundled | schema/default/security/reference and provenance validators | M14/M24 product catalogue and release evidence |
| M03 / ZBW-CONTENT-002..009 | Stable configurable starter profile IDs for shop, modes, quests, achievements, battle pass and cosmetics; 300-cosmetic gate; private/effect compatibility metadata | identifier/type/default/range/reference tests | M06/M10â€“M14/M20 definitions and runtime |
| M03 / ZBW-CONTENT-010 | Versioned `content.yml` schema, strict validation and migration/reload foundations complement the M02 registry contracts | schema/migration/reload/API compatibility tests | family loaders and content runtime in owning milestones |
| M03 / ZBW-COMPAT-001..009 | `compatibility.yml` declares safe fallback policy and no-unsupported-exposure posture without claiming an adapter | exact schema/metadata/architecture tests; five-JDK build matrix | M06/M22 adapters and runtime certification |
| M03 / ZBW-READY-003 | `cosmetics.yml` requires at least 300 original/licensed production definitions and supports custom definitions/rarities | 300 lower-bound, provenance and schema tests | M14 catalogue/content/effects |
| M03 / ZBW-READY-004 | scripting remains disabled by default and requires explicit capability authorization in security metadata | default/security/dependency tests | M05/M11 sandbox workers, review and runtime |
| M03 / ZBW-READY-010/011 | replay/Atlas privacy and retention defaults are typed declarations with strict duration validation | privacy/retention/default/reference tests | M04/M17/M18 persistence, holds, scheduler and encryption |
| M03 / ZBW-READY-013/014 | authenticated-network/authority modes and secret references are schema-validated; no network/storage implementation exists | cross-document secret/topology/config tests | M04/M19/M20 durable authority and messaging |
| M03 / ZBW-READY-015 | immutable starter balance profile IDs are selected through typed schemas | profile-ID and malformed-input tests | M10/M11 registry, activation and simulations |
| M03 / ZBW-READY-018 | private visibility and explicit-consent defaults are typed in statistics/privacy configuration | enum/default/schema tests | M15/M17/M18 runtime policy and migration |
| M03 / ZBW-ADDON-464 | M03 `modes.yml` declares the original RESOURCE SCARCITY modifier configuration family | exact schema/reference tests | M20 host/runtime surface |
| M03 / ZBW-ADDON-465..468 | Scarce/Reduced/Normal/Abundant/Extreme independently configure iron, gold, diamond and emerald | exact preset-value and independent-key tests | M20 generator application |
| M03 / ZBW-ADDON-469 | Every preset has custom-default plus namespaced per-resource overrides; `generators.yml` enables custom resources | parser/duplicate/bounds/custom-resource tests | M20 custom-generator application |
| M03 / ZBW-ADDON-470 | Preset values are versioned config: 0.50, 0.75, 1.00, 1.50 and 2.50 with global 0.10..5.00 bounds | exact five-preset/default/range tests | M20 preview/selection/runtime |
| M03 / ZBW-ADDON-471 | Change policy supports `countdown-locked` and capability-gated `dynamic-rate-safe`; management node is canonical | enum/permission identity/reference tests | M20 GUI lock/reset/preview/feedback |
| M03 / ZBW-ADDON-472 | `zartrabedwars.private.resource-scarcity.manage` and public config/auth types provide the M03 surface foundation | authorization, binary/API and metadata tests | M20 commands/GUI/events/PlaceholderAPI |
| M03 / ZBW-ADDON-473 | Generator multiplier bounds and all native/custom resource inputs are strictly validated before later application | malformed/bounds/dependency/rollback tests | M20 scheduler, item-loss and gameplay E2E |

## Coverage controls

The following checks are required from M01 onward:

1. Extract the 199 Part I IDs from this file/PRD and the append-only `ZBW-ADDON-001..473` Part III IDs from the addon catalogue; fail on a missing, duplicate or extra semantic ID. The generated catalogue may display an append-only amendment beside its owning addon, but numeric allocation history remains immutable and validator-sorted.
2. Fail when an implementation milestone closes while its rows still contain only generic planned locations.
3. Generate command, permission, GUI, config, API/event and placeholder inventories and compare them with the exact references derived from this matrix.
4. Require test and documentation links for every final compliance row.
5. Regenerate Part III with `tools/coverage/generate_addon_feature_catalog.py`; fail unless all 49 references, 8/41 tier split, 473 atomic IDs, required mapping surfaces and `COVERED` statuses match.
6. Run `tools/coverage/validate_preimplementation_decisions.py`; fail unless all 55 Part I decision IDs, `ZBW-ADDON-464..473`, sixteen accepted ADRs, required documents/fields and resolved RC-003/004/017/018/021/022/024/027/029/040/041/043/046/050/059/061/062/065/066/071/072..076 rows match.
7. Regenerate Part II and the combined report with `tools/coverage/generate_master_prompt_coverage.py`; fail on a source SHA-256 or line-count mismatch, an unmapped source assertion, unknown semantic ID, absent requested category, any `PARTIALLY COVERED`/`MISSING` row, or total coverage below 100%.
8. Treat owner-supplied addon and decision inventories as normative supplements while preserving the factual distinction that they were not present in the original Master Prompt baseline.

## Part II â€” Atomic source-to-PRD coverage matrix

`docs/MASTER_PROMPT_COVERAGE.md` is incorporated here by reference as Part II. Each row retains the original source assertion and maps it to one or more Part I IDs and PRD sections. The generated report is the authoritative detailed-child check required by ZBW-GOV-011 and ZBW-QA-007.

## Part III â€” Native addon atomic feature matrix

`docs/ADDON_FEATURE_CATALOG.md` is incorporated here by reference as Part III. Its 473 `ZBW-ADDON-*` rows are semantic requirements, not informative examples. Every row maps its atomic capability to PRD Â§4.17/Â§8.9, the retained Part I overlap, milestone, module, configuration, GUI, commands, permissions, API/events, PlaceholderAPI, performance, security, tests and documentation. The catalogue's `Trace entry` cell is the canonical matrix location for that ID. All 49 addon references remain explicit even where their behavior overlaps a core requirement.

## Source-to-PRD coverage audit

This table is the human-readable range summary of the full beginning-to-end review of `MASTER_PROMPT.md`; it does not replace Part II's atomic rows. Line ranges use the baseline file read for M00; if the source changes, regenerate both audits before implementation.

| Master lines | Source themes | Consolidated requirement IDs |
|---|---|---|
| 1â€“155 | Product vision, originality, enterprise quality, PRD contract, measurable/unambiguous requirements, no placeholders, consistency and non-reduction | GOV-001..010, OPS-009, QA-006 |
| 156â€“409 | Engineering principles/modules/dependencies, threading/performance, DB/config/docs/commands/permissions/GUI/tests/integrations/map identity/failure/Definition of Done | ARC-001..009, DEPLOY-007, OPS-001..006, UX-001..005, QA-001, ARENA-002/003 |
| 410â€“641 | Milestones/order/repository/branching, self-review/correction, traceability, autonomous improvements, reviews/release/final statuses | GOV-003/007/008/010, QA-001/004/005/006, OPS-008/009 |
| 642â€“926 | Core gameplay, all modes, arena/map/duplicate/world/lobby/selectors/hotbars/setup/validator | GAME-001..010, ARENA-001..009, UX-001/003/004 |
| 927â€“1196 | Shop catalog/config/GUI/API/currencies/purchases, upgrades, generators, utility/custom items/shopkeepers/performance | SHOP-001..007, INT-006, UX-001 |
| 1197â€“1582 | Unified progression, XP/levels/prestige/currency, cosmetics categories/definitions/rarities/ownership/equipment/GUI/performance/API | PROG-001..008, PROG-014 |
| 1583â€“2337 | Quests/objectives/progress/rewards/admin/API, achievements, challenges, battle pass, reward engine/summary/calendar/profile/settings/storage/placeholders/commands/permissions/editors/migration/performance/security | PROG-009..014, DEPLOY-007, PAPI-003, ECO-001, QA-001 |
| 2338â€“2901 | Replay identity/capture/accuracy/viewer/speeds/timeline/telemetry/GUI/access/staff/annotations/storage/retention/recovery/performance/privacy/commands/permissions/API | REPLAY-001..010, OPS-002/005/006 |
| 2902â€“3588 | Atlas eligibility/permissions/cases/anonymization/review/verdict/reasons/profile/reputation/accuracy/anti-abuse/interactions/rewards/GUI/commands/staff/punishment/API/placeholders/performance and Grim/Vulcan alert integration | ATLAS-001..013, INT-008, PAPI-004, PROG-011 |
| 3589â€“4266 | Authoritative stats/dimensions/ratios/streaks/storage/admin/GUI/API, leaderboards, complete PlaceholderAPI families/context/performance/format/admin/dev API, Discord/external API/privacy/migration/testing | STATS-001..008, PAPI-001..006, OPS-007, ECO-001, QA-003 |
| 4267â€“4755 | Shared/proxy modes, Velocity/Bungee/CloudNet/Redis/DB, Placeholder/Vault/LuckPerms/packet/world/NPC/hologram/party/anticheat/Via/Geyser/Floodgate | DEPLOY-001..009, INT-001..010, ARC-007, UX-006 |
| 4756â€“5279 | Global/player/admin GUI and editor, command/player/staff/admin inventories, permission actions/Atlas nodes, public/event APIs, configuration files/comments/validation/reload, localization | UX-001..006, OPS-001..004, ARC-003/004, ATLAS-011 |
| 5280â€“5592 | Documentation/reference deliverables, automated/gameplay/performance tests, CI/build artifacts, installation validator/Doctor, delivery and final compliance | OPS-003/006/008/009, QA-001..006, GOV-007/009 |
| 5593â€“6276 | Governance/ownership/ID/status/traceability/change/dependencies/risk/phases/debt/backward compatibility/versioning/review/health, deliverables, compatibility/performance/security reports, ecosystem/marketplace/SDK/migration/AI evolution | GOV-001..010, ARC-008/010, OPS-006/008/009, QA-005/006, ECO-001..005 |
| 6277â€“6470 | Highest-priority priority taxonomy, dependency declarations, complete DoD, UX/accessibility, ADRs, observability, secret management, no duplication, failure handling and maintainability | all IDs via Â§2â€“Â§3 profiles; specifically GOV-004..007, ARC-001/005, UX-001/006, OPS-002/005/006, QA-006 |

## M04 foundational allocation for continuing requirements

These rows close only generic durable-storage foundations. Feature schemas, serializers, commands, GUIs, permissions, APIs/events, placeholders and gameplay/runtime behavior remain with their named later milestones.

| Requirement | M04 implementation | Configuration/surfaces | Tests and evidence | Remaining owner |
|---|---|---|---|---|
| M04 / ZBW-PROG-001..014 | typed record key/revision/payload, atomic UoW, outbox/idempotency, retention/tombstone and recovery ports can persist future progression aggregates without defining them | M03 database/security settings; no progression GUI/command/API/placeholder added | primitive/CRUD/conflict/rollback/outbox/privacy suites pass on SQLite plus certified MySQL/MariaDB foundation contracts | M12/M13 feature model, rewards, migration and surfaces |
| M04 / ZBW-STATS-001..008 | authoritative versioned aggregate envelope and transaction/idempotency foundation only | M03 statistics/database settings; no stats query/leaderboard surface | record revision, transaction, restart and dedupe tests | M15 dimensions, aggregation, leaderboards, privacy and migration |
| M04 / ZBW-REPLAY-001..010 | metadata envelope, recovery objective, retention/hold/tombstone and backup evidence foundation; no replay content is stored | M03 replay/privacy settings; no replay GUI/command/API/placeholder | checksum/recovery/privacy/cache invalidation foundations plus certified encrypted backup/restore contract on both external engines | M17 capture, chunks, viewer, encryption, quota and access |
| M04 / ZBW-ATLAS-001..013 | case-key-capable durable envelope, legal hold/release and identity-separated generic schema only | M03 Atlas/privacy settings; no Atlas workflow/surface | hold/release/duplicate/tombstone persistence passes across certified MySQL/MariaDB engine restart | M18 cases, review, anti-abuse, reputation and identity vault |
| M04 / ZBW-READY-011 | retention policy, retention rows, authorized hold/release and content-free tombstones | retention classes from M03; commands/GUI remain later | duplicate-safe atomic SQLite suite plus certified MySQL/MariaDB restart persistence | M17/M18 scheduler, encryption, deletion/export workflows |
| M04 / ZBW-READY-014 | SQL authority, SQLite topology guard, atomic outbox/inbox, bounded retries and sanitized pool health | M03 deployment/database/redis schema | crash/retry/idempotency/concurrency/migration tests pass on both certified external engines; seven plans/engine and Hikari evidence attached to run `29406777872` | M19/M20 leases, Redis/proxy partitions and runtime health |
| M04 / ZBW-READY-016 | encrypted validated backup-evidence coordinator and SQL zero-RPO/15-minute-RTO declarations | operational defaults; secrets supplied through provider driver | provider failure evidence plus encrypted/checksummed validated restore contracts pass on MySQL and MariaDB | M05/M24 quotas, degradation and production RPO/RTO drill evidence |

## M06 foundational allocation for continuing requirements

These rows record the verified M06 foundation without marking any M22 or later acceptance complete. Exact implementation and certification evidence is in `docs/IMPLEMENTATION_M06.md` and `build/evidence/m06-paper-primary.json`.

| Requirement | M06 foundation | Tests and evidence assigned to M06 | Remaining owner |
|---|---|---|---|
| M06 / ZBW-ARC-002/007 | VERIFIED Java-8 compatibility/world contracts and Java-21 primary adapter/bootstrap dependency boundary | graph/order/cycle, class-major 52/65, binary API and forbidden-import checks pass | Provider families and final architecture evidence in owning milestones/M24 |
| M06 / ZBW-ARENA-005/006 | VERIFIED neutral world SPI/reset orchestration and native Paper 1.21.1 provider | load/clone/reset/unload E2E passes; owner-thread/filesystem split, rollback, cancellation, timeout, drain and leak CT pass | Arena integration M07; optional provider adapters M21 |
| M06 / ZBW-INT-004/005 | VERIFIED primary-safe packet/UI capability boundary and native world provider only | primary capability and provider contracts; provided-API/no-platform-bundle checks pass | ProtocolLib/full packet matrix M22; optional world providers M21 |
| M06 / ZBW-INT-010 | VERIFIED neutral compatibility API and `zbw-compat-v1_20-v1_21` behavior required only by Paper 1.21.1 foundation certification | primary adapter/bootstrap/capability CT and exact five-operation E2E pass | Legacy/intermediate adapters, remaining runtime rows, Via and Bedrock M22 |
| M06 / ZBW-COMPAT-001..009 | VERIFIED semantic capability/fallback contracts, 19 primary mappings, typed degradation/LKG policy and primary evidence schema | mapping completeness, duplicate/malformed, supported/unsupported/fallback/degraded and no-platform-leak tests pass | `zbw-compat-v1_8`, all legacy fallback implementations and complete feature matrix M22 |
| M06 / ZBW-CONTENT-009 | VERIFIED semantic sound/visual-effect identifiers and primary mappings | primary mapping/config validation CT passes | Starter catalogue M14; mandatory 1.8 mapping/usability M22 |
| M06 / ZBW-READY-001/002/006 | VERIFIED approved Java-8 neutral and Java-21 primary artifacts consuming only locked Paper 1.21.1 build 133 | five-JDK matrix, class-major, exact-fixture and primary bootstrap/world evidence pass with no broader claim | All other runtime artifacts and full release certification M22 |
| M06 / ZBW-READY-003 | VERIFIED compatibility-renderer contract foundation only; no cosmetic definition or runtime effect delivered | semantic renderer capability CT passes | 300 definitions/effects M14; legacy rendering M22; release quality M24 |

## M07/M09 ArenaSetup allocation for continuing requirements

These rows preserve every ArenaSetup capability while separating presentation-neutral behavior from its final command/GUI adapters. M07 verification may close only the core/application column. M09 owns every mapped command, GUI, editor and common confirmation-token surface; M16 retains PlaceholderAPI and M22 retains full compatibility certification.

| Requirement | M07 core/application ownership | M09 presentation ownership | Later owner |
|---|---|---|---|
| M07/M09 / ZBW-ADDON-408 | Isolated setup-session enter/exit operations, state transitions, authorization intents and events | Canonical `/zbw setup <arena>` entry/exit command, help, completion and feedback | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-409 | Setup-tool definitions, action identities, slot policy and localized message keys | Command/GUI editor and Paper item presentation through the unified UI contracts | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-410 | Waiting-lobby spawn capture, validation and draft mutation use case | Setup command and wizard step | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-411 | Team create/edit/spawn policies plus M08.1 arena-derived runtime identity/display/color/capacity hardening | Team setup commands and editor pages | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-412 | Team-bed location/facing capture and validation | Bed setup commands and editor pages | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-413 | Team resource-generator capture and validation | Generator setup commands and editor pages | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-414 | Team shop-NPC location/orientation/remove use cases | NPC setup commands and editor pages | M16 placeholders; M21 provider adapters; M22 compatibility |
| M07/M09 / ZBW-ADDON-415 | Team-upgrade-NPC location/orientation/remove use cases | Upgrade-NPC setup commands and editor pages | M16 placeholders; M21 provider adapters; M22 compatibility |
| M07/M09 / ZBW-ADDON-416 | Diamond-generator location and validation use cases | Generator setup commands and editor pages | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-417 | Emerald-generator location and validation use cases | Generator setup commands and editor pages | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-418 | Marker discovery, immutable preview and explicit apply requirement bound to draft revision | Unified confirmation token, discovery/import command and confirmation GUI | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-419 | Arena mode/group/team/bounds/world-adapter policies; M08.1 shared team/capacity authority and typed default profile | Property commands and arena-property editor | M16 placeholders; M21 provider adapters; M22 compatibility |
| M07/M09 / ZBW-ADDON-420 | Presentation-neutral team/generator/NPC/property editor operations and validation contracts | Setup GUI pages, navigation, state retention, loading/error handling and editor framework | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-421 | Completeness/collision/spawn/region rules plus M08.1 exact typed standard/custom generator prerequisite profiles | Validate command, validator GUI and actionable localized rendering | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-422 | Draft preview, history, undo/redo, revision-fenced apply, atomic save and last-known-good rollback | Editor interactions and common confirmation-token flow | M16 placeholders; M22 compatibility |
| M07/M09 / ZBW-ADDON-423 | Typed setup/query APIs, permission enforcement and cancellable/immutable events | Granular setup commands, GUI action bindings and presentation audit context | M16 authorized-staff placeholders; M22 compatibility |

## M07 verified arena, map and setup application allocation

The following rows close only the presentation-neutral M07 allocation. They do
not mark the M09 command/GUI cells above, M16 placeholders, M21 optional world
providers or M22 compatibility work complete. Evidence is
`docs/IMPLEMENTATION_M07.md`, the M07 API/lifecycle/validation/archive
references, Maven reports and `build/evidence/m07-paper-primary.json`.

| Requirement | Verified M07 implementation | Verification | Remaining owner |
|---|---|---|---|
| M07 / ZBW-ARENA-001 | VERIFIED authorized arena CRUD/list/lifecycle, immutable repositories, typed failures/events and audit | lifecycle, authorization, revision, cancellation and SQLite transaction tests | M09 presentation; M16 fields; M22 compatibility |
| M07 / ZBW-ARENA-002 | VERIFIED immutable map aggregate and rename/reference preservation | equality, rename and reference regression tests | M09 map surfaces; M16 fields |
| M07 / ZBW-ARENA-003 | VERIFIED typed ID allocation, collision rejection and import identity policy | duplicate-ID, malformed archive and import collision tests | M09 import/duplicate surfaces; migration completion where later assigned |
| M07 / ZBW-ARENA-004 | VERIFIED deep arena/map duplication with new IDs and independently owned values | deep-copy, mapping, rollback and exact Paper tests | M09 duplicate flow |
| M07 / ZBW-ARENA-005 | VERIFIED M07 consumption of M06 async world contracts for arena lifecycle | five-operation exact Paper E2E and worker/owner affinity evidence | M09 surfaces; M21 optional providers |
| M07 / ZBW-ARENA-006 | VERIFIED bounded arena reset/recovery admission, typed failure and diagnostics | concurrency, cancellation, failure and leak-free Paper tests | M09 progress surface; M21/M22 completion |
| M07 / ZBW-ARENA-007 | VERIFIED setup sessions, all typed steps, history, preview/apply, undo/redo and atomic commit | deterministic full workflow, stale preview, rollback, SQLite and Paper tests | M09 wizard/editor; M16 fields; M22 compatibility |
| M07 / ZBW-ARENA-008 | VERIFIED complete validation report and block-enable policy | valid/invalid category, collision, reference, import/restore and enable-gate tests | M09 validator rendering; M16 fields |
| M07 / ZBW-ARENA-009 | VERIFIED presentation-neutral administration authorization, two-phase mutations and audit facts | authorization, cancellation, stale revision and audit tests | M09 commands/GUI/common confirmation |
| ZBW-ADDON-408..423 | VERIFIED all sixteen atomic ArenaSetup core/application rows without merging or weakening any catalogue row | 37 M07 tests, exact Paper E2E, catalogue/allocation validators and API signature baseline | Every command/GUI/editor cell remains M09; later owners remain as listed above |

## M08/M09 game and addon allocation for continuing requirements

These rows preserve every atomic M08 addon capability while separating Java-8 engine/application behavior, closed Paper 1.21.1 projections and final reusable presentation. M08 may close only the first two columns. M09 owns every mapped production command, GUI, editor and common confirmation flow; M16 retains PlaceholderAPI, M20 proxy delivery, M21 the NPC/hologram portions of `ZBW-GAME-006`, and M22 full compatibility. Feature actions whose owning systems arrive after M08 remain with those milestones. No M08 module depends on an M09-or-later module.

M08 core/application and closed primary-Paper cells for all 61 rows below are VERIFIED by
`game/zbw-game`, `platform/paper/zbw-paper-modern`, 41 zero-skip unit/contract tests and
`build/evidence/m08-paper-primary.json`. Their M09/M10/M16/M20/M21/M22 cells remain open
exactly as recorded; this status does not close or weaken any continuing requirement.

| Requirement | M08 core/application ownership | M08 primary Paper 1.21.1 projection | M09 final presentation ownership | Later owner |
|---|---|---|---|---|
| M08/M09 / ZBW-ADDON-001 | State-specific hotbar loadout model and selection policy | Apply the selected loadout on state change | Player/admin configuration, editor and preview surfaces | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-002 | Typed slot/item definition and validation | Version-safe item materialization in owned hotbar slots | Slot/icon/name/lore/glint editor | M22 complete mappings |
| M08/M09 / ZBW-ADDON-003 | Stable action identities, eligibility intent and dispatcher contract | Translate owned item interaction into a typed action intent | Action selector and command/GUI navigation | M10/M11/M17/M20 action providers; M22 compatibility |
| M08/M09 / ZBW-ADDON-004 | Permission/state/mode/arena/player visibility and availability policy | Apply filtered slots and reject stale interaction | Condition/permission editor and diagnostics | M22 compatibility |
| M08/M09 / ZBW-ADDON-005 | Deterministic global/group/mode/arena precedence | Rebuild from the resolved immutable definition | Override editor, preview and conflict reporting | M22 compatibility |
| M08/M09 / ZBW-ADDON-006 | Feature-specific move/action/validate/preview use cases and snapshots | Apply a validated preview only through the closed projector | Paginated editor, navigation, action picker and confirmation | M22 compatibility |
| M08/M09 / ZBW-ADDON-007 | Atomic validation, reload plan and last-known-good policy | Rebuild players only after successful publication | Reload/validate commands and result GUI | M22 compatibility |
| M08/M09 / ZBW-ADDON-008 | Inventory ownership, replacement and idempotent restore policy | Exact hotbar replace/restore effects with no contamination | Inspect/repair feedback surfaces | M22 compatibility |
| M08/M09 / ZBW-ADDON-009 | Hotbar query/rebuild API and lifecycle events | Publish selected-action and rebuild outcomes | Admin inspect/rebuild command and GUI bindings | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-108 | Deposit eligibility and typed item-action/command request use case | Translate configured item action and apply accepted request | `/deposit` and admin command/GUI entry points | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-109 | Atomic held-stack deposit policy | Owner-thread inventory/Ender Chest transfer | Result and inspection presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-110 | Resource selection and explicit quantity request validation | Apply accepted bounded quantity transfer | Quantity command/selector/editor presentation | M11 resource definitions; M22 compatibility |
| M08/M09 / ZBW-ADDON-111 | Allow/deny, state, cooldown and per-match-limit policy | Read owned inventory snapshot and apply approved transfer | Policy editor, cooldown/limit diagnostics | M11 resource definitions; M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-112 | Capacity quote and all-or-accounted partial/overflow outcome | Apply one atomic inventory mutation | Localized capacity/overflow result rendering | M22 compatibility |
| M08/M09 / ZBW-ADDON-113 | Private ownership and protected/synthetic-item rejection | Enforce owner inventory boundaries while applying outcome | Permission-aware inspection feedback | M22 compatibility |
| M08/M09 / ZBW-ADDON-114 | Localized result facts, audit facts, API events and diagnostics use cases | Direct localized result feedback | Admin reload/inspect/configuration commands and GUI | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-124 | Threshold-crossing trigger and exactly-once publish intent | Deliver the local primary announcement | Admin configuration/preview/reload surfaces | M20 distributed delivery; M22 compatibility |
| M08/M09 / ZBW-ADDON-125 | Immutable localized arena/mode/group/count/capacity/countdown payload | Render the resolved local message | Template editor and preview | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-126 | Eligibility/reservation-checked join intent | Render the supported primary interactive affordance without a temporary command | Final clickable command/GUI action binding | M20 remote reservation; M22 fallback |
| M08/M09 / ZBW-ADDON-127 | Local/server-group/permission audience policy and proxy delivery intent | Deliver only local/primary audiences | Audience configuration and diagnostics | M20 proxy-network delivery; M22 compatibility |
| M08/M09 / ZBW-ADDON-128 | Per-arena dedupe, cooldown and regression-cancellation policy | Remove/cancel stale local presentation | Inspect/reset diagnostics | M22 compatibility |
| M08/M09 / ZBW-ADDON-129 | Sound/title/action-bar/text semantic feedback intent | Render native primary feedback channels | Preview/configuration surfaces | M22 text/effect fallbacks |
| M08/M09 / ZBW-ADDON-130 | Publish/join-click events, preview model and diagnostic facts | Test-only/authorized closed preview projection | Admin preview/reload/diagnostic commands and GUI | M16 placeholders; M20 distributed diagnostics; M22 compatibility |
| M08/M09 / ZBW-ADDON-148 | Arena-owned item/loss-boundary detection policy | Translate item movement/removal observations | Admin boundary/configuration presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-149 | Exactly-once capture identity and fence | Capture the accepted entity stack before removal | Capture inspection feedback | M22 compatibility |
| M08/M09 / ZBW-ADDON-150 | Owner/killer/team/nearest recipient resolution | Resolve live primary player handles only after policy decision | Recipient-policy editor and diagnostics | M22 compatibility |
| M08/M09 / ZBW-ADDON-151 | Item/resource/source/state/age/ownership filters | Translate version-safe item metadata into neutral snapshot | Filter editor and validation report | M11 resource/item definitions; M22 compatibility |
| M08/M09 / ZBW-ADDON-152 | Atomic delivery, overflow and recoverable-pending-grant outcome | Apply accepted delivery/overflow effects | Pending/overflow diagnostic presentation | M12 grant integration; M22 compatibility |
| M08/M09 / ZBW-ADDON-153 | Death/disconnect/pickup/reset race resolution and idempotency | Owner-thread compare/apply/cleanup | Race/outcome inspection surface | M22 compatibility |
| M08/M09 / ZBW-ADDON-154 | Recovery feedback facts, inspect API and capture events | Direct localized recovery feedback | Admin inspect/configuration commands and GUI | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-334 | Leave-delay state machine and eligible-state policy | Start owner-thread feedback cadence | Player/admin leave command entry points | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-335 | Remaining-time projection model and message keys | Direct chat/title/action-bar/boss-bar feedback | Feedback-channel editor and preview | M22 compatibility |
| M08/M09 / ZBW-ADDON-336 | Movement/damage/combat/death/state/command cancellation policy | Translate primary Paper cancellation signals | Command feedback and policy editor | M22 compatibility |
| M08/M09 / ZBW-ADDON-337 | Per-state delay/cancellation configuration policy | Apply resolved state policy | Admin configuration/validation presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-338 | Bypass authorization and audited immediate-leave use case | Apply authorized immediate exit | Staff command/confirmation and audit presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-339 | Exactly-once leave completion and session restore | Restore inventory/location/session and clear feedback | Completion/error surface | M22 compatibility |
| M08/M09 / ZBW-ADDON-340 | Delay query, cancellation/completion API and events | Publish remaining/terminal projection updates | Admin diagnostics and player command binding | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-398 | Deterministic team-order policy | Apply primary tab ordering | Admin order editor/preview | M22 compatibility |
| M08/M09 / ZBW-ADDON-399 | Deterministic within-team rank/name comparator | Apply stable primary order | Comparator configuration/preview | M21 rank-meta provider; M22 compatibility |
| M08/M09 / ZBW-ADDON-400 | Explicit spectator/staff/lobby section policy | Apply primary tab sections | Section editor and preview | M10 spectator policies; M22 compatibility |
| M08/M09 / ZBW-ADDON-401 | Privacy-safe tab-entry field snapshot | Render allowed primary fields | Field editor/preview/diagnostics | M15 statistics; M21 metadata; M22 compatibility |
| M08/M09 / ZBW-ADDON-402 | Localized header template snapshot | Render primary tab header | Header editor and preview | M22 compatibility |
| M08/M09 / ZBW-ADDON-403 | Localized footer template snapshot | Render primary tab footer | Footer editor and preview | M22 compatibility |
| M08/M09 / ZBW-ADDON-404 | Timeout/fallback-safe placeholder value input contract with no remote owner-thread query | Render only resolved/cached values | Placeholder field configuration/diagnostics | M16 PlaceholderAPI expansion; M22 compatibility |
| M08/M09 / ZBW-ADDON-405 | Diff and bounded refresh-cadence policy | Update changed primary entries within packet budget | Refresh diagnostics/editor | M22 complete packet matrix |
| M08/M09 / ZBW-ADDON-406 | Clear/restore and scoreboard-owner arbitration policy | Clean stale tab state on exit/reset | Ownership-conflict diagnostics | M22 packet/scoreboard compatibility |
| M08/M09 / ZBW-ADDON-407 | Tab render API/events and diagnostic facts | Authorized closed primary preview/update | Admin reload/preview/diagnostics commands and GUI | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-424 | Waiting/countdown boss-bar state/progress snapshot | Render native primary waiting bar | Admin editor/preview | M22 legacy fallback |
| M08/M09 / ZBW-ADDON-425 | Playing event/bed/team/mode progress snapshot | Render native primary playing bar | Admin editor/preview | M10 mode events; M22 legacy fallback |
| M08/M09 / ZBW-ADDON-426 | Winner/return/requeue post-game snapshot | Render native primary post-game bar | Admin editor/preview | M10/M20 requeue integration; M22 legacy fallback |
| M08/M09 / ZBW-ADDON-427 | Atomic bar transition/viewer ownership policy | Replace/clear native bars without stale viewers | Transition diagnostics | M22 legacy cleanup equivalence |
| M08/M09 / ZBW-ADDON-428 | Template/colour/style/progress/cadence resolution | Apply resolved primary bar attributes | Configuration editor and preview | M22 compatibility |
| M08/M09 / ZBW-ADDON-429 | Per-player visibility/locale/vanish/private-field policy | Filter viewers and render localized primary bar | Player/admin settings presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-430 | Native-versus-fallback semantic capability request | Use only native modern primary boss bars | Compatibility diagnostics | M22 implements/certifies legacy fallback |
| M08/M09 / ZBW-ADDON-431 | Boss-bar render API/events and diagnostics facts | Authorized closed primary preview/update | Admin reload/preview/diagnostics commands and GUI | M16 placeholders; M22 compatibility |
| M08/M09 / ZBW-ADDON-432 | Eligible waiting-state AdventureMode transition policy | Set waiting players to adventure mode | Admin configuration/status presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-433 | Active-team survival transition policy | Set accepted active players to survival mode | Admin configuration/status presentation | M22 compatibility |
| M08/M09 / ZBW-ADDON-434 | Eliminated-viewer spectator-compatible mode policy | Apply accepted spectator mode | Admin configuration/status presentation | M10 spectator behavior; M22 compatibility |
| M08/M09 / ZBW-ADDON-435 | Idempotent pre-arena/lobby mode restoration policy | Restore mode on every exit/failure path | Recovery diagnostics | M22 compatibility |
| M08/M09 / ZBW-ADDON-436 | Per-arena/mode/state policy with creative-deny invariant | Apply only accepted non-creative transition | Configuration editor/validation | M22 compatibility |
| M08/M09 / ZBW-ADDON-437 | Reconnect recovery state, transition API and events | Reapply the correct primary mode after reconnect | Admin inspect/recover presentation | M16 placeholders; M22 compatibility |

## M08.1 corrective hardening allocation

M08.1 adds no Requirement ID and changes no final feature owner. It closes the
following cross-cutting defects in the existing M07/M08 foundations. M09 retains every
command/GUI/editor surface and M10 retains modes, matchmaking and selectors.

| Requirement | M08.1 implementation | Configuration | Commands / GUI | API / events | Tests | Documentation / remaining owner |
|---|---|---|---|---|---|---|
| M08.1 / ZBW-GAME-001 | Version-fenced enabled-arena-to-waiting-match assembly; arena-derived identity/display/color/capacity and shared limits | arena definitions plus independent timing policy | None; M09 | `ArenaMatchAssembler`, `MatchAssemblyRequest`, `TeamDefinition` | seven layout assemblies, assignment/admission, stale/disabled/inconsistent and lifecycle recovery | `API_M08_1.md`; M09 presentation |
| M08.1 / ZBW-GAME-002 | Exactly-one-surviving-eligible-team evaluator emits typed repeatable completion intent; existing completion key remains the terminal fence | evaluator selected by composition | None; M09/M10 | `VictoryEvaluator`, `VictoryEvaluation`; fact in existing transition | automatic winner, retry, override, completion/restore/reset | `GAME_ENGINE_M08.md`; M10 mode policy |
| M08.1 / ZBW-GAME-004 | Standard and custom layouts represented by the same data/contracts with no preset branch | arena team list and limits | None; M10 | shared team/victory contracts only | 8x1, 8x2, 4x3, 4x4, 2x4, 12x3, 64x4 | `LAYOUT_COMPATIBILITY_M08_1.md`; M10 completion |
| M08.1 / ZBW-ARENA-002 | Shared authoritative limits and replaceable typed arena defaults | `ArenaDefaultProfile` mapping | None; M09 | `TeamLayoutLimits`, `ArenaDefaultProfile` | boundary, malformed and copied-default tests | `TEAM_CONFIGURATION_M08_1.md`; M09 presentation |
| M08.1 / ZBW-ARENA-008 | Exact typed generator prerequisites and cross-definition group/mode/team-size/capacity validation | `ArenaValidationProfile` mapping | None; M09 | validation profile/report contracts | diamond/emerald exact IDs, substring lookalike, arbitrary custom type and inconsistency tests | `ARENA_VALIDATION_PROFILES_M08_1.md`; M09 rendering |
| M08.1 / ZBW-OPS-001 | Replaces anonymous arena defaults and generator-name heuristics with typed profiles | profile IDs and values | None; M09 | immutable profile APIs | range/null/immutability/equality tests | M03 configuration engine remains authoritative |
| M08.1 / ZBW-QA-001/002 | Deterministic M08.1 suites, binary baseline, strict JavaDoc and architecture/governance runner | locked toolchains | Validation commands only | test/evidence contracts | zero mandatory skips; Java 8/11/16/17/21 and Paper regression | `IMPLEMENTATION_M08_1.md`; later full gameplay matrix unchanged |
| M08.1 / ZBW-ADDON-156 | Neutral model and tests support 12 and up to the shared 64-team bound without an eight-team engine ceiling | arena-defined identities/order | Team-selector GUI remains M10 | `TeamDefinition` list | 12x3 and 64x4 assembly/assignment | M10 selector acceptance unchanged |
| M08.1 / ZBW-ADDON-411/419 | Team create/edit data now reaches runtime without manual metadata reconstruction; defaults and bounds are typed | arena/default profiles | Setup commands/editors remain M09 | assembler/team contracts | all eight standard identities plus custom IDs/labels/colors | M07 core retained; M09 presentation |
| M08.1 / ZBW-ADDON-421 | Standard/custom prerequisite profiles use exact generator type identities | validation profiles | Validate command/GUI remains M09 | `ArenaValidationProfile` | exact/custom/missing prerequisite tests | M09 localized rendering; M22 compatibility |

## M09 verified presentation allocation

| Requirement allocation | Implementation | Configuration | Commands / permissions | GUI | API / events | Tests and evidence | Documentation / remaining ownership |
|---|---|---|---|---|---|---|---|
| ZBW-UX-001/002/003/006 | `CommandModel`, `CommandFramework`, `UiModel`, `UiFramework`, `AdminDashboard` | immutable action/page metadata | 87 generated paths; 87 granular nodes; M03 revalidation | 88 generated pages including dashboard; bounded sessions | Java 8 command/UI extension APIs | 24 M09 tests; exact Paper evidence | `API_M09.md`, framework guides; VERIFIED |
| ZBW-ARENA-001..009 presentation | Shared action registry delegates to typed M07 bindings | M07 schemas unchanged | arena/setup families and confirmation | arena/setup pages, dashboard and editor mechanics | neutral `PresentationActions.UseCase` | complete parity inventory and negative-path tests | M09 presentation VERIFIED; M16/M21/M22 portions unchanged |
| ZBW-GAME-001..003/006/008/010 presentation | Shared action registry delegates to typed M08 bindings | M08 policies unchanged | game lifecycle/health/diagnostic family | equivalent game pages | neutral structured response | parity, permission, error, async and Paper tests | M09 presentation VERIFIED; M10/M16/M20/M22 portions unchanged |
| ZBW-ADDON-001..009/108..114/124..130/148..154/334..340/398..407/424..437 presentation | typed M08 action bindings | configuration editing metadata only | generated addon families and exact permissions | equivalent editor/preview/validate/inspect pages | common use-case binding | inventory completeness and all-action parity | M09 presentation VERIFIED; later milestone cells preserved |
| ZBW-ADDON-408..423 presentation | generic editor/confirmation/dashboard over M07 setup use cases | typed drafts/revision/migration | setup family and destructive confirmation | setup/editor pages with stale protection | editor policy SPI | editor lifecycle/conflict/migration/parity tests | M09 presentation VERIFIED; M07 core retained |

## M10 reconciled allocation for continuing requirements

M10 materializes no new module. Its Java-8-neutral application behavior belongs to `zbw-game` and
uses existing `zbw-arena` and M08 use cases. Command and GUI surfaces extend the existing M09
modules; primary platform translation remains in `zbw-paper-modern`. The following table is the
authoritative ownership split and prevents a framework registration from closing later gameplay.

| Requirement allocation | M10 ownership | Later ownership retained |
|---|---|---|
| M10 / ZBW-GAME-004/005 | Mode identity, immutable registry, compatibility, schema/version validation, enablement, selection and typed deferred bindings | M11 named-mode mechanics and component packs; M15 statistics; M16 placeholders |
| M10 / ZBW-GAME-007 | Shared-server selectors, bounded deterministic matchmaking, party atomicity, revision-bound reservations and M08 assignment delegation | M20 proxy-wide matchmaking/routing and distributed persistence |
| M10 / ZBW-GAME-009 | Spectator lifecycle, admission, navigation, restrictions, restoration, diagnostics and primary Paper projection | M17 staff moderation/evidence extensions; M16 placeholders; M22 full compatibility |
| M10 / ZBW-CONTENT-003 | Versioned mode-profile identity/schema/selection and deferred balance-provider binding | M11 original balance packs/runtime; M16 active-profile placeholders |
| M10 / ZBW-ADDON-092..101 | Spectator Options core, command/GUI and primary Paper behavior | M16 placeholders; M22 full compatibility |
| M10 / ZBW-ADDON-115..123 | Local spectator/play-again menu, selectors and reservation intents | M16 placeholders; M20 remote/proxy routing; M22 full compatibility |
| M10 / ZBW-ADDON-131..140 | Compass tracking and quick-communication core, command/GUI and primary Paper behavior | M16 placeholders; M22 full compatibility |
| M10 / ZBW-ADDON-155..163 | Party-aware Team Selector core, command/GUI and primary Paper behavior | M16 placeholders; M22 full compatibility |
| M10 / ZBW-ADDON-236..244 | Swappage registration, selection metadata and explicit deferred binding only | M11 complete swap gameplay and owned-component transfer; M15 isolated statistics; M16 placeholders; M22 full compatibility |
## M10 implementation evidence overlay

| Allocation | Implementation | Tests and documentation | Deferred completion |
|---|---|---|---|
| `ZBW-GAME-004/005`, `ZBW-CONTENT-003`, `ZBW-ADDON-236..244` registration/selection | `zbw-game` mode/selector packages; additive M09 actions | `ModeSelectorM10Test`, `M10BranchCoverageTest`; `API_M10.md`, `MODE_FRAMEWORK_M10.md`, `SELECTORS_M10.md` | M11 mechanics, M15 statistics, M16 placeholders, M22 compatibility |
| `ZBW-GAME-007` shared server | bounded queues, fair policy, arena availability/reservations and M08 assignment port | `MatchmakingM10Test`, `M10BranchCoverageTest`; matching framework/admin/extension guides | M20 proxy-wide routing and durable distributed recovery |
| `ZBW-GAME-009`, `ZBW-ADDON-092..101/115..123` | spectator lifecycle, restrictions, restoration and owner-thread projection | `SpectatorAddonM10Test`, `M10PaperProjectionTest`; `SPECTATOR_FRAMEWORK_M10.md` | M16 placeholders, M17 evidence tools, M22 compatibility |
| `ZBW-ADDON-131..140/155..163` | Compass communication/tracking and party-safe Team Selector | `SpectatorAddonM10Test`, `M10BranchCoverageTest`; generated inventories | M16 placeholders and M22 compatibility |

## M11 reconciled allocation for continuing requirements

This table is the authoritative ownership overlay for M11. It preserves every Part I and Part III
row while preventing a platform or provider contract from being mistaken for its later concrete
implementation. The M11 governance checkpoint allocates planned modules only; it does not
materialize source modules or activate M11.

| Requirement allocation | M11 ownership | Later ownership retained |
|---|---|---|
| M11 / ZBW-GAME-004/005 | Named-mode mechanics, component packs and match-local shop/generator/upgrade/item bindings installed through M10 deferred bindings; no M08 lifecycle duplication | M15 per-mode statistics; M16 placeholders; M22 full compatibility |
| M11 / ZBW-SHOP-001/002 | Catalog, Quick Buy/favourites/history, feature actions/pages and primary Paper 1.21.1 projection using M09 frameworks | M16 placeholders; M21 NPC/shopkeeper providers; M22 legacy presentation |
| M11 / ZBW-SHOP-003/004 | Atomic quoted purchases, tender SPI and iron/gold/diamond/emerald/custom/multiple match-resource providers | M12 persistent/virtual currency ledger/providers; M16 placeholders; M21 Vault provider; M22 legacy presentation |
| M11 / ZBW-SHOP-005 | Team upgrades, forge, heal pool, dragon buff, queued/multiple traps and custom upgrade behavior | M16 placeholders; M22 legacy presentation |
| M11 / ZBW-SHOP-006 | Generator definitions/runtime, upgrades/split/caps/overflow and display-state intent | M16 placeholders; M21 concrete hologram providers; M22 legacy mappings/fallbacks |
| M11 / ZBW-SHOP-007 and ZBW-READY-004 | Original utility-item/action registry, disabled declarative sandbox and primary Paper mapping | M16 placeholders; M22 legacy item/effect mappings and full certification |
| M11 / ZBW-CONTENT-002/003 and ZBW-READY-015 | Original versioned shop/mode balance packs, validation, preview and golden simulations | M16 active-profile placeholders; M22 compatibility validation |
| M10/M11 / ZBW-ADDON-236..244 | Complete Swappage gameplay, atomic owned-match-state transfer, rejoin/reset behavior and local events; M10 registration/selection remains verified | M15 isolated statistics; M16 placeholders; M22 full compatibility |
| M11 / ZBW-ADDON-010..025, 061..070, 141..147, 184..201, 300..322, 341..349, 363..368, 379..397, 438..452 | Every listed atomic mechanic, configuration, feature-specific M09 presentation, API/event, security/performance rule and primary Paper 1.21.1 behavior | M15 statistics where named; M16 every placeholder cell; M19 Redis coordination and M20 proxy/server synchronization for ZBW-ADDON-387; M21 external provider cells; M22 legacy/full-matrix compatibility |
| M11 / ZBW-ADDON-387 | Local deterministic schedule, durable active-rotation state, transition event and recovery port | M19 cross-node coordination; M20 proxy/server distribution; M16 placeholders; M22 compatibility |

M11 materializes typed `RotationContracts`, local state/events, selection, overrides, durable
recovery, M09 presentation and runtime mechanics for ZBW-ADDON-380..388. M19/M20 retain all
cross-node coordination and distribution.

M11 implementation may emit immutable events and cached state for later statistics, PlaceholderAPI,
Redis, proxy, provider and compatibility consumers. Emitting those contracts is not evidence that a
later consumer or adapter exists.

### M11 final-integration checkpoint evidence

| Allocation | Implemented integration | Verification | Remaining owner |
|---|---|---|---|
| `ZBW-SHOP-001..004` | 25 additive M09 command/GUI parity actions, concrete inventory/configuration/SQL adapters and generated 140-action inventories | acceptance suites; `m11_inventories.py`; Paper certification | persistent currency M12; placeholders M16; Vault M21; legacy M22 |
| `ZBW-SHOP-005..007` | `M11MatchRuntime` consumes M08 snapshots; Java 21 Paper adapters translate committed generator, upgrade and utility intents | lifecycle, owner-thread, API/JavaDoc and Paper gates | placeholders M16; external providers M21; legacy M22 |
| M11 addon allocations | bounded named-mode/addon mechanics remain explicitly mapped; no catalogue row is removed | 49 addon references/473 IDs, dashboard and Paper validation | later cells retain M15/M16/M19/M20/M21/M22 |
| `ZBW-READY-004` | Java-8-neutral, disabled-by-default bounded declarative engine | security tests, API baseline and strict JavaDoc | later extension/provider ownership remains unchanged |

The final-integration checkpoint is additive and preserves every M10 public signature. Together
with the completed M11.1 corrective scope and PR #18 remote certification, it satisfies the M11
exit criteria. It also preserves M12 ownership boundaries while M12 was executed separately for full closure.

### M11.1 Phase 1 corrective evidence

| Allocation | Implemented Phase 1 portion | Evidence / subsequent completion |
|---|---|---|
| `ZBW-READY-004` | materialized disabled-by-default bounded declarative interpreter; action/capability allowlists, scheduler deadlines, cancellation, depth/operation quotas, validation and secret-free audit | `DeclarativeScriptEngineTest`; feature-handler integration completed by the final M11.1 checkpoint |
| `ZBW-READY-015`, `ZBW-CONTENT-002/003` | versioned eight-section M11 configuration snapshots, consecutive migration, deterministic validation/activation, rollback, last-known-good retention and golden mode-balance simulation | `M11RuntimeConfigurationTest`, `ModeBalanceCatalogTest`; named-mode packs and Paper activation completed by M11.1 |
| `ZBW-GAME-004/005`, `ZBW-SHOP-001..007`, M11 addon configuration portions | typed shops/generators/upgrades/traps/items/modes/rotations/scripts configuration families can activate atomically without platform dependencies | mechanics, inventory/Paper adapters and acceptance matrices completed and remotely certified by PR #18 |

## M12 planned ownership overlay

M12 is now materialized and complete; this section is retained for requirement ownership boundaries.

| Requirement allocation | Planned M12 ownership | Ownership explicitly retained |
|---|---|---|
| `ZBW-PROG-001..005`, `ZBW-PROG-011` | exactly-once progression projection, XP, levels, prestige, internal persistent/virtual currencies, immutable transaction ledger and transactional rewards | M15 statistics; M16 placeholders; M17 replay; M18 Atlas; M19/M20 distributed/proxy transport; M21 Vault/NPC/hologram providers; M22 legacy compatibility |
| `ZBW-SHOP-003/004` M12 portion | persistent/virtual tender provider and ledger settlement through the stable M11 quote/tender SPI | M11 catalog, quote, match resources and purchase policy; M21 Vault; M16 placeholders |
| `ZBW-GAME-003` M12 portion | reward/progression consumer of the M08 completion event | M08 lifecycle/completion; M15 statistics; M17 replay |
| `ZBW-ADDON-174..183` | Reward Commands policy, idempotent execution and primary M09 presentation | M16 placeholders; M19/M20 distributed transport; M22 compatibility |
| `ZBW-ADDON-210..216` | Golden GG post-game policy and primary presentation | M16 visible-state placeholders where applicable; M22 legacy rendering |
| `ZBW-ADDON-245..251` | XP Bar progression projection and primary display contract | M16 placeholders; M22 packet/API fallbacks and full compatibility |
| `ZBW-ADDON-266..273` | reward aggregation, ledger/delivery state, history and primary summary presentation | M13 later reward-source integrations; M16 placeholders; M19/M20 distributed delivery; M22 compatibility |
| `ZBW-ADDON-274..282` | holiday campaign eligibility, claims, idempotent reward delivery and primary calendar/claim presentation | M14 general profile/calendar platform; M16 placeholders; M19/M20 distributed delivery; M22 compatibility |

### M11.1 Phase 2 corrective evidence

| Allocation | Implemented Phase 2 portion | Evidence / remaining owner |
|---|---|---|
| `ZBW-ADDON-010..025`, `061..070`, `141..147`, `184..193` | Armed, bounded LuckyBlock, sponge and Pop-up Tower neutral mechanics with typed M10 bindings and atomic effect intents | `ModeMechanicsTest`; concrete Paper effects remain M11.1 Phase 3, compatibility remains M22 |

### M11.1 final integration and certification evidence

| Requirement allocation | Final M11 implementation | Verification and retained ownership |
|---|---|---|
| `ZBW-SHOP-001..004` | owner-thread atomic inventory/tender purchase execution; `JdbcShopStateStore` Quick Buy, favourites, history and local rotation recovery | `AtomicInventoryPurchasePortTest`, `JdbcShopStateStoreTest`; M12 virtual-currency ledgers and M21 Vault remain deferred |
| `ZBW-SHOP-005..007`, `ZBW-CONTENT-002/003` | lifecycle-integrated upgrades, forge, traps, generators, utility actions, original content and transactional configuration | shop/configuration/content suites plus exact Paper certification; M16 placeholders and M22 fallbacks remain deferred |
| `ZBW-GAME-004/005` and all M11 addon ranges | M10-bound named mechanics run from M08 snapshots through one deterministic M11 coordinator | `ModeMechanicsTest`, `M11MatchRuntimeTest`, addon catalogue validator; M15 statistics and all listed later cells remain deferred |
| M11 commands, GUIs and permissions | all 25 M11 actions bind to the M09 registry, unified command tree and generated parity pages through `M11PresentationBindings` | `M11BindingsTest`, M11 inventories, catalogue/dashboard validation; no replacement framework |
| M11 primary platform surface | Java 21 `BukkitM11Platform` translates committed intents to inventory, drops, blocks, sounds, particles, effects and cleanup | mandatory `m11_paper_e2e.py` on Paper 1.21.1 build 133; M22 legacy certification remains deferred |

The RC-087 implementation uses the cycle-free production edge `zbw-storage-sql -> zbw-shop`;
arena/game no longer depend on the concrete SQL implementation. PR #18 subsequently passed its
mandatory CI and Paper 1.21.1 jobs and was squash-merged as
`3e68835c361216e6dc8be37b9e024734bb565884`, resolving RC-087. The matrix retains 672 requirements
and every later M12/M15/M16/M17/M18/M19/M20/M21/M22 allocation; M12 is now complete.
| `ZBW-ADDON-236..244`, `300..322`, `341..349` | arbitrary-team Swappage mechanics, all seven Ultimate abilities, Voidless and Rush driven by M08 snapshots | 2/4/8-team, lifecycle, ability and bounds tests; M15 statistics and M16 placeholders remain deferred |
| `ZBW-ADDON-363..368`, `379..397`, `438..452` | per-arena generator overrides, deterministic local rotation, metadata-preserving colour conversion and team-isolated BedSteal state | generator tests plus `ModeMechanicsTest`; M19/M20 distribution, M16 placeholders and M22 fallbacks remain deferred |
