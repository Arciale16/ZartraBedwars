# ZartraBedWars Requirements Traceability Matrix

## Rules and legend

This baseline contains 652 stable semantic requirement IDs. Part I contains exactly one row for each of the 179 PRD IDs (144 original core IDs plus 35 accepted owner-decision IDs). Part II is the normative atomic matrix in `docs/MASTER_PROMPT_COVERAGE.md`, containing one `MP-L####` row for every non-empty source assertion in `MASTER_PROMPT.md`. Part III is `docs/ADDON_FEATURE_CATALOG.md`, containing one complete mapping row for each of the 473 owner-supplied native-addon requirements. Locations are planned until implementation; replace plans with exact package/class/table/key/file/test links as each milestone starts. A dash means the surface has no direct user-facing behavior and must include a reason; it never omits documentation or verification.

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
| ZBW-GOV-003 | requirement registry and dependency graph | — | validation / maintainer | health dashboard later | requirement model | health counts | orphan/duplicate ID test | traceability guide; M00/M01 |
| ZBW-GOV-004 | ADR repository/checker | — | — | — | ADR metadata | — | ADR completeness lint | `docs/DECISIONS`; M00 |
| ZBW-GOV-005 | architecture rules/build checks | quality config | — | — | — | — | architecture/static tests | architecture; M01 |
| ZBW-GOV-006 | style/static analysis | Checkstyle/SpotBugs | — | — | — | — | quality gates | developer guide; M01 |
| ZBW-GOV-007 | Definition-of-Done/compliance generator | release policy | compliance / maintainer | compliance dashboard | compliance report API | requirement status | release gate tests | release/compliance guide; M24 |
| ZBW-GOV-008 | milestone review workflow | CI policy | — | — | — | — | clean-build gate | milestones/contribution; all |
| ZBW-GOV-009 | license/assets scan and notices | license allowlist | licenses / maintainer | license report | — | — | license/original-asset scan | notices/legal guide; M01/M24 |
| ZBW-GOV-010 | requirement discovery workflow | — | — | — | — | — | review checklist | governance guide; all |
| ZBW-GOV-011 | atomic source catalog, source hash and Java-work gate | coverage-report metadata; Python 3.11+ stdlib tool | coverage verify / maintainer | coverage summary | coverage schema/read API later | coverage percentage/counts | source/hash/ID/category/status self-check | `docs/MASTER_PROMPT_COVERAGE.md`; M00/M01/M24 |

## Architecture foundations (10)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ARC-001 | Maven modules + architecture rules | build/module metadata | modules / debug | module health | module lifecycle | module health | cycle/forbidden dependency | architecture; M01 |
| ZBW-ARC-002 | `zbw-domain`, `zbw-compat-*` boundary | compatibility selection | compat status / debug | compatibility view | `CompatibilityApi` | server/client version | forbidden import + CT | compatibility dev guide; M02/M06 |
| ZBW-ARC-003 | `zbw-api`, SDK, binary baseline | API version policy | api info / developer | API browser later | all public services | api version | binary/source contract | API guide/JavaDoc; M02 |
| ZBW-ARC-004 | event envelope/bus in application | event queue limits | event debug / debug | event diagnostics | `EventApi`, all events | safe event metrics | order/thread/cancel tests | event guide; M02 |
| ZBW-ARC-005 | scheduler port/thread guards | executor/deadline config | threads / debug | thread dashboard | `SchedulerApi` | thread/queue health | owner-thread tests | threading guide; M05 |
| ZBW-ARC-006 | bounded work/lifecycle drain | queue/backpressure config | queues / admin.debug | queue dashboard | work/lifecycle API | queue sizes | saturation/cancel/shutdown | operations guide; M05 |
| ZBW-ARC-007 | provider SPIs/registry | provider selection | integration status/reload / manage | integration manager | provider APIs/events | provider health | provider contract suites | provider dev guide; M02/M06 |
| ZBW-ARC-008 | schema/version registry + migrations | version sections | version,migrate / admin.migrate | migration/version views | version/migration APIs | versions | rolling/migration CT | compatibility/migration guide; M04 |
| ZBW-ARC-009 | typed ID generator/registry; ID mapping | ID policy | inspect ID / inspect | inspectors | identity APIs/events | public IDs | uniqueness/import/clone MT | identity guide; M02/M07 |
| ZBW-ARC-010 | extension registries/metadata | extension settings | extensions / developer.manage | extension manager | registry SPIs | extension health | example extension CT | SDK/marketplace guide; M02/M23 |

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
| ZBW-DEPLOY-007 | SQL repositories/migrations/backups | database/security | database manage/migrate / database.* | database manager | Storage/Migration APIs | database status | DB/MT/PT/ST | database guide; M04 |
| ZBW-DEPLOY-008 | distributed envelope/inbox/outbox | deployment/redis/database | message debug / debug | protocol health | Message API/events | protocol versions | order/rolling/duplicate | protocol guide; M04/M19 |
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
| ZBW-UX-004 | authorization policy/node registry/aliases | permissions/security | permission inspect / permission.* | permission inspector | Permission API/events | — | full matrix/escalation ST | permission reference; M03/M09 |
| ZBW-UX-005 | message catalogs/renderers/import/export | messages | language/reload / language.* | language/editor/report | Localization API/events | locale-aware | fallback/completeness/CT | localization guide; M03/M22 |
| ZBW-UX-006 | accessibility/capability patterns | gui/messages | settings / player | accessible alternatives | capability API | settings | task usability/Bedrock E2E | accessibility matrix; M09/M22 |

## Operations, security and delivery (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-OPS-001 | typed config schemas/docs/version migration | all listed files | config validate/reload / config.* | config editor | Configuration API/events | config version/health | schema/doc/MT | configuration guide; M03 |
| ZBW-OPS-002 | secret references/redaction/export allowlist | security + secret refs | security diagnose / admin | security status | SecretRef/redaction API | none | seeded-secret ST | security guide; M03/M24 |
| ZBW-OPS-003 | startup/install/config validators | all configs | validate/doctor / validate | validator/dashboard | Validation API/events | validation status | mutation/version tests | install guide; M03 |
| ZBW-OPS-004 | transactional targeted reload plans | relevant files | targeted reload / reload.* | reload controls | Reload API/events | reload state | partial-failure E2E | reload guide; M03 |
| ZBW-OPS-005 | error taxonomy/retry/circuit/alerts | performance/security/messages | diagnostics/recover / admin | health/error panels | Failure API/events | sanitized health | fault injection | recovery guide; M05 |
| ZBW-OPS-006 | metrics/health/Doctor/debug exports | performance/security | health,performance,doctor / admin.* | all health dashboards | Health/Performance APIs | safe health families | cardinality/redaction/PT | operations/tuning; M05/M24 |
| ZBW-OPS-007 | external stats/Discord providers | integrations/security | external-api manage / api.* | integration manager | ExternalStats SPI/API | only scoped public | auth/rate/privacy ST/PT | external API guide; M16/M21 |
| ZBW-OPS-008 | Maven/CI/package/release artifacts | build/workflows | — | — | published API artifact | build/version | clean/repro/security CI | CI/release guide; M01/M24 |
| ZBW-OPS-009 | documentation inventory/generators | docs config | docs generate / maintainer | doc links | generated API docs | generated PH docs | link/inventory lint | all guides; continuous |

## Quality (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-QA-001 | testkit/suite taxonomy/CI | test profiles | — | — | test fixtures only | — | all suite types | testing guide; all |
| ZBW-QA-002 | gameplay E2E scenario matrix | test fixtures | — | tested GUIs | tested APIs/events | tested mode PH | full gameplay matrix | test report; M10/M24 |
| ZBW-QA-003 | replay/Atlas/stat/rank/PH suites | test fixtures | — | tested surfaces | tested APIs | full PH test | mandated cases | test report; M16–M18 |
| ZBW-QA-004 | benchmark/load harness + result store | benchmark manifest | benchmark run / developer | performance dashboard | benchmark API | performance metrics | all workload PT | benchmark report; M24 |
| ZBW-QA-005 | release static/inventory/vulnerability gates | CI policy | — | — | API compatibility gate | PH inventory gate | release gates | release checklist; M24 |
| ZBW-QA-006 | compliance report generator | release metadata | compliance / maintainer | compliance dashboard | compliance schema | status counts | 652-semantic-ID audit plus atomic annex evidence | final compliance; M24 |
| ZBW-QA-007 | deterministic atomic coverage verifiers | Master/addon/decision manifest rules | coverage verify / maintainer | combined coverage reports | coverage report schema | coverage percentage/counts | 6,438 Master + 473 addon + 35 decision rows and document/ADR gate | coverage audits; M00/M01/M24 |

## Ecosystem and migration (5)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-ECO-001 | migration framework/adapters/maps/reports | migration/integrations | migrate/import/export / migrate.* | migration manager | Migration API/events | migration progress | fixture dry-run/rollback MT | migration guide; M23 |
| ZBW-ECO-002 | module metadata/compat validator | extensions | extensions validate / developer | module manager | Marketplace metadata API | extension status | sample module CT | marketplace guide; M23 |
| ZBW-ECO-003 | SDK/example/API deprecation tooling | API version policy | api/deprecation report / developer | API browser | SDK/public API | api version | binary/deprecation CT | SDK guide; M02/M23 |
| ZBW-ECO-004 | migration/Doctor provider registries | migration/doctor/performance | doctor/migrate / admin | Doctor/migration | Doctor/Migration SPIs | health | provider extension CT | operations SDK; M23 |
| ZBW-ECO-005 | AI suggestion-only ports/policy | security/integrations (disabled) | future provider manage / admin | reviewed suggestion surface | AI suggestion SPI | none | threat/design CT before enable | future API policy; post-M24 MAY |

## Original content and provenance (11)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-CONTENT-001 | clean-room/content-rights gate | content/licence allowlists | content audit / maintainer | provenance report | content metadata API | provenance status only | copied-content/hash scan | starter catalogue + provenance; M01/M24 |
| ZBW-CONTENT-002 | shop balance definition pack | `content.yml`, `shops.yml` | content shop validate/preview / content.manage | shop profile editor | BalanceProfile API/events | active shop profile | schema/golden/E2E | original starter catalogue; M11 |
| ZBW-CONTENT-003 | per-mode balance definition packs | `content.yml`, `modes.yml` | content mode validate/preview / content.manage | mode balance editor | ModeBalance API/events | active mode profile | mode matrix/PT | original starter catalogue; M10/M11 |
| ZBW-CONTENT-004 | quest seed definitions | `quests.yml`, `content.yml` | quest validate/import / quest.manage | quest editor | Quest registry/events | quest ID/progress | objective/config E2E | original starter catalogue; M13 |
| ZBW-CONTENT-005 | achievement seed definitions | `achievements.yml`, `content.yml` | achievement validate/import / achievement.manage | achievement editor | Achievement registry/events | achievement ID/tier | tier/MT/E2E | original starter catalogue; M13 |
| ZBW-CONTENT-006 | starter season/tier definitions | `battlepass.yml`, `content.yml` | pass validate/import / battlepass.manage | pass editor | BattlePass registry/events | season/tier/track | claim/rollover/MT | original starter catalogue; M13 |
| ZBW-CONTENT-007 | cosmetic seed + 300-definition gate | `cosmetics.yml`, content packs | cosmetics validate/import / cosmetics.manage | cosmetics/provenance editor | Cosmetic registry/events | catalogue/provenance counts | 300-count/hash/PT | starter/provenance guides; M14 |
| ZBW-CONTENT-008 | private preset definitions | private-games/content config | private preset validate/preview / private.manage | host/admin preset editor | PrivatePreset API/events | active preset/multipliers | GUI/generator E2E | Resource Scarcity catalogue; M20 |
| ZBW-CONTENT-009 | semantic feedback/effect definitions | content/compatibility/messages | effect validate/preview / content.manage | effect preview | SemanticEffect API/events | selected effect/fallback | adapter/usability CT | starter + fallback matrix; M06/M14 |
| ZBW-CONTENT-010 | content-pack registry/version/migration | `content.yml`, pack manifests | content pack validate/reload/migrate / content.manage | content pack manager | ContentPack SPI/events | pack version/health | extension/reload/MT CT | content SDK; M02/M03 |
| ZBW-CONTENT-011 | asset provenance/hash registry | provenance manifest | provenance audit / maintainer | provenance report | provenance schema/report API | approved/blocked counts | file/hash/licence CI | `docs/ASSET_PROVENANCE.md`; M01/M14/M24 |

## Discord provider topology (8)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-DISCORD-001 | scoped integration gateway/event outbox | discord event allowlists/scopes | discord status/debug / admin.discord | integration diagnostics | DiscordIntegration API/envelopes | safe delivery health | schema/scope/idempotency ST | Discord architecture; M02/M16 |
| ZBW-DISCORD-002 | provider SPI/registry/lifecycle | provider selection/capabilities | provider list/reload/drain / admin.discord | provider manager | DiscordProvider SPI/events | provider capability/health | provider contract/PT | provider SDK; M02/M16 |
| ZBW-DISCORD-003 | embedded webhook adapter | destination/event/rate + SecretRef | webhook validate/test / admin.discord.webhook | webhook settings/status | webhook delivery events | webhook health only | timeout/rate/allowlist ST | webhook guide; M16 |
| ZBW-DISCORD-004 | external bot transport + link records | transport/scopes/link policy + secrets | link/unlink + bot diagnose / discord.* | link/privacy/bot health | external protocol/link events | consented link state | auth/replay/link CT/ST | external bot protocol; M16 |
| ZBW-DISCORD-005 | custom provider SDK/metadata | extension metadata | provider validate / developer.manage | extension manager | custom provider SPI | provider health only | sample provider CT/ST | custom provider guide; M16/M23 |
| ZBW-DISCORD-006 | disabled/no-op provider | `enabled: false` default | discord status / use | disabled state | no-op provider | disabled | no-provider full E2E | install/default guide; M03/M16 |
| ZBW-DISCORD-007 | bounded outbox/retry/circuit/dead letter | queue/deadline/backoff/rate budgets | discord retry/drain / admin.discord | queue/circuit dashboard | delivery/failure events | queue/circuit health | outage/saturation/PT | failure runbook; M05/M16 |
| ZBW-DISCORD-008 | secret resolution/redaction/rotation | secret references/env names only | secret diagnose/rotate / admin.security | redacted status only | SecretRef integration | — (secrets prohibited) | seeded-secret/export ST | security/Discord guides; M03/M16 |

## Mandatory Minecraft 1.8 fallbacks (9)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-COMPAT-001 | separate legacy artifact/toolchain/bootstrap | version matrix/toolchains | compat status / debug | compatibility report | capability/version API | server/adapter version | 1.8 startup/gameplay | fallback matrix; M01/M22 |
| ZBW-COMPAT-002 | `zbw-compat-v1_8` adapter only | adapter selection | compat inspect / debug | capability diagnostics | Compatibility SPI | capability health | forbidden import/startup CT | compatibility architecture; M06 |
| ZBW-COMPAT-003 | legacy material/data/NBT registry | material fallbacks | compat material test / debug | mapping inspector | material/item capability API | fallback material ID | round-trip/tamper/MT | fallback matrix; M06/M22 |
| ZBW-COMPAT-004 | legacy particle renderer | particle fallbacks/budgets | compat particle preview / debug | effect preview | particle capability API | fallback/suppression state | packet/budget/usability CT | fallback matrix; M06/M22 |
| ZBW-COMPAT-005 | legacy sound renderer | sound fallbacks/volume | compat sound preview / debug | sound preview | sound capability API | fallback/suppression state | mapping/usability CT | fallback matrix; M06/M22 |
| ZBW-COMPAT-006 | legacy text/action renderer | text/action fallback rules | compat text preview / debug | rendered preview | text capability API | renderer class | task/format/Unicode E2E | fallback matrix; M06/M22 |
| ZBW-COMPAT-007 | entity/packet/GUI/input fallback providers | entity/packet/gui mappings | compat component inspect / debug | component diagnostics | packet/entity/UI capabilities | capability/fallback | packet/cleanup/input E2E | fallback matrix; M06/M22 |
| ZBW-COMPAT-008 | typed degradation/last-known-good policy | suppression/fallback order | compat validate / admin.validate | validation report | fallback decision events | suppression reason | mutation/fault CT | compatibility policy; M06/M22 |
| ZBW-COMPAT-009 | generated complete fallback inventory | compatibility manifest | compat report / maintainer | compatibility matrix | report schema | coverage counts | row/fixture/link validator | `docs/COMPATIBILITY_FALLBACKS.md`; M01/M22/M24 |

## Dependency licensing and redistribution (7)

| Requirement | Planned implementation and data/migration | Configuration | Cmd / permission | GUI | API / events | PH | Tests | Documentation / milestone |
|---|---|---|---|---|---|---|---|---|
| ZBW-LICENSE-001 | exact dependency/SBOM audit registry | approved dependency policy | licences audit / maintainer | licence report | SBOM/report schema | — (build governance) | inventory/transitive scan | dependency audit; M01/M24 |
| ZBW-LICENSE-002 | version/checksum lock and drift gate | lock/update policy | dependencies verify / maintainer | dependency diff | dependency metadata API | build version only | dynamic/drift CI | dependency audit; M01 |
| ZBW-LICENSE-003 | default-deny redistribution classifier | licence allow/deny policy | licences validate / maintainer | distribution decision report | — (governance only) | — (governance only) | release artifact scan | licensing guide; M01/M24 |
| ZBW-LICENSE-004 | shading/modification/obligation scanner | relocation/modification declarations | licences shade-report / maintainer | shaded-content report | SBOM relations | — (build governance) | package/source-offer scan | dependency audit/notices; M01/M24 |
| ZBW-LICENSE-005 | compile-only/provided integration scopes | dependency scopes/provider versions | integrations validate / admin | integration manager | provider SPIs | provider presence/version | artifact/scope/provider CT | integration guides; M01/M21 |
| ZBW-LICENSE-006 | proprietary binary repository/artifact denylist | binary/hash deny rules | licences proprietary-scan / maintainer | violation report | — (governance only) | — (security) | repository/release scan | contributor/legal guide; M01/M24 |
| ZBW-LICENSE-007 | generated notices/SBOM/provenance release gate | notice/provenance policy | licences notices/verify / maintainer | compliance report | notice/SBOM schemas | approved/blocked counts | reproducibility/licence CI | `THIRD_PARTY_NOTICES.md`; M01/M24 |

## Coverage controls

The following checks are required from M01 onward:

1. Extract the 179 Part I IDs from this file/PRD and the append-only `ZBW-ADDON-001..473` Part III IDs from the addon catalogue; fail on a missing, duplicate or extra semantic ID. The generated catalogue may display an append-only amendment beside its owning addon, but numeric allocation history remains immutable and validator-sorted.
2. Fail when an implementation milestone closes while its rows still contain only generic planned locations.
3. Generate command, permission, GUI, config, API/event and placeholder inventories and compare them with the exact references derived from this matrix.
4. Require test and documentation links for every final compliance row.
5. Regenerate Part III with `tools/coverage/generate_addon_feature_catalog.py`; fail unless all 49 references, 8/41 tier split, 473 atomic IDs, required mapping surfaces and `COVERED` statuses match.
6. Run `tools/coverage/validate_preimplementation_decisions.py`; fail unless all 35 Part I decision IDs, `ZBW-ADDON-464..473`, five accepted ADRs, required documents/fields and resolved RC-072..076 rows match.
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
