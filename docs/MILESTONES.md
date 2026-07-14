# ZartraBedWars Dependency-Aware Milestones

## Rules

Each milestone is a small reviewable merge unit. Tests and documentation are produced with the feature, not postponed to the final testing/documentation milestones. A milestone exits only with a clean build, all applicable tests, traceability updates, no production TODO/stub and evidence for its core and `ZBW-ADDON-*` IDs. Later milestones may not bypass an unmet dependency.

The master prompt's sequential “documentation/testing near the end” list is treated as final hardening order; continuous testing and documentation requirements have higher safety value and apply to every milestone.

## Plan

### M00 — Requirements, architecture and pre-code decision baseline

- **Requirements:** ZBW-GOV-001..011, ZBW-QA-007, ZBW-READY-001..020, architecture/plan portions of all 672 semantic IDs.
- **Entry:** `MASTER_PROMPT.md` available and read completely.
- **Deliver:** PRD, architecture, milestones, requirement-level traceability, deterministic Master/addon/decision coverage reports; all 25 pre-code decision outcomes, sixteen ADRs and normative runtime/performance/privacy/network/scripting/licensing/content/operations/quality specifications; risks/conflicts and repository instructions.
- **Exit:** 672 unique semantic IDs (199 Part I plus 473 addon); Parts I–III and all decision-document inventories validate; source hash/line count, 49-addon inventory, 8/41 split, sixteen accepted ADRs and required schemas match; all atomic rows are `COVERED`, combined functional coverage is 100%, the pre-code readiness report has no unresolved pre-code decision and no Java source/build scaffold exists.

### M01 — Materialize the accepted build governance baseline

- **Requirements:** GOV-003..011, ARC-001/002/008, OPS-008, QA-001/005/007, CONTENT-001/011, COMPAT-001/009, LICENSE-001..007, READY-001/002/005/006/008/009/012/017/019/020.
- **Entry:** M00 PRE-CODE READY baseline approved; exact selections and architecture are not reopened without an ADR.
- **Deliver:** Maven parent/BOM/wrapper/toolchains; pre-resolution checksum/licence lock generator; SBOM/notices/provenance gates; empty module graph; pinned validator runtime; CI, static architecture/bytecode/quality rules and private runtime-fixture manifest.
- **Exit:** clean empty multi-module build on all compile JDKs; all documentation/licence/provenance/dependency-lock gates pass; every resolved build artifact has exact checksum/licence evidence; runtime/provider matrix is immutable; no functional production path is claimed.

### M02 — Public API, domain primitives and extension metadata

- **Requirements:** ARC-003/004/009/010, ECO-002/003, CONTENT-010, DISCORD-001/002/005.
- **Entry:** M01 module/API ADR accepted.
- **Deliver:** typed IDs, clocks/results/events, public API versioning, provider/extension contracts and metadata validator.
- **Exit:** binary/API contract tests and example metadata pass; no Bukkit/store dependency in domain/API; JavaDoc documents thread/error/version rules.

### M03 — Configuration, localization, permissions and validation foundations

- **Requirements:** OPS-001..004, UX-004/005, OPS-002/003, DISCORD-006/008, content/compatibility schema foundations.
- **Entry:** M02 types/events stable enough for schemas.
- **Deliver:** typed versioned configuration, comments/docs generator, transactional targeted reload, localization and authorization ports, startup/manual validator.
- **Exit:** malformed/unknown/dependency/secret cases tested; every initial option has metadata; reload never partially applies; permission checks are centralized.

### M04 — Storage, migrations, outbox and cache foundations

- **Requirements:** DEPLOY-007/008, ARC-008, data portions of PROG/STATS/REPLAY/ATLAS.
- **Entry:** M01 DB ADR and M02 IDs/events accepted; M03 secret/config services usable.
- **Deliver:** repository/unit-of-work APIs; SQLite/MySQL/MariaDB adapters; Hikari; migrations; backup/restore; inbox/outbox; bounded local cache.
- **Exit:** container/SQLite contract suites, crash/retry/idempotency/concurrency/migration tests pass; query plans and pool metrics documented; zero SQL in feature/platform classes.

### M05 — Scheduler, lifecycle, observability and failure substrate

- **Requirements:** ARC-005/006, OPS-005/006, GAME-010.
- **Entry:** M02 application ports and M03 config exist.
- **Deliver:** owner-thread dispatch, bounded executors/queues, lifecycle drain, health/metrics, sanitized diagnostics, Plugin Doctor extension SPI.
- **Exit:** thread guards catch illegal calls; saturation/cancellation/shutdown/fault tests pass; exported diagnostics contain no test secrets.

### M06 — Compatibility and world-provider foundation

- **Requirements:** ARC-002/007, ARENA-005/006, INT-004/005, INT-010 architecture subset, COMPAT-002..009, CONTENT-009.
- **Entry:** M01 compatibility ADR and M05 scheduler ready.
- **Deliver:** compatibility capability API, primary 1.21.1 adapter, world provider contracts and at least native primary adapter; version-neutral material/effect/item representations.
- **Exit:** primary Paper world load/clone/reset/unload E2E passes; file work vs owner-thread calls proven; provider contract suite reusable for later adapters.

### M07 — Arena, map and setup lifecycle

- **Requirements:** ARENA-001..009; ZBW-ADDON-408..423.
- **Entry:** M03–M06 complete.
- **Deliver:** arena/map definitions, ID registry, CRUD/import/export/backup/duplicate, setup wizard/validator, health and admin surfaces.
- **Exit:** duplicate creates new mapped ID and independent state; rename preserves references; invalid arenas cannot enable; concurrent reset and recovery tests pass.

### M08 — Game engine, sessions, teams and lobby

- **Requirements:** GAME-001..003, GAME-006/008/010; ZBW-ADDON-001..009, 108..114, 124..130, 148..154, 334..340, 398..407, 424..437.
- **Entry:** M07 provides validated arena lease; M04 outbox and M05 scheduler stable.
- **Deliver:** deterministic match/session/team state machines, lobby/waiting, restore/recovery and completion transaction orchestration.
- **Exit:** transition/property tests and primary Paper E2E cover start through reset, reconnect/crash and exactly-once end; player state is always restored.

### M09 — Unified command and GUI frameworks

- **Requirements:** UX-001..003/006.
- **Entry:** M03 authorization/localization and M05 async loading are ready; M08 supplies first real use cases.
- **Deliver:** command tree/help/audit, page renderer/state/confirm tokens/editor accessibility patterns.
- **Exit:** command/permission inventories and GUI interaction tests pass; command and GUI call identical use cases; async data never blocks tick thread.

### M10 — Modes, matchmaking and selectors

- **Requirements:** GAME-004/005/007/009; CONTENT-003; ZBW-ADDON-092..101, 115..123, 131..140, 155..163, 236..244.
- **Entry:** M08/M09 complete.
- **Deliver:** standard/custom mode SPI, all named modes, selectors/queues/spectator/staff controls on shared server.
- **Exit:** gameplay matrix passes for every mode/team size and rejoin/disconnect; each mode has independent config/stats/shop/generator/upgrade/event/placeholder contracts.

### M11 — Shop, item, generator and upgrade platform

- **Requirements:** SHOP-001..007; CONTENT-002/003; ZBW-ADDON-010..025, 061..070, 141..147, 184..201, 300..322, 341..349, 363..368, 379..397, 438..452.
- **Entry:** M08 event engine, M09 UI and M10 mode SPI complete.
- **Deliver:** catalog/purchase/tender services, Quick Buy, generators, upgrades/traps and all original utility items/custom action SPI.
- **Exit:** item/purchase matrices, atomicity/exploit and high-GUI-load tests pass; scripts/actions meet sandbox policy; admin/API/config/docs surfaces complete.

### M12 — Progression transaction core

- **Requirements:** PROG-001..005, PROG-011; ZBW-ADDON-174..183, 210..216, 245..251, 266..282.
- **Entry:** M04 outbox, M08 events and M11 tender model stable.
- **Deliver:** event projection, XP/level/prestige/currency and unified transactional reward engine with offline/cross-server-ready delivery contracts.
- **Exit:** duplicate/retry/crash tests cannot double award; formula/migration/admin/audit tests pass; reward summary has all presentation adapters.

### M13 — Objectives, quests, achievements and battle pass

- **Requirements:** PROG-009/010/012/013; CONTENT-004..006; ZBW-ADDON-081..091.
- **Entry:** M12 reward/progression core and M09 UI ready.
- **Deliver:** reusable objective engine, every listed objective/scope, quest/achievement/challenge/pass definitions and editors.
- **Exit:** objective/reward catalog tests, season rollover, claims/rerolls/chains and migration/security E2E pass; no duplicated objective logic.

### M14 — Cosmetics, profiles and calendar rewards

- **Requirements:** PROG-006..008/014; CONTENT-007/009/011; ZBW-ADDON-026..040, 369..378.
- **Entry:** M12/M13 complete; original content/license ADR accepted.
- **Deliver:** 300 original definitions, rarity/ownership/equipment/effects, profile/privacy/settings and holiday/calendar rewards.
- **Exit:** catalog count/license scan, ownership/expiry/preset/migration and rate-limit load tests pass; low-performance/emergency controls meet budgets.

### M15 — Statistics, ratios, streaks and leaderboards

- **Requirements:** STATS-001..008; ZBW-ADDON-217..225, 260..265, 350..356.
- **Entry:** M08 event schema and M12 projection/idempotency patterns stable.
- **Deliver:** authoritative projections, all dimensions/ratios/streaks, administration and cached ranking engine.
- **Exit:** duplicate/private/test separation, ratio/tie/reset/repair/migration and large-data ranking tests pass; no full-table query per request.

### M16 — PlaceholderAPI and external statistics surfaces

- **Requirements:** PAPI-001..006, OPS-007, DISCORD-001..008; ZBW-ADDON-071..080, 202..209, 357..362, 453..463.
- **Entry:** M12–M15 visible data contracts stable; M05 metrics ready.
- **Deliver:** native expansion, dynamic contexts/families/formatters/admin tools/docs generator and secured external/Discord provider APIs.
- **Exit:** placeholder inventory/context/offline/fallback/cache/parser tests pass; zero sync I/O and p95 budget verified; privacy/scope/rate tests pass.

### M17 — Replay recording, storage and viewer

- **Requirements:** REPLAY-001..010.
- **Entry:** M08 canonical events, M05 queues, M04 metadata storage, replay ADR accepted.
- **Deliver:** capture/codec/manifest/store/retention/recovery/privacy, playback scene/rendering, timeline/telemetry/staff evidence and all surfaces.
- **Exit:** golden timing/order, interruption/corruption/repair/hold/privacy/provider failure and performance tests pass; evidence degradation policy demonstrated.

### M18 — Atlas case and review platform

- **Requirements:** ATLAS-001..013; ZBW-ADDON-323..333 controlled staff tooling.
- **Entry:** M12 rewards, M14 profile, M15 stats and M17 replay complete; moderation/privacy ADRs accepted.
- **Deliver:** cases/anonymization/reservation/verdict/reputation/accuracy/anti-abuse/rewards/staff policy and all Atlas surfaces.
- **Exit:** eligibility/bypass/conflict/anonymization/reservation/abuse/reward/override tests pass; no default community permanent punishment; gameplay budgets hold.

### M19 — Redis and distributed consistency

- **Requirements:** DEPLOY-006/008/009.
- **Entry:** M04 outbox, M12/15/17/18 durable semantics complete; Redis ADR accepted.
- **Deliver:** versioned keys/messages, invalidation/streams/locks/fencing/leader election, health/circuit/degradation.
- **Exit:** partition/reconnect/duplicate/order/rolling-schema/lock-expiry load tests pass; no split-brain finalization and no unbounded polling.

### M20 — Proxy networking and scalable deployment

- **Requirements:** DEPLOY-002..004, distributed portions of GAME/PROG/STATS/REPLAY/ATLAS/INT-009; CONTENT-008; ZBW-ADDON-041..060, 102..107, 164..173, 252..259, 291..299, 464..473.
- **Entry:** M19 complete and M10 routing contracts stable.
- **Deliver:** Velocity and Bungee adapters, backend registry/reservations/transfers/failover/drain and cross-server user flows.
- **Exit:** provider-equivalence and signed-message security tests pass; crash/retry/fallback/duplicate player and cross-server queue/party/rejoin/play-again E2E pass.

### M21 — CloudNet, parties and remaining providers

- **Requirements:** DEPLOY-005, INT-001..003/006..009; ZBW-ADDON-226..235.
- **Entry:** M20 scalable routing and relevant feature APIs stable.
- **Deliver:** CloudNet scaling; native/external parties; Placeholder/Vault/LuckPerms/NPC/hologram/Grim/Vulcan adapters and dashboards.
- **Exit:** every pinned supported provider passes shared contract/failure/version tests; both anticheats can run together without duplicate cases; scale/drain/crash replacement works.

### M22 — Full 1.8–1.21.x and Bedrock compatibility matrix

- **Requirements:** INT-010, COMPAT-001..009, remaining ARENA/SHOP/REPLAY/UX compatibility acceptance.
- **Entry:** M06 adapter contract proven and all feature semantics stable.
- **Deliver:** separate version-family bootstraps/adapters/toolchains, mappings, Via integrations and Geyser/Floodgate alternatives.
- **Exit:** every claimed matrix row passes build/startup/gameplay/GUI/item/packet/replay tests on its required JDK; unsupported limitations are explicit and owner-approved alternatives exist where necessary.

### M23 — Migration, ecosystem and operational completion

- **Requirements:** ECO-001..005, OPS-006/009; ZBW-ADDON-283..290.
- **Entry:** Feature schemas/APIs stable; source formats legally documented.
- **Deliver:** migration assistants, SDK/example, marketplace metadata, extensible doctor, complete operator/developer guides.
- **Exit:** dry-run/backup/map/duplicate/rollback reports pass on fixtures; sample extension uses public API only; all required docs/reference inventories are complete.

### M24 — Security, performance and release qualification

- **Requirements:** QA-001..007, GOV-007/011, OPS-008, CONTENT-001/011, LICENSE-001..007, READY-001..020 and final acceptance of all 672 semantic IDs and atomic children.
- **Entry:** M01–M23 complete with no mandatory open implementation work.
- **Deliver:** threat/privacy/license review, compatibility report, full benchmark/security report, recovery exercise, reproducible artifacts, final compliance report and release notes.
- **Exit:** clean release CI; all budgets/matrices pass; vulnerability exceptions are approved; every one of 199 Part I IDs, 473 addon IDs and every `MP-L####` child has an allowed final status and evidence; dependency/asset SBOM, notices and fallback matrices match artifacts; atomic functional coverage remains 100%; no unresolved mandatory requirement.

## Native addon allocation summary

The authoritative row-level allocation is the `Milestone` column of `docs/ADDON_FEATURE_CATALOG.md`. The grouped ranges below are a planning index and do not merge their child requirements.

| Milestone | Addon references and stable requirement ranges |
|---|---|
| M07 | ArenaSetup (`ZBW-ADDON-408..423`) |
| M08 | HotbarManager (`001..009`); Deposit (`108..114`); Arena Start Message (`124..130`); AntiDrop (`148..154`); LeaveDelay (`334..340`); TabSorter (`398..407`); BossBar (`424..431`); AdventureMode (`432..437`) |
| M10 | Spectator Options (`092..101`); Spectator/Play-Again Menu (`115..123`); Compass (`131..140`); Team Selector (`155..163`); Swappage (`236..244`) |
| M11 | Armed (`010..025`); LuckyBlock (`061..070`); Sponge (`141..147`); Pop-up Towers (`184..193`); Generator Split (`194..201`); Ultimate (`300..314`); Voidless (`315..322`); Rush (`341..349`); PerArenaGen (`363..368`); Item Rotation (`379..388`); Color Changer (`389..397`); BedSteal (`438..452`) |
| M12 | Reward Commands (`174..183`); Golden GG (`210..216`); XP Bar (`245..251`); RewardSummary (`266..273`); HolidayReward (`274..282`) |
| M13 | Quests (`081..091`) |
| M14 | Premium Cosmetics (`026..040`); network-grade original Cosmetics (`369..378`) |
| M15 | Winstreak (`217..225`); KDR/FKDR/WLR (`260..265`); GroupStats (`350..356`) |
| M16 | DiscordStats (`071..080`); Discord/Corebot adapter (`202..209`); Per Group Stats (`357..362`); DiscordUtils (`453..463`) |
| M18 | AdminAddon controlled tools (`323..333`) |
| M20 | Private Games (`041..060`, `464..473`); Spectate (`102..107`); BedWarsProxy (`164..173`); MapSelector (`252..259`); Play Again (`291..299`) |
| M21 | CloudNet Support (`226..235`) |
| M23 | lawful layout migrator (`283..290`) |
| M24 | final verification of `ZBW-ADDON-001..473` |

## Resolved decision requirement allocation

| Decision | Stable IDs | Foundation milestone | Feature milestone | Final evidence |
|---|---|---|---|---|
| RC-072 RESOURCE SCARCITY | `ZBW-ADDON-464..473`, CONTENT-008 | M02/M03 generator/modifier/config contracts | M20 Private Games | Host GUI/permission/API/custom-generator E2E |
| RC-073 original content/provenance | CONTENT-001..011 | M01 provenance gate; M02/M03 content registry | M10–M14 and M20 by catalogue family | M24 asset/content/SBOM scan |
| RC-074 Discord providers | DISCORD-001..008 | M02 API/SPI, M03 secrets/default, M05 failure substrate | M16 | No-provider/outage/security/protocol tests |
| RC-075 1.8 fallbacks | COMPAT-001..009 | M01 toolchain matrix, M06 adapter/fallback foundation | M22 full matrix | Every fallback row/fixture passes |
| RC-076 dependency licensing | LICENSE-001..007 | M01 exact artifact approvals before Java | Continuous dependency changes | M24 reproducible notices/SBOM/artifact audit |

## Consolidated readiness decision allocation

Canonical requirements allocated below: `ZBW-READY-001`, `ZBW-READY-002`, `ZBW-READY-003`, `ZBW-READY-004`, `ZBW-READY-005`, `ZBW-READY-006`, `ZBW-READY-007`, `ZBW-READY-008`, `ZBW-READY-009`, `ZBW-READY-010`, `ZBW-READY-011`, `ZBW-READY-012`, `ZBW-READY-013`, `ZBW-READY-014`, `ZBW-READY-015`, `ZBW-READY-016`, `ZBW-READY-017`, `ZBW-READY-018`, `ZBW-READY-019`, `ZBW-READY-020`.

| Decisions | Stable IDs | Foundation milestone | Dependent feature milestones | Final evidence |
|---|---|---|---|---|
| RC-003/004/022 runtime matrix | READY-001/002/006 | M01 artifacts/toolchains/fixtures | M06 compatibility; M22 all runtime/client rows | Per-row boot/game/fallback evidence |
| RC-021/024/027 dependencies/providers | READY-005/007/008 | M01 immutable lock and module stack | M02 provider SPIs; M21 integrations | Reproducible SBOM/licence/provider CT |
| RC-018 scripting | READY-004 | M02 capability API; M03 schema/auth; M05 bounded workers | M11 items/shop custom logic; M23 SDK | Escape/fuzz/budget/no-main-thread ST |
| RC-029/062 performance/quality | READY-009/017 | M01 harness/gates | Every milestone supplies evidence | M24 three-profile benchmark and all quality gates |
| RC-040/041/065 privacy | READY-010/011/018 | M03 auth/privacy schema; M04 retention storage | M15 stats, M17 replay, M18 Atlas | No-chat/export/delete/hold/visibility ST/MT |
| RC-043/071 clean-room scope | READY-012/020 | M01 provenance/legal scans | Every addon/content/migration milestone | M24 49/473 + artifact/content release gate |
| RC-046/050 network authority | READY-013/014 | M03 secrets/envelopes; M04 outbox/inbox | M19 Redis, M20 proxy, M21 CloudNet | Forge/replay/duplicate/partition/rolling CT/ST |
| RC-059 balance | READY-015 | M03 content schema/validation | M10 modes; M11 shop/generator; M13 rewards | Golden deterministic mode/economy simulations |
| RC-061 operations | READY-016 | M04 backup/recovery; M05 failure substrate | M17 evidence, M19/M20 distributed | RPO/RTO/restore/chaos drill evidence |
| RC-017 cosmetics | READY-003 | M02 content API; M03 schema; M06 renderers | M14 five 60-item batches | M24 300 count/provenance/visual/PT gate |
| RC-066 licensing | READY-019 | M01 build/release classifier | M23 SDK/marketplace | M24 executed legal terms and artifact audit |

## Critical dependency chain

`M00 → M01 → M02 → (M03, M05) → M04/M06 → M07 → M08 → M09/M10 → M11/M12 → M13–M16 → M17 → M18 → M19 → M20 → M21/M22 → M23 → M24`.

Parallel work is allowed only where this graph and module boundaries show no unmet data/API dependency.
