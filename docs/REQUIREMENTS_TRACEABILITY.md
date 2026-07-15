# ZartraBedWars Requirements Traceability Matrix

## Rules and legend

This baseline contains 672 stable semantic requirement IDs. Part I contains exactly one row for each of the 199 PRD IDs (144 original core IDs plus 55 accepted owner-decision IDs). Part II is the normative atomic matrix in `docs/MASTER_PROMPT_COVERAGE.md`, containing one `MP-L####` row for every non-empty source assertion in `MASTER_PROMPT.md`. Part III is `docs/ADDON_FEATURE_CATALOG.md`, containing one complete mapping row for each of the 473 owner-supplied native-addon requirements. Locations are planned until implementation; replace plans with exact package/class/table/key/file/test links as each milestone starts. A dash means the surface has no direct user-facing behavior and must include a reason; it never omits documentation or verification.

- **Cmd/Perm:** command family and canonical permission family; exact subcommands/nodes are frozen before the implementation milestone.
- **API/Evt:** public API service/provider and event family.
- **PH:** PlaceholderAPI family; `health` means sanitized public/operator status only.
- **Tests:** required suites in addition to the PRD impact profile.
- All rows inherit the global config/documentation/quality rules in ZBW-OPS-001/009 and ZBW-QA-001/006/007.

## Part I — Requirement-level matrix

## Governance (11)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-GOV-001 | PRD/change-control checks; docs only | document metadata | — | — | requirement metadata schema | — | docs lint | PRD governance; M00/M01 |
| ZBW-GOV-002 | scope-change approval policy | — | — | — | — | — | traceability diff check | contribution/governance; M00 |
| ZBW-GOV-003 | `build/module-graph.json`; PRD/trace registries; `tools/validation/foundation.py` dependency/orphan checks | versioned JSON governance manifests | M1 validator / maintainer | — (build governance) | machine-readable module/requirement manifests; runtime API remains M02+ | — | graph and coverage-ID tests | `docs/IMPLEMENTATION_M01.md`; M00/M01 |
| ZBW-GOV-004 | accepted ADR repository plus decision-document validator | — | pre-code decision validator / maintainer | — | ADR metadata in Markdown | — | ADR/document completeness gate | `docs/DECISIONS`; M00/M01 |
| ZBW-GOV-005 | `build/module-graph.json`, `build/toolchains.json`, `tools/validation/foundation.py` | machine-readable layer/bytecode policy | M1 validator / maintainer | — | — (public architecture API is later) | — | cycle, forbidden-layer, reactor and class-major checks | architecture + implementation guide; M01 |
| ZBW-GOV-006 | `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`, M1 static source policy | `build/quality-policy.json` | M1 validator / maintainer | — | — | — | static config/forbidden-marker/bytecode tests; Java analyzers activate with Java modules | quality gates + implementation guide; M01 |
| ZBW-GOV-007 | deterministic M1 evidence runner; final cross-feature compliance remains M24 | `build/milestone-state.json`, quality/release policy | M1 validator / maintainer | — in M01 | generated lock/SBOM/report schemas; runtime API M24 | — in M01 | M1 exit-gate suite; final 672-ID compliance M24 | implementation guide; M01/M24 |
| ZBW-GOV-008 | two pinned CI workflows and clean build orchestrator | `.github/workflows/*.yml` | `python tools/build/clean_build.py --jdk <id>` | — | — | — | five-JDK reactor matrix + deterministic governance job | milestones + implementation guide; all |
| ZBW-GOV-009 | `lock_dependencies.py`, asset provenance scan, generated SBOM/notices | dependency acquisition and asset manifests | dependency `validate` / maintainer | — (reports are files in M01) | CycloneDX/lock schemas | — | hash/licence/rights/binary/asset drift gates | dependency audit, asset provenance, notices; M01/M24 |
| ZBW-GOV-010 | PRD change procedure, `AGENTS.md`, exact M1 scope guard | — | M1 validator / maintainer | — | — | — | source/trace diff and milestone-scope review | PRD governance + implementation guide; all |
| ZBW-GOV-011 | three coverage tools orchestrated by `run_m01_validation.py`; Java work controlled by milestone state | `.python-version` 3.12.13; coverage metadata | M1 validator / maintainer | — (generated reports) | coverage report contracts; runtime API later | coverage counts in reports | 6,438 Master + 473 addon + 55 decision assertions | Master coverage + implementation guide; M00/M01/M24 |

## Architecture foundations (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ARC-001 | M1 parent/BOM/governance plus M02 five-module API/domain/application/SDK/Discord reactor and machine graph | Maven/module manifests | deterministic validators / maintainer | runtime health later | M02 provider/extension lifecycle contracts | — | exact reactor/dependency/cycle/forbidden-import checks | architecture + M01/M02 implementation guides |
| ZBW-ARC-002 | platform-free core/version-family boundaries fixed in `build/module-graph.json`; no compat production module materialized | toolchain/module manifests | M1 validator / maintainer | — | `CompatibilityApi` remains M06 | — | forbidden-layer and zero-Java checks; CT M06/M22 | runtime/compatibility guides; M01/M06/M22 |
| ZBW-ARC-003 | M02 `zbw-api`, `zbw-sdk`, five Java-8 artifacts and `api-signature-baseline.txt` | semantic API/version-range policy in contracts; runtime config later | developer tooling later | API browser later | `VersionedApi`, `ApiVersions`, immutable public contracts | api version later | binary descriptor/class-major/unit contracts | `API_M02.md`, generated JavaDoc, ADR-0017; M02 VERIFIED |
| ZBW-ARC-004 | M02 immutable event identity/metadata/cancellation/listener primitives; runtime bus/queues later | queue limits remain M05 | event debug later | event diagnostics later | `EventMetadata`, `ApiEvent`, thread context and schema/order fields | safe metrics later | event equality/order/schema/thread/cancel/null tests | `API_M02.md`; M02 contracts VERIFIED / M05 runtime |
| ZBW-ARC-005 | scheduler port/thread guards | executor/deadline config | threads / debug | thread dashboard | `SchedulerApi` | thread/queue health | owner-thread tests | threading guide; M05 |
| ZBW-ARC-006 | bounded work/lifecycle drain | queue/backpressure config | queues / admin.debug | queue dashboard | work/lifecycle API | queue sizes | saturation/cancel/shutdown | operations guide; M05 |
| ZBW-ARC-007 | M02 generic provider descriptor/health/capability/asynchronous lifecycle SPI; selection/registries/adapters later | provider selection remains M06+ | runtime status/reload later | integration manager later | `Provider`, capability and extension contracts | typed health snapshot | M02 capability/metadata/shape tests; adapter suites later | `API_M02.md`; M02 foundation / M06+ adapters |
| ZBW-ARC-008 | Versioned M1 manifests plus M04 contiguous SQL schema plan/history, SHA-256 drift rejection and Flyway bridge | manifest/database schema versions | database migrate/validate remains M09 command surface | — in M04 | `Migration`, `MigrationPlan`, `SchemaMigrator` | schema health later | ordering/checksum/restart/Flyway CT | `MIGRATIONS_M04.md`; M01/M04 |
| ZBW-ARC-009 | M02 UUID and namespaced immutable typed-ID families; runtime registry/mapping later | canonical ID grammar | inspect later | inspectors later | 18 typed public IDs | public IDs later | type separation, canonical parse/round-trip/equality/hash/malformed tests | `API_M02.md`; M02 VERIFIED / M07 registry |
| ZBW-ARC-010 | M02 extension metadata/lifecycle/catalogue/point contracts plus restricted reader and deterministic validator | schema-1 metadata keys; runtime extension settings later | validate tool contract; commands later | extension manager later | `Extension`, `ExtensionMetadata`, `ExtensionValidation` | extension health later | six fixtures, duplicate/self/missing/version/unknown/malformed tests | `API_M02.md`, ADR-0017; M02 VERIFIED / M23 runtime |

## Core game and modes (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-GAME-001 | game state machine; match/session records | modes/arenas/performance | join,start,forcestart / play,force | waiting/admin arena | Game/Arena APIs + lifecycle | arena state/countdown | state/property/E2E | gameplay guide; M08 |
| ZBW-GAME-002 | combat/elimination/phase policies | modes/events/items | game event controls / force | spectator/admin | combat/phase events | bed/team/time state | gameplay matrix | gameplay/events guide; M08/M10 |
| ZBW-GAME-003 | completion orchestrator + outbox | rewards/restore | end / force | reward/end summary | completion/reward/stat/replay events | result/rewards | retry/crash exactly-once | recovery/reward guide; M08/M12 |
| ZBW-GAME-004 | mode definitions/strategies | `modes.yml` | mode select/manage / use,manage | mode selector/editor | Mode API/events | mode + mode stats | all-mode E2E | modes guide; M10 |
| ZBW-GAME-005 | per-mode component registry/data migration | modes + feature files | mode validate / manage | mode editor | mode extension SPI | dynamic mode values | isolation/extension CT | mode SDK guide; M10 |
| ZBW-GAME-006 | lobby services/state | config/messages/gui/npcs/holograms | lobby / use,manage | lobby menus/admin | Lobby API/events | lobby/player counts | protection/interaction E2E | lobby guide; M08 |
| ZBW-GAME-007 | matchmaking/selector services | modes/arenas/proxy | join,quick,random / play | arena/map/mode/group | Matchmaking API/events | queue/arena availability | capacity/routing PT | matchmaking guide; M10/M20 |
| ZBW-GAME-008 | state-specific hotbar service | gui/items | hotbar reload / manage | hotbar editor | Hotbar API/events | — | transition/leak E2E | hotbar guide; M08/M09 |
| ZBW-GAME-009 | spectator/staff use cases + audit | security/messages | staff,spectate,freeze… / staff.* | spectator/staff panels | Spectator/Staff APIs | safe ping/state | auth/privacy E2E/ST | staff guide; M10/M17 |
| ZBW-GAME-010 | recovery coordinator + recovery markers | recovery/performance | recover,health / admin.force | health/recovery | Recovery API/events | recovery state | fault/restore MT | recovery runbook; M05/M08 |

## Arena, map, world and setup (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ARENA-001 | arena repositories/services; arena schema | arenas/maps | arena CRUD/import/export… / arena.* | arena manager | Arena API/events | arena fields | lifecycle/MT | arena guide; M07 |
| ZBW-ARENA-002 | map aggregate/snapshots | maps | map edit/rename/inspect / map.* | map manager | Map API/events | map fields | rename/reference UT | map guide; M07 |
| ZBW-ARENA-003 | ID registry/import mapper | maps/migration | map import/duplicate / import,duplicate | import/duplicate flow | identity/migration events | map_id | collision/mapping MT | map/migration guide; M07 |
| ZBW-ARENA-004 | deep duplicate use case; copied definitions | maps/arenas/world | map duplicate / duplicate | duplicate map GUI | duplicate API/event | source/new map IDs | deep-copy E2E | duplicate guide; M07 |
| ZBW-ARENA-005 | world SPI + provider adapters | integrations/world | world load/clone/backup… / world.* | world manager | World API/provider events | world/provider health | provider CT | world provider guide; M06/M21 |
| ZBW-ARENA-006 | reset/cleanup pipeline | performance/world | world reset / reset,force | reset progress | Reset API/events | reset state/time | concurrent reset PT | reset/tuning guide; M06/M07 |
| ZBW-ARENA-007 | setup session/wizard data | arenas/gui/messages | setup wizard/save/cancel / setup.* | setup wizard | Setup API/events | completion percent | full wizard E2E | setup guide; M07 |
| ZBW-ARENA-008 | validation rule registry/report | all arena/world configs | setup/arena validate / validate | validator GUI | Validation API/events | validation state | mutation/rule UT | validation guide; M07 |
| ZBW-ARENA-009 | admin use-case surfaces/audit | permissions/gui | all arena/map/world admin / granular | arena/map/world panels | existing APIs | — | surface/auth ST | admin reference; M07/M09 |

## Shop, upgrades, generators and items (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-SHOP-001 | shop catalogs/favourites; player quick-buy data | shops | shop,quickbuy / shop.use | shop/Quick Buy | Shop API/events | shop/category state | catalog/E2E | shop guide; M11 |
| ZBW-SHOP-002 | declarative layout/rendering/purchase history | shops/gui | shop manage / shop.manage | full shop/editor | GUI extension API | visible price/state | GUI input E2E | shop GUI guide; M11 |
| ZBW-SHOP-003 | purchase transaction/limits | shops/items | purchase admin / manage,bypass | confirmation/history | Purchase API/events | purchase limits | concurrency/exploit ST | purchase API guide; M11 |
| ZBW-SHOP-004 | tender provider/value model | shops/integrations | currency inspect / manage | price/tender views | Tender API | tender values | provider/atomicity CT | currency integration guide; M11 |
| ZBW-SHOP-005 | upgrade/trap definitions/team state | upgrades | upgrade manage / upgrade.* | upgrade shop/editor | Upgrade API/events | levels/traps | upgrade matrix | upgrades guide; M11 |
| ZBW-SHOP-006 | generator definitions/runtime state | generators | generator manage / generator.* | generator editor | Generator API/events | state/countdown/level | load/cap/split PT | generator guide; M11 |
| ZBW-SHOP-007 | item/action registries; definition migration | items/security | item manage / item.* | item editor/preview | Item/Action SPIs/events | item cooldown | item/version/sandbox CT/ST | item guide; M11 |

## Progression and rewards (14)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-PROG-001 | projection coordinator + inbox | progression/rewards | progression debug / admin.debug | progression inspector | Progression events/API | projection health | duplicate/fanout IT | progression architecture; M12 |
| ZBW-PROG-002 | XP ledger/rules | rewards/levels | experience add/set/remove / admin.* | XP summary/admin | Experience API/events | XP/progress | formula/farming ST | leveling guide; M12 |
| ZBW-PROG-003 | level state/formulas/history migration | levels/rewards | level/experience admin / level.* | level-up/formula editor | Level API/events | level fields | formula/recalc MT | level guide; M12 |
| ZBW-PROG-004 | currency ledger/transactions/migration | currencies/database | coins/currency admin / currency.* | currency/audit | Currency API/events | currency_<id> | atomic/race/MT/ST | currency guide; M12 |
| ZBW-PROG-005 | prestige definitions/history migration | prestiges/messages | prestige admin / prestige.* | editor/preview | Prestige API/events | prestige fields | render/version/MT | prestige guide; M12/M22 |
| ZBW-PROG-006 | cosmetic catalog + licensed assets | cosmetics | cosmetics admin / cosmetics.manage | cosmetic/editor | Cosmetic registry/API | catalog/count | 300-count/license/PT | cosmetics guide; M14 |
| ZBW-PROG-007 | ownership/equipment/preset repositories | cosmetics/database | grant/revoke/equip / cosmetics.* | ownership/preset/admin | Ownership API/events | owned/equipped | expiry/auth/MT | cosmetics admin guide; M14 |
| ZBW-PROG-008 | effect runtime/budgets | cosmetics/performance | cosmetics disable/profile / admin | visibility/performance | Effect API/events | visibility setting | packet/entity PT | cosmetics tuning; M14 |
| ZBW-PROG-009 | quest definitions/assignments/history | quests | quest/admin CRUD… / quest.* | quest/editor/inspector | Quest API/events | quest values | lifecycle/E2E | quest guide; M13 |
| ZBW-PROG-010 | objective registry/progress repository | quests/achievements/challenges | progress admin / progression.admin | objective editor/debug | Objective SPI/events | objective progress | objective matrix/MT | objective SDK; M13 |
| ZBW-PROG-011 | reward ledger/outbox/failure queue | rewards | rewards grant/retry / rewards.* | summary/editor/queue | Reward API/events | reward summary | idempotency/failure ST | reward guide; M12 |
| ZBW-PROG-012 | achievement definitions/progress/history | achievements | achievement admin / achievement.* | achievement/editor | Achievement API/events | achievement values | shared-objective E2E | achievement guide; M13 |
| ZBW-PROG-013 | challenge/pass/season/claim data + migration | challenges/battlepass | challenge/pass admin / battlepass.* | challenge/pass/editors | Challenge/BattlePass API/events | pass/challenge/season | claim/rollover/MT/ST | battle pass guide; M13 |
| ZBW-PROG-014 | profile/settings/calendar data; privacy deletion | profile/rewards/security | profile/settings/admin repair / profile.* | profile/settings/calendar admin | Profile/Calendar APIs | all profile settings | privacy/calendar/MT | profile/privacy guide; M14 |

## Replay (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-REPLAY-001 | replay metadata/manifest identities | replay/database | replay info/list / replay.use | replay browser/details | Replay API/events | count/recent ID | lifecycle/identity | replay guide; M17 |
| ZBW-REPLAY-002 | capture buffer/codec/chunks | replay/performance | recording admin / manage | recording status | capture/event SPI | recording state | ordering/load PT | replay format guide; M17 |
| ZBW-REPLAY-003 | fidelity classification/source metadata | replay | telemetry debug / staff | telemetry detail | Telemetry API | safe availability | semantic/golden tests | evidence semantics; M17 |
| ZBW-REPLAY-004 | playback scene/session/render adapter | replay/gui | replay view controls / view.* | viewer controls | Viewer API/events | playback state | speed/timing/CT | viewer guide; M17/M22 |
| ZBW-REPLAY-005 | timeline index/search | replay | replay search / view | timeline | Timeline API | — | filter/jump correctness | timeline guide; M17 |
| ZBW-REPLAY-006 | telemetry index/annotations/case links | replay/anticheat/security | annotation/case/export / staff.* | staff investigation | Evidence API/events | restricted none | auth/source/ST | staff evidence guide; M17 |
| ZBW-REPLAY-007 | replay access/favourite/share data | replay/security | replay player/map/favorite… / granular | browser/history/settings | Access API/events | safe history | privacy/permission ST | player replay guide; M17 |
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
| ZBW-STATS-006 | admin correction/audit/import/export | statistics/migration | stats admin all / admin.* | inspector/repair | Admin Statistics API | — | dry-run/rollback ST/MT | admin stats guide; M15 |
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
| ZBW-DEPLOY-001 | shared Paper assembly/resource manager | deployment/performance | deployment/health / admin | health/world/arena | Deployment API/events | deployment counts | 40-world/10-active PT | shared-server guide; M08/M24 |
| ZBW-DEPLOY-002 | distributed application flows/state | deployment/proxy/redis | network status / admin | network manager | Network API/events | server/backend/proxy | cross-server E2E | proxy network guide; M20 |
| ZBW-DEPLOY-003 | deployment validator/safe state | deployment/integrations | validate / admin.validate | validation GUI | validation API/events | provider status | missing-service tests | deployment guide; M03/M20 |
| ZBW-DEPLOY-004 | proxy protocol + Velocity/Bungee adapters | proxy/security | proxy commands / proxy.* | proxy manager | Proxy API/events | proxy/backend | equivalence/signature ST | Velocity/Bungee guides; M20 |
| ZBW-DEPLOY-005 | CloudNet provider/scaling state | cloudnet | cloudnet commands / cloudnet.* | CloudNet manager | ServiceDiscovery API/events | cloudnet status | scale/drain/crash | CloudNet guide; M21 |
| ZBW-DEPLOY-006 | Redis keys/streams/locks/invalidation | redis/security | redis manage/diagnostics / redis.* | Redis manager | Coordination API/events | redis status | partition/load/ST | Redis guide; M19 |
| ZBW-DEPLOY-007 | M04 typed repositories/UoW; Hikari SQLite/MySQL/MariaDB adapters; migration/cache/recovery foundation | database/security | database manage/migrate / database.* remains M09 | database manager remains M09 | Storage/Migration/Recovery APIs | sanitized pool/schema status | SQLite DB/MT; external DB CT pending; PT/ST later | M04 database/migration/backup/query-plan guides |
| ZBW-DEPLOY-008 | M04 versioned envelope plus atomic inbox/outbox/idempotency/order store; Redis/proxy transport remains M19/M20 | deployment/redis/database | message debug / debug later | protocol health later | `MessageEnvelope`/`MessageRepository` | protocol versions later | order/duplicate/retry SQLite IT; partition/rolling later | M04 API/implementation; protocol guide M19 |
| ZBW-DEPLOY-009 | degradation/recovery coordinator | deployment/performance | maintenance/recover / force | network health | Recovery API/events | degraded state | chaos/MT | failure runbook; M19/M20 |

## Integrations and compatibility (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-INT-001 | PlaceholderAPI adapter | integrations/placeholders | integration status/reload / manage | integration manager | provider adapter | native | absence/version CT | Placeholder install guide; M16 |
| ZBW-INT-002 | Vault economy/permission/chat adapters | integrations | vault status / manage | integration manager | provider SPIs | provider status | provider/transaction CT | Vault guide; M21 |
| ZBW-INT-003 | LuckPerms context/meta adapter/cache | integrations/permissions | luckperms status / manage | integration manager | Permission/Meta provider | safe rank meta | invalidation/context CT | LuckPerms guide; M21 |
| ZBW-INT-004 | ProtocolLib/direct packet adapters | integrations/performance | packet diagnostics / debug | packet health | Packet SPI/events | packet health | version/rate ST/PT | packet guide; M06/M22 |
| ZBW-INT-005 | world provider adapters/intermediate migration | integrations/world | provider migrate/status / world.* | world/integration manager | WorldProvider SPI | provider status | provider CT/MT | provider guides; M06/M21 |
| ZBW-INT-006 | internal/Citizens/ZNPC NPC adapters/data migration | npcs/integrations | npc CRUD/import/export / npc.* | NPC manager | NpcProvider API/events | NPC state if safe | provider/switch CT | NPC guide; M21 |
| ZBW-INT-007 | internal/Decent hologram adapters | holograms/integrations | hologram manage / hologram.* | hologram manager | HologramProvider | content values | rate/provider CT/PT | hologram guide; M21 |
| ZBW-INT-008 | Grim/Vulcan normalized alert adapters | anticheat/integrations | alerts/manage / anticheat.* | alert manager | AntiCheatProvider/events | safe violation values | dual/dedupe/PT/ST | Grim/Vulcan guides; M21 |
| ZBW-INT-009 | native/AlessioDP party provider + migration | parties/integrations | party + admin / party.* | party/admin | Party API/events | party fields | provider/cross-server MT | party guide; M20/M21 |
| ZBW-INT-010 | version/Bedrock capability adapters/mappings | integrations/compatibility | compat diagnostics / debug | compatibility/alt controls | Compatibility API/events | server/client version | 1.8–1.21/Bedrock matrix | compatibility guide; M22 |

## GUI, commands, permissions and localization (6)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-UX-001 | UI page model/Paper renderer/state | gui/messages/performance | gui reload / use,manage | all required GUIs | GUI API/events | rendered safe values | inventory/accessibility PT | GUI guide; M09 |
| ZBW-UX-002 | GUI definition editor/schema migration | gui | gui edit/import/export / gui.* | GUI editor | GUI registry API/events | preview values | undo/validation/MT/ST | GUI editor guide; M09 |
| ZBW-UX-003 | command tree/adapters/help/audit | commands/messages | all required commands / per action | command inspector/help | Command API/events | — | command inventory/contexts | command reference; M09+ |
| ZBW-UX-004 | M03 `PermissionNode`/request/decision/service plus sole exact-grant implementation, target scoping, 33 actions, one-hop aliases and audit port | `permissions.yml`, security metadata | command inventory remains M09 | inspector remains M09 | authorization API; decision audit port | — | exact/target/alias/deny/null/centralization tests VERIFIED | `PERMISSIONS_M03.md`; M03 VERIFIED / M09 surfaces |
| ZBW-UX-005 | M03 locale/message/typed-parameter API plus immutable catalogs, fallback, completeness and deterministic import/export; component renderers remain M22 | `config.yml`, `messages.yml` | language/reload remain M09/M22 | language/editor/report remain M09/M22 | localization API | locale-aware contract; PAPI remains M16 | fallback/completeness/switch/escape/malformed tests VERIFIED | `LOCALIZATION_M03.md`; M03 VERIFIED / M22 rendering |
| ZBW-UX-006 | accessibility/capability patterns | gui/messages | settings / player | accessible alternatives | capability API | settings | task usability/Bedrock E2E | accessibility matrix; M09/M22 |

## Operations, security and delivery (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-OPS-001 | M03 immutable versioned `ConfigurationModel`, exact 36 schemas, full option metadata, reference generator and backup-first pure migration service | all 36 listed logical files | config command remains M09 | editor remains M09 | typed configuration API/model | config version remains later surface | schema/default/range/unknown/dependency/reference/migration/fixture tests VERIFIED | `CONFIGURATION_REFERENCE_M03.md`; M03 VERIFIED |
| ZBW-OPS-002 | M03 `SecretRef`, injected provider/environment/protected-file sources, zeroizable lease, exact redactor and export allowlist | `security.yml` plus provider credential refs | security diagnose remains M09/M24 | security status remains later | secret API/services | none | priority/missing/zeroize/redaction/allowlist tests VERIFIED | `API_M03.md`; M03 VERIFIED / M24 operations |
| ZBW-OPS-003 | M03 validates all 36 documents, external-check ports, cross-document dependencies, duplicate/missing files and bounded option counts | all 36 schemas | validate/doctor remains M09/M23 | dashboard remains later | typed validation reports | validation status remains later | malformed/unknown/dependency/secret/cross-document/fixture tests VERIFIED | `CONFIGURATION_REFERENCE_M03.md`; M03 VERIFIED / platform checks later |
| ZBW-OPS-004 | M03 prepare/apply/reverse-rollback plans, restart reporting, targets and last-known-good publication | every option declares target/restart metadata | reload command remains M09 | reload controls remain M09 | transactional reload service | reload state remains later | success/prepare/apply/rollback/restart tests VERIFIED | `CONFIGURATION_REFERENCE_M03.md`; M03 VERIFIED |
| ZBW-OPS-005 | error taxonomy/retry/circuit/alerts | performance/security/messages | diagnostics/recover / admin | health/error panels | Failure API/events | sanitized health | fault injection | recovery guide; M05 |
| ZBW-OPS-006 | metrics/health/Doctor/debug exports | performance/security | health,performance,doctor / admin.* | all health dashboards | Health/Performance APIs | safe health families | cardinality/redaction/PT | operations/tuning; M05/M24 |
| ZBW-OPS-007 | external stats/Discord providers | integrations/security | external-api manage / api.* | integration manager | ExternalStats SPI/API | only scoped public | auth/rate/privacy ST/PT | external API guide; M16/M21 |
| ZBW-OPS-008 | checksum-locked Maven, eight-project reactor, two M02 CI workflows, dual locks/SBOMs/notices and API baseline | `.mvn/`, POMs, `build/*`, workflows | clean build and M01/M02 validators / maintainer | — | five M02 Java 8 artifacts | build/API versions in manifests | five-JDK offline builds, quality and governance CI | M01/M02 implementation guides; M01/M02/M24 |
| ZBW-OPS-009 | documentation inventory/generators | docs config | docs generate / maintainer | doc links | generated API docs | generated PH docs | link/inventory lint | all guides; continuous |

## Quality (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-QA-001 | Python stdlib governance tests at `tests/governance`; Java test BOM selections declared but unresolved | quality/toolchain manifests | `python -m unittest discover -s tests/governance -v` | — | test-only helpers | — | 8 M1 tests; feature suites remain their milestones | implementation guide; all |
| ZBW-QA-002 | gameplay E2E scenario matrix | test fixtures | — | tested GUIs | tested APIs/events | tested mode PH | full gameplay matrix | test report; M10/M24 |
| ZBW-QA-003 | replay/Atlas/stat/rank/PH suites | test fixtures | — | tested surfaces | tested APIs | full PH test | mandated cases | test report; M16–M18 |
| ZBW-QA-004 | benchmark/load harness + result store | benchmark manifest | benchmark run / developer | performance dashboard | benchmark API | performance metrics | all workload PT | benchmark report; M24 |
| ZBW-QA-005 | M1 governance plus M02 Checkstyle/SpotBugs/JaCoCo, architecture and binary API gates; release/mutation/vulnerability evidence continues M24 | quality policy, POM profiles, CI | M02 validator / maintainer | — | exact binary signature baseline active | — | zero static findings, enforced 90/85 domain/application, forbidden imports and binary drift | quality gates + M02 implementation; M01/M02/M24 |
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
| ZBW-CONTENT-001 | provenance/binary scan plus M02 generic typed content contracts; no creative asset or balance is packaged | exact provenance/rights manifest; runtime content later | validators / maintainer | — | content registry/provider/pack metadata | — | clean-room asset/hash/repository and registry tests | asset provenance + M01/M02 implementation; M01/M02/M24 |
| ZBW-CONTENT-002 | shop balance definition pack | `content.yml`, `shops.yml` | content shop validate/preview / content.manage | shop profile editor | BalanceProfile API/events | active shop profile | schema/golden/E2E | original starter catalogue; M11 |
| ZBW-CONTENT-003 | per-mode balance definition packs | `content.yml`, `modes.yml` | content mode validate/preview / content.manage | mode balance editor | ModeBalance API/events | active mode profile | mode matrix/PT | original starter catalogue; M10/M11 |
| ZBW-CONTENT-004 | quest seed definitions | `quests.yml`, `content.yml` | quest validate/import / quest.manage | quest editor | Quest registry/events | quest ID/progress | objective/config E2E | original starter catalogue; M13 |
| ZBW-CONTENT-005 | achievement seed definitions | `achievements.yml`, `content.yml` | achievement validate/import / achievement.manage | achievement editor | Achievement registry/events | achievement ID/tier | tier/MT/E2E | original starter catalogue; M13 |
| ZBW-CONTENT-006 | starter season/tier definitions | `battlepass.yml`, `content.yml` | pass validate/import / battlepass.manage | pass editor | BattlePass registry/events | season/tier/track | claim/rollover/MT | original starter catalogue; M13 |
| ZBW-CONTENT-007 | cosmetic seed + 300-definition gate | `cosmetics.yml`, content packs | cosmetics validate/import / cosmetics.manage | cosmetics/provenance editor | Cosmetic registry/events | catalogue/provenance counts | 300-count/hash/PT | starter/provenance guides; M14 |
| ZBW-CONTENT-008 | private preset definitions | private-games/content config | private preset validate/preview / private.manage | host/admin preset editor | PrivatePreset API/events | active preset/multipliers | GUI/generator E2E | Resource Scarcity catalogue; M20 |
| ZBW-CONTENT-009 | semantic feedback/effect definitions | content/compatibility/messages | effect validate/preview / content.manage | effect preview | SemanticEffect API/events | selected effect/fallback | adapter/usability CT | starter + fallback matrix; M06/M14 |
| ZBW-CONTENT-010 | M02 typed content definition/provider/pack/version contracts and immutable duplicate-safe registry; parsing/migration later | pack metadata contract; `content.yml` remains M03 | runtime validate/reload/migrate later | manager later | `ContentRegistry` and provider contracts | pack version/health later | ordering, immutability, lookup, duplicate ID and metadata tests | `API_M02.md`; M02 VERIFIED / M03 runtime |
| ZBW-CONTENT-011 | machine registry `build/asset-provenance.json`; zero approved/packaged assets; deterministic scanner | provenance manifest | M1 validator / maintainer | — (file report in M01) | provenance schema; runtime API later | zero approved/packaged in manifest | path/hash/licence/status drift CI | `docs/ASSET_PROVENANCE.md`; implementation guide; M01/M14/M24 |

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
| ZBW-DISCORD-008 | M03 provider-neutral `SecretRef`, injected resolution/zeroization/redaction foundation; provider rotation remains M16 | credential references only; no token values | diagnose/rotate remain M16/M24 | redacted status remains later | secret API integrated with config foundation | — (secrets prohibited) | seeded priority/missing/zeroize/redaction/export tests VERIFIED | `API_M03.md`; M03 VERIFIED / M16 rotation |

## Mandatory Minecraft 1.8 fallbacks (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-COMPAT-001 | separate artifact/toolchain boundaries in `module-graph.json`; exact Temurin 8/11/16/17/21 acquisition; no bootstrap/adapter claim yet | toolchain/module manifests | M1 validator / maintainer; runtime status later | — in M01 | capability API remains M06 | — in M01 | five-JDK empty build + class-major policy; startup/gameplay M22 | runtime/fallback matrices + implementation guide; M01/M22 |
| ZBW-COMPAT-002 | `zbw-compat-v1_8` adapter only | adapter selection | compat inspect / debug | capability diagnostics | Compatibility SPI | capability health | forbidden import/startup CT | compatibility architecture; M06 |
| ZBW-COMPAT-003 | legacy material/data/NBT registry | material fallbacks | compat material test / debug | mapping inspector | material/item capability API | fallback material ID | round-trip/tamper/MT | fallback matrix; M06/M22 |
| ZBW-COMPAT-004 | legacy particle renderer | particle fallbacks/budgets | compat particle preview / debug | effect preview | particle capability API | fallback/suppression state | packet/budget/usability CT | fallback matrix; M06/M22 |
| ZBW-COMPAT-005 | legacy sound renderer | sound fallbacks/volume | compat sound preview / debug | sound preview | sound capability API | fallback/suppression state | mapping/usability CT | fallback matrix; M06/M22 |
| ZBW-COMPAT-006 | legacy text/action renderer | text/action fallback rules | compat text preview / debug | rendered preview | text capability API | renderer class | task/format/Unicode E2E | fallback matrix; M06/M22 |
| ZBW-COMPAT-007 | entity/packet/GUI/input fallback providers | entity/packet/gui mappings | compat component inspect / debug | component diagnostics | packet/entity/UI capabilities | capability/fallback | packet/cleanup/input E2E | fallback matrix; M06/M22 |
| ZBW-COMPAT-008 | typed degradation/last-known-good policy | suppression/fallback order | compat validate / admin.validate | validation report | fallback decision events | suppression reason | mutation/fault CT | compatibility policy; M06/M22 |
| ZBW-COMPAT-009 | normative fallback doc plus exact 22-row `private-runtime-fixtures.json` inventory; certification stays NOT_STARTED | compatibility/fixture manifests | M1 validator / maintainer | — (matrix documents in M01) | fixture/report schema; runtime API later | fixture/coverage counts | row/hash/private-lock/no-support-claim validator | fallback/runtime matrices + implementation guide; M01/M22/M24 |

## Dependency licensing and redistribution (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-LICENSE-001 | M01 14-artifact tool registry plus M02 170-component/535-file exact Maven registry and two CycloneDX SBOMs | acquisition/lock policy JSON | dependency validators / maintainer | — | CycloneDX/lock schemas | — | inventory/rights/licence/source/hash scan | dependency audit + M01/M02 implementation; M01/M02/M24 |
| ZBW-LICENSE-002 | immutable tool lock plus Maven Central URL/SHA-256/size lock; wrapper and restore verify before offline build | default-deny lock/update policy | lock `generate|capture|validate|seed|restore` / maintainer | — | dependency metadata files | build versions only | hash/source/dynamic-version/staleness/offline CI | dependency audit; M01/M02 |
| ZBW-LICENSE-003 | explicit product-right booleans; all activated M02 Maven components are build/test-only and non-bundled | rights records per artifact/component | dependency `validate` / maintainer | — | governance records only | — | redistribution/product-binary rejection | root/build notices; M01/M02/M24 |
| ZBW-LICENSE-004 | per-component SPDX/declaration/source evidence and generated M02 build notices; product shading/modification disabled | rights/obligation records | dependency `validate` / maintainer | — | SBOM properties | — | missing-rights/evidence and product-packaging scan | dependency audit/build notices; M01/M02/M24 |
| ZBW-LICENSE-005 | Only approved exact M02 JUnit/build/quality selections resolve; complete graph locked before normal offline build | BOM + scope policy | dependency and Maven lock validators / maintainer | — | M02 neutral provider SPIs | — | direct dependency/plugin lock and architecture checks | dependency/integration guides; M01/M02/M21 |
| ZBW-LICENSE-006 | repository scan rejects JAR/class/native/server/proprietary binaries outside ignored verified cache | prohibited suffix/name policy | dependency `validate` / maintainer | — | — | — | repository binary scan | contributor/legal guide; M01/M24 |
| ZBW-LICENSE-007 | deterministic M01/M02 CycloneDX SBOMs, build notices and asset provenance cross-gates | notice/provenance policy | M02 validator / maintainer | — | notice/SBOM schemas | approved/blocked counts | regeneration/staleness/licence CI | root/build notices + M02 implementation; M01/M02/M24 |

## Consolidated pre-code readiness decisions (20)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-READY-001 | exact multi-JDK archives and class-major policy; platform-free planned graph; five-JDK empty reactor verified | toolchain/module manifests | M1 validator / maintainer; runtime command later | — in M01 | RuntimeCapability API remains M02/M06 | — | five clean builds + graph/bytecode tests; boot CT M22 | runtime matrix + ADR-0006 + implementation guide; M01/M22 |
| ZBW-READY-002 | 22-row server registry fixed with every certification `NOT_STARTED`; no unsupported version claim | private fixture/fallback manifests | M1 validator / maintainer; certify later | — in M01 | report schema only; API later | — | inventory/hash/no-claim tests; lifecycle E2E M22 | runtime/fallback matrices + implementation guide; M01/M22 |
| ZBW-READY-003 | M02 generic stable-ID content registry/pack contracts only; 300 cosmetic definitions/batches remain M14 | typed content/provenance metadata foundation | audit command later | catalogue/provenance/preview later | registry/provider base contracts | none in M02 | generic duplicate/order/version contract tests; full count/visual/PT M14/M24 | `API_M02.md`; M02 foundation / cosmetic plan M14 |
| ZBW-READY-004 | M02 capability IDs/sets and extension capability declarations only; DSL/schema/auth/workers remain M03/M05/M11 | capability metadata only | scripts commands later | script review later | capability contract foundation | none | capability duplicate/containment/metadata tests | `API_M02.md`; M02 foundation / scripting security later |
| ZBW-READY-005 | M01 14 tool artifacts plus M02 exact lock of 170 Maven build/test components and 535 files, generated SBOM/notices, offline restore/build | acquisition/lock policy | Maven lock `capture|validate|seed|restore` / maintainer | — | CycloneDX/lock schemas | build versions only | exact hash/licence/source/rights/staleness/offline gates | dependency audit + M02 implementation; M01/M02 VERIFIED |
| ZBW-READY-006 | exact public fixture hashes plus explicit private legacy lock states in 22-row manifest | `private-runtime-fixtures.json` | M1 validator / maintainer; runtime verify later | — in M01 | fixture report schema; API later | no support placeholder in M01 | hash/registry/no-binary tests; boot/game CT M22 | runtime matrix + implementation guide; M01/M22 |
| ZBW-READY-007 | M02 vendor-neutral event-push `AntiCheatProvider`, normalized alert/severity/subscription contracts; Grim/Vulcan/no-provider adapters remain M21 | no runtime provider/secret config in M02 | diagnose later | health GUI later | neutral provider and alert contracts | sanitized health later | alert bounds/null/type/thread metadata tests; adapter CT M21 | `API_M02.md`; M02 SPI foundation / M21 adapters |
| ZBW-READY-008 | exact BOM/plugin properties plus M02 five-module manual-DI graph, capability/provider SPIs, locked Maven graph and architecture checks | parent/BOM/quality/module/lock manifests | deterministic validators / maintainer | — | provider/extension/content SPIs; command/GUI later | — | dependency graph, forbidden imports, no globals/service locator, licence and binary tests | dependency audit + ADR-0007 + M02 implementation; M01/M02 VERIFIED |
| ZBW-READY-009 | benchmark thresholds remain immutable; M1 supplies deterministic result/gate schema only, not feature benchmarks | `quality-policy.json`, benchmark specification | M1 validator / maintainer; benchmark commands M24 | — in M01 | BenchmarkResult SPI remains later | — | deterministic gate/schema tests; workload PT at feature/M24 milestones | benchmark baseline + ADR-0009 + implementation guide; M01/M24 |
| ZBW-READY-010 | purpose-classified replay schema/encryption/access | replay privacy profile, chat off | replay privacy/export / `.privacy.replay.*` | replay privacy/export | ReplayPrivacy API/events | none — private | no-chat/access/export/encryption ST | privacy policy + ADR-0010; M03/M17 |
| ZBW-READY-011 | M04 retention/hold/release/tombstone schema and repository; scheduler/encrypted feature stores remain M17/M18 | retention classes | privacy commands remain M09/M17 | workflow remains M17/M18 | `RetentionPolicy`/`RetentionRepository` | none — private | duplicate/transaction/privacy storage IT; clock/delete/backup later | privacy policy + M04 API; M04/M17/M18 |
| ZBW-READY-012 | zero-asset provenance manifest, repository binary/resource scan and 49/473 clean-room coverage preservation | asset/dependency provenance manifests | M1 validator / maintainer | — (file report) | provenance machine schema; API later | approved/blocked counts in manifest | repository/content/hash/coverage scan | project licensing/provenance + implementation guide; M01/M23/M24 |
| ZBW-READY-013 | authenticated envelope/key/dedupe services | network security/SecretRefs | network peer/key rotate/diagnose / `.network.*` | peer/key/security report | SecureEnvelope API/events | none — security | forge/replay/rotation/fuzz ST | network security + ADR-0011; M03/M19/M20 |
| ZBW-READY-014 | M04 SQL authority, atomic outbox/inbox and SQLite single-JVM/single-writer topology guard; leases/Redis remain M19/M20 | deployment/database/redis | diagnose commands remain M09/M19 | sanitized pool health foundation | transaction/message contracts | health only | duplicate/retry/concurrency IT; partition/lease later | network security + M04 pool/query docs |
| ZBW-READY-015 | immutable balance profile registry/migration | balance/content/modes/shops | balance validate/activate / `.content.balance.*` | balance editor/preview | BalanceProfile API/events | active version | schema/golden/simulation/PT | balance baseline + ADR-0014; M03/M10/M11 |
| ZBW-READY-016 | M04 encrypted/validated backup evidence coordinator, zero-loss/15-minute objectives and pool health; quota/degradation runtime remains M05/M24 | operations/backup/quotas | backup/restore drill commands remain M09/M24 | recovery/pool health foundation | `RecoveryService`/`SqlRecoveryCoordinator` | sanitized health only | evidence/failure tests; real restore/RPO/RTO drills later | operational defaults + M04 runbook; M04/M05/M24 |
| ZBW-READY-017 | two M02 CI workflows; exact quality manifest; static, coverage, binary, architecture, dependency and docs gates | quality policy, POM profiles, pinned workflows | M02 validator / maintainer | — | binary/API and lock/SBOM reports | — | 24 Java tests on five JDKs, 13 governance tests, zero static findings, enforced coverage; mutation later | quality gates + ADR-0009 + M02 implementation; M01/M02/M24 |
| ZBW-READY-018 | visibility policy/tombstone/consent migration | privacy visibility defaults | privacy visibility/export / `.privacy.*` | player privacy + staff restricted views | PrivacyPreference/events | consented public fields only | permission/privacy E2E/ST | privacy policy; M03/M15/M17/M18 |
| ZBW-READY-019 | M1 build/CI rights classifier and proprietary product-release posture enforced; executed public terms remain release evidence | licensing/acquisition policy | dependency/M1 validators / maintainer | — | build rights metadata; SDK metadata M23 | — | artifact/notice/binary/legal-policy checklist | licensing recommendation + ADR-0016 + implementation guide; M01/M23/M24 |
| ZBW-READY-020 | addon catalogue validator plus zero-asset/binary provenance gates retain all 49 references and 473 clean-room rows | addon/asset provenance manifests | M1 validator / maintainer | generated addon report | addon metadata report | 49/473 coverage counts | exact addon/provenance/legal CI | addon catalogue + ADR-0013 + implementation guide; M01/M24 |

## M02 foundational allocation for continuing requirements

These rows record only the contracts delivered by M02. They do not mark later configuration, GUI, command, permission, placeholder, content-production or runtime behavior complete.

| Requirement | M02 implementation | Configuration / GUI / commands / permissions / placeholders | Tests and evidence | Remaining owner |
|---|---|---|---|---|
| ZBW-CONTENT-001..011 | Stable content/pack/resource/definition IDs, generic registry/provider/version metadata and immutable assembly policy support every listed content family without embedding assets or balances | All runtime content files and product surfaces remain their assigned M03/M06/M10–M14/M20 work | Registry duplicate/order/version/immutability and zero-product-bundle gates | Per-family milestones and M24 provenance/release gate |
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
| M03 / ZBW-CONTENT-002..009 | Stable configurable starter profile IDs for shop, modes, quests, achievements, battle pass and cosmetics; 300-cosmetic gate; private/effect compatibility metadata | identifier/type/default/range/reference tests | M06/M10–M14/M20 definitions and runtime |
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

## Part II — Atomic source-to-PRD coverage matrix

`docs/MASTER_PROMPT_COVERAGE.md` is incorporated here by reference as Part II. Each row retains the original source assertion and maps it to one or more Part I IDs and PRD sections. The generated report is the authoritative detailed-child check required by ZBW-GOV-011 and ZBW-QA-007.

## Part III — Native addon atomic feature matrix

`docs/ADDON_FEATURE_CATALOG.md` is incorporated here by reference as Part III. Its 473 `ZBW-ADDON-*` rows are semantic requirements, not informative examples. Every row maps its atomic capability to PRD §4.17/§8.9, the retained Part I overlap, milestone, module, configuration, GUI, commands, permissions, API/events, PlaceholderAPI, performance, security, tests and documentation. The catalogue's `Trace entry` cell is the canonical matrix location for that ID. All 49 addon references remain explicit even where their behavior overlaps a core requirement.

## Source-to-PRD coverage audit

This table is the human-readable range summary of the full beginning-to-end review of `MASTER_PROMPT.md`; it does not replace Part II's atomic rows. Line ranges use the baseline file read for M00; if the source changes, regenerate both audits before implementation.

| Master lines | Source themes | Consolidated requirement IDs |
|---|---|---|
| 1–155 | Product vision, originality, enterprise quality, PRD contract, measurable/unambiguous requirements, no placeholders, consistency and non-reduction | GOV-001..010, OPS-009, QA-006 |
| 156–409 | Engineering principles/modules/dependencies, threading/performance, DB/config/docs/commands/permissions/GUI/tests/integrations/map identity/failure/Definition of Done | ARC-001..009, DEPLOY-007, OPS-001..006, UX-001..005, QA-001, ARENA-002/003 |
| 410–641 | Milestones/order/repository/branching, self-review/correction, traceability, autonomous improvements, reviews/release/final statuses | GOV-003/007/008/010, QA-001/004/005/006, OPS-008/009 |
| 642–926 | Core gameplay, all modes, arena/map/duplicate/world/lobby/selectors/hotbars/setup/validator | GAME-001..010, ARENA-001..009, UX-001/003/004 |
| 927–1196 | Shop catalog/config/GUI/API/currencies/purchases, upgrades, generators, utility/custom items/shopkeepers/performance | SHOP-001..007, INT-006, UX-001 |
| 1197–1582 | Unified progression, XP/levels/prestige/currency, cosmetics categories/definitions/rarities/ownership/equipment/GUI/performance/API | PROG-001..008, PROG-014 |
| 1583–2337 | Quests/objectives/progress/rewards/admin/API, achievements, challenges, battle pass, reward engine/summary/calendar/profile/settings/storage/placeholders/commands/permissions/editors/migration/performance/security | PROG-009..014, DEPLOY-007, PAPI-003, ECO-001, QA-001 |
| 2338–2901 | Replay identity/capture/accuracy/viewer/speeds/timeline/telemetry/GUI/access/staff/annotations/storage/retention/recovery/performance/privacy/commands/permissions/API | REPLAY-001..010, OPS-002/005/006 |
| 2902–3588 | Atlas eligibility/permissions/cases/anonymization/review/verdict/reasons/profile/reputation/accuracy/anti-abuse/interactions/rewards/GUI/commands/staff/punishment/API/placeholders/performance and Grim/Vulcan alert integration | ATLAS-001..013, INT-008, PAPI-004, PROG-011 |
| 3589–4266 | Authoritative stats/dimensions/ratios/streaks/storage/admin/GUI/API, leaderboards, complete PlaceholderAPI families/context/performance/format/admin/dev API, Discord/external API/privacy/migration/testing | STATS-001..008, PAPI-001..006, OPS-007, ECO-001, QA-003 |
| 4267–4755 | Shared/proxy modes, Velocity/Bungee/CloudNet/Redis/DB, Placeholder/Vault/LuckPerms/packet/world/NPC/hologram/party/anticheat/Via/Geyser/Floodgate | DEPLOY-001..009, INT-001..010, ARC-007, UX-006 |
| 4756–5279 | Global/player/admin GUI and editor, command/player/staff/admin inventories, permission actions/Atlas nodes, public/event APIs, configuration files/comments/validation/reload, localization | UX-001..006, OPS-001..004, ARC-003/004, ATLAS-011 |
| 5280–5592 | Documentation/reference deliverables, automated/gameplay/performance tests, CI/build artifacts, installation validator/Doctor, delivery and final compliance | OPS-003/006/008/009, QA-001..006, GOV-007/009 |
| 5593–6276 | Governance/ownership/ID/status/traceability/change/dependencies/risk/phases/debt/backward compatibility/versioning/review/health, deliverables, compatibility/performance/security reports, ecosystem/marketplace/SDK/migration/AI evolution | GOV-001..010, ARC-008/010, OPS-006/008/009, QA-005/006, ECO-001..005 |
| 6277–6470 | Highest-priority priority taxonomy, dependency declarations, complete DoD, UX/accessibility, ADRs, observability, secret management, no duplication, failure handling and maintainability | all IDs via §2–§3 profiles; specifically GOV-004..007, ARC-001/005, UX-001/006, OPS-002/005/006, QA-006 |

## M04 foundational allocation for continuing requirements

These rows close only generic durable-storage foundations. Feature schemas, serializers, commands, GUIs, permissions, APIs/events, placeholders and gameplay/runtime behavior remain with their named later milestones.

| Requirement | M04 implementation | Configuration/surfaces | Tests and evidence | Remaining owner |
|---|---|---|---|---|
| M04 / ZBW-PROG-001..014 | typed record key/revision/payload, atomic UoW, outbox/idempotency, retention/tombstone and recovery ports can persist future progression aggregates without defining them | M03 database/security settings; no progression GUI/command/API/placeholder added | primitive/CRUD/conflict/rollback/outbox/privacy SQLite suites; external DB suite pending | M12/M13 feature model, rewards, migration and surfaces |
| M04 / ZBW-STATS-001..008 | authoritative versioned aggregate envelope and transaction/idempotency foundation only | M03 statistics/database settings; no stats query/leaderboard surface | record revision, transaction, restart and dedupe tests | M15 dimensions, aggregation, leaderboards, privacy and migration |
| M04 / ZBW-REPLAY-001..010 | metadata envelope, recovery objective, retention/hold/tombstone and backup evidence foundation; no replay content is stored | M03 replay/privacy settings; no replay GUI/command/API/placeholder | checksum/recovery/privacy/cache invalidation foundations | M17 capture, chunks, viewer, encryption, quota and access |
| M04 / ZBW-ATLAS-001..013 | case-key-capable durable envelope, legal hold/release and identity-separated generic schema only | M03 Atlas/privacy settings; no Atlas workflow/surface | hold/release/duplicate/tombstone and authorization-record storage tests | M18 cases, review, anti-abuse, reputation and identity vault |
| M04 / ZBW-READY-011 | retention policy, retention rows, authorized hold/release and content-free tombstones | retention classes from M03; commands/GUI remain later | duplicate-safe atomic SQLite privacy suite | M17/M18 scheduler, encryption, deletion/export workflows |
| M04 / ZBW-READY-014 | SQL authority, SQLite topology guard, atomic outbox/inbox, bounded retries and sanitized pool health | M03 deployment/database/redis schema | crash/retry/idempotency/concurrency/migration tests; external containers pending | M19/M20 leases, Redis/proxy partitions and runtime health |
| M04 / ZBW-READY-016 | encrypted validated backup-evidence coordinator and SQL zero-RPO/15-minute-RTO declarations | operational defaults; secrets supplied through provider driver | provider failure/evidence tests and backup runbook | M05/M24 quotas, degradation, real restore and RPO/RTO drill evidence |
