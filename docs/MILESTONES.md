# ZartraBedWars Dependency-Aware Milestones

## Rules

Each milestone is a small reviewable merge unit. Tests and documentation are produced with the feature, not postponed to the final testing/documentation milestones. A milestone exits only with a clean build, all applicable tests, traceability updates, no production TODO/stub and evidence for its PRD IDs. Later milestones may not bypass an unmet dependency.

The master prompt's sequential “documentation/testing near the end” list is treated as final hardening order; continuous testing and documentation requirements have higher safety value and apply to every milestone.

## Plan

### M00 — Requirements and architecture baseline

- **Requirements:** ZBW-GOV-001..011, ZBW-QA-007, architecture/plan portions of all IDs.
- **Entry:** `MASTER_PROMPT.md` available and read completely.
- **Deliver:** PRD, architecture, milestones, requirement-level traceability, a deterministic verbatim atomic coverage report, risks/conflicts, ADR templates and repository instructions.
- **Exit:** 144 unique requirement IDs; Part I has one row per ID; Part II has one row per non-empty source assertion; the recorded source hash and line count validate; every requested audit category is declared; all atomic rows are `COVERED` and overall functional coverage is 100%; unresolved decisions are explicit; no Java source/build scaffold created.

### M01 — Resolve blocking ADRs and establish build governance

- **Requirements:** GOV-003..011, ARC-001/002/008, OPS-008, QA-001/005/007.
- **Entry:** M00 approved; owners available for blocking decisions.
- **Deliver:** accepted ADRs for runtime/toolchains, module graph, dependency versions/licenses, benchmark baseline, namespace and privacy; Maven parent/BOM/toolchains; pinned Python 3.11+ documentation-verifier runtime; CI skeleton and static architecture rules.
- **Exit:** clean empty multi-module build on primary JDK; CI quality gates, including deterministic atomic coverage drift detection, pass; supported-version/provider matrix is pinned; no functional production path claimed.

### M02 — Public API, domain primitives and extension metadata

- **Requirements:** ARC-003/004/009/010, ECO-002/003.
- **Entry:** M01 module/API ADR accepted.
- **Deliver:** typed IDs, clocks/results/events, public API versioning, provider/extension contracts and metadata validator.
- **Exit:** binary/API contract tests and example metadata pass; no Bukkit/store dependency in domain/API; JavaDoc documents thread/error/version rules.

### M03 — Configuration, localization, permissions and validation foundations

- **Requirements:** OPS-001..004, UX-004/005, OPS-002/003.
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

- **Requirements:** ARC-002/007, ARENA-005/006, INT-004/005, INT-010 architecture subset.
- **Entry:** M01 compatibility ADR and M05 scheduler ready.
- **Deliver:** compatibility capability API, primary 1.21.1 adapter, world provider contracts and at least native primary adapter; version-neutral material/effect/item representations.
- **Exit:** primary Paper world load/clone/reset/unload E2E passes; file work vs owner-thread calls proven; provider contract suite reusable for later adapters.

### M07 — Arena, map and setup lifecycle

- **Requirements:** ARENA-001..009.
- **Entry:** M03–M06 complete.
- **Deliver:** arena/map definitions, ID registry, CRUD/import/export/backup/duplicate, setup wizard/validator, health and admin surfaces.
- **Exit:** duplicate creates new mapped ID and independent state; rename preserves references; invalid arenas cannot enable; concurrent reset and recovery tests pass.

### M08 — Game engine, sessions, teams and lobby

- **Requirements:** GAME-001..003, GAME-006/008/010.
- **Entry:** M07 provides validated arena lease; M04 outbox and M05 scheduler stable.
- **Deliver:** deterministic match/session/team state machines, lobby/waiting, restore/recovery and completion transaction orchestration.
- **Exit:** transition/property tests and primary Paper E2E cover start through reset, reconnect/crash and exactly-once end; player state is always restored.

### M09 — Unified command and GUI frameworks

- **Requirements:** UX-001..003/006.
- **Entry:** M03 authorization/localization and M05 async loading are ready; M08 supplies first real use cases.
- **Deliver:** command tree/help/audit, page renderer/state/confirm tokens/editor accessibility patterns.
- **Exit:** command/permission inventories and GUI interaction tests pass; command and GUI call identical use cases; async data never blocks tick thread.

### M10 — Modes, matchmaking and selectors

- **Requirements:** GAME-004/005/007/009.
- **Entry:** M08/M09 complete.
- **Deliver:** standard/custom mode SPI, all named modes, selectors/queues/spectator/staff controls on shared server.
- **Exit:** gameplay matrix passes for every mode/team size and rejoin/disconnect; each mode has independent config/stats/shop/generator/upgrade/event/placeholder contracts.

### M11 — Shop, item, generator and upgrade platform

- **Requirements:** SHOP-001..007.
- **Entry:** M08 event engine, M09 UI and M10 mode SPI complete.
- **Deliver:** catalog/purchase/tender services, Quick Buy, generators, upgrades/traps and all original utility items/custom action SPI.
- **Exit:** item/purchase matrices, atomicity/exploit and high-GUI-load tests pass; scripts/actions meet sandbox policy; admin/API/config/docs surfaces complete.

### M12 — Progression transaction core

- **Requirements:** PROG-001..005, PROG-011.
- **Entry:** M04 outbox, M08 events and M11 tender model stable.
- **Deliver:** event projection, XP/level/prestige/currency and unified transactional reward engine with offline/cross-server-ready delivery contracts.
- **Exit:** duplicate/retry/crash tests cannot double award; formula/migration/admin/audit tests pass; reward summary has all presentation adapters.

### M13 — Objectives, quests, achievements and battle pass

- **Requirements:** PROG-009/010/012/013.
- **Entry:** M12 reward/progression core and M09 UI ready.
- **Deliver:** reusable objective engine, every listed objective/scope, quest/achievement/challenge/pass definitions and editors.
- **Exit:** objective/reward catalog tests, season rollover, claims/rerolls/chains and migration/security E2E pass; no duplicated objective logic.

### M14 — Cosmetics, profiles and calendar rewards

- **Requirements:** PROG-006..008/014.
- **Entry:** M12/M13 complete; original content/license ADR accepted.
- **Deliver:** 300 original definitions, rarity/ownership/equipment/effects, profile/privacy/settings and holiday/calendar rewards.
- **Exit:** catalog count/license scan, ownership/expiry/preset/migration and rate-limit load tests pass; low-performance/emergency controls meet budgets.

### M15 — Statistics, ratios, streaks and leaderboards

- **Requirements:** STATS-001..008.
- **Entry:** M08 event schema and M12 projection/idempotency patterns stable.
- **Deliver:** authoritative projections, all dimensions/ratios/streaks, administration and cached ranking engine.
- **Exit:** duplicate/private/test separation, ratio/tie/reset/repair/migration and large-data ranking tests pass; no full-table query per request.

### M16 — PlaceholderAPI and external statistics surfaces

- **Requirements:** PAPI-001..006, OPS-007.
- **Entry:** M12–M15 visible data contracts stable; M05 metrics ready.
- **Deliver:** native expansion, dynamic contexts/families/formatters/admin tools/docs generator and secured external/Discord provider APIs.
- **Exit:** placeholder inventory/context/offline/fallback/cache/parser tests pass; zero sync I/O and p95 budget verified; privacy/scope/rate tests pass.

### M17 — Replay recording, storage and viewer

- **Requirements:** REPLAY-001..010.
- **Entry:** M08 canonical events, M05 queues, M04 metadata storage, replay ADR accepted.
- **Deliver:** capture/codec/manifest/store/retention/recovery/privacy, playback scene/rendering, timeline/telemetry/staff evidence and all surfaces.
- **Exit:** golden timing/order, interruption/corruption/repair/hold/privacy/provider failure and performance tests pass; evidence degradation policy demonstrated.

### M18 — Atlas case and review platform

- **Requirements:** ATLAS-001..013.
- **Entry:** M12 rewards, M14 profile, M15 stats and M17 replay complete; moderation/privacy ADRs accepted.
- **Deliver:** cases/anonymization/reservation/verdict/reputation/accuracy/anti-abuse/rewards/staff policy and all Atlas surfaces.
- **Exit:** eligibility/bypass/conflict/anonymization/reservation/abuse/reward/override tests pass; no default community permanent punishment; gameplay budgets hold.

### M19 — Redis and distributed consistency

- **Requirements:** DEPLOY-006/008/009.
- **Entry:** M04 outbox, M12/15/17/18 durable semantics complete; Redis ADR accepted.
- **Deliver:** versioned keys/messages, invalidation/streams/locks/fencing/leader election, health/circuit/degradation.
- **Exit:** partition/reconnect/duplicate/order/rolling-schema/lock-expiry load tests pass; no split-brain finalization and no unbounded polling.

### M20 — Proxy networking and scalable deployment

- **Requirements:** DEPLOY-002..004, distributed portions of GAME/PROG/STATS/REPLAY/ATLAS/INT-009.
- **Entry:** M19 complete and M10 routing contracts stable.
- **Deliver:** Velocity and Bungee adapters, backend registry/reservations/transfers/failover/drain and cross-server user flows.
- **Exit:** provider-equivalence and signed-message security tests pass; crash/retry/fallback/duplicate player and cross-server queue/party/rejoin/play-again E2E pass.

### M21 — CloudNet, parties and remaining providers

- **Requirements:** DEPLOY-005, INT-001..003/006..009.
- **Entry:** M20 scalable routing and relevant feature APIs stable.
- **Deliver:** CloudNet scaling; native/external parties; Placeholder/Vault/LuckPerms/NPC/hologram/Grim/Vulcan adapters and dashboards.
- **Exit:** every pinned supported provider passes shared contract/failure/version tests; both anticheats can run together without duplicate cases; scale/drain/crash replacement works.

### M22 — Full 1.8–1.21.x and Bedrock compatibility matrix

- **Requirements:** INT-010, remaining ARENA/SHOP/REPLAY/UX compatibility acceptance.
- **Entry:** M06 adapter contract proven and all feature semantics stable.
- **Deliver:** separate version-family bootstraps/adapters/toolchains, mappings, Via integrations and Geyser/Floodgate alternatives.
- **Exit:** every claimed matrix row passes build/startup/gameplay/GUI/item/packet/replay tests on its required JDK; unsupported limitations are explicit and owner-approved alternatives exist where necessary.

### M23 — Migration, ecosystem and operational completion

- **Requirements:** ECO-001..005, OPS-006/009.
- **Entry:** Feature schemas/APIs stable; source formats legally documented.
- **Deliver:** migration assistants, SDK/example, marketplace metadata, extensible doctor, complete operator/developer guides.
- **Exit:** dry-run/backup/map/duplicate/rollback reports pass on fixtures; sample extension uses public API only; all required docs/reference inventories are complete.

### M24 — Security, performance and release qualification

- **Requirements:** QA-001..007, GOV-007/011, OPS-008 and final acceptance of all IDs and atomic children.
- **Entry:** M01–M23 complete with no mandatory open implementation work.
- **Deliver:** threat/privacy/license review, compatibility report, full benchmark/security report, recovery exercise, reproducible artifacts, final compliance report and release notes.
- **Exit:** clean release CI; all budgets/matrices pass; vulnerability exceptions are approved; every one of 144 IDs and every `MP-L####` child has an allowed final status and evidence; atomic functional coverage remains 100%; no unresolved mandatory requirement.

## Critical dependency chain

`M00 → M01 → M02 → (M03, M05) → M04/M06 → M07 → M08 → M09/M10 → M11/M12 → M13–M16 → M17 → M18 → M19 → M20 → M21/M22 → M23 → M24`.

Parallel work is allowed only where this graph and module boundaries show no unmet data/API dependency.
