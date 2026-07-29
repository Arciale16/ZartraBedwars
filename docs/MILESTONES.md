# ZartraBedWars Dependency-Aware Milestones

## Rules

Each milestone is a small reviewable merge unit. Tests and documentation are produced with the feature, not postponed to the final testing/documentation milestones. A milestone exits only with a clean build, all applicable tests, traceability updates, no production TODO/stub and evidence for its core and `ZBW-ADDON-*` IDs. Later milestones may not bypass an unmet dependency.

The master prompt's sequential â€œdocumentation/testing near the endâ€ list is treated as final hardening order; continuous testing and documentation requirements have higher safety value and apply to every milestone.

## Plan

### M00 â€” Requirements, architecture and pre-code decision baseline

- **Requirements:** ZBW-GOV-001..011, ZBW-QA-007, ZBW-READY-001..020, architecture/plan portions of all 672 semantic IDs.
- **Entry:** `MASTER_PROMPT.md` available and read completely.
- **Deliver:** PRD, architecture, milestones, requirement-level traceability, deterministic Master/addon/decision coverage reports; all 25 pre-code decision outcomes, sixteen ADRs and normative runtime/performance/privacy/network/scripting/licensing/content/operations/quality specifications; risks/conflicts and repository instructions.
- **Exit:** 672 unique semantic IDs (199 Part I plus 473 addon); Parts Iâ€“III and all decision-document inventories validate; source hash/line count, 49-addon inventory, 8/41 split, sixteen accepted ADRs and required schemas match; all atomic rows are `COVERED`, combined functional coverage is 100%, the pre-code readiness report has no unresolved pre-code decision and no Java source/build scaffold exists.

### M01 â€” Materialize the accepted build governance baseline

- **Implementation status:** VERIFIED on 2026-07-14; evidence in `docs/IMPLEMENTATION_M01.md`. This status covers only the M01 allocation of requirements that also continue into later milestones.
- **Requirements:** GOV-003..011, ARC-001/002/008, OPS-008, QA-001/005/007, CONTENT-001/011, COMPAT-001/009, LICENSE-001..007, READY-001/002/005/006/008/009/012/017/019/020.
- **Entry:** M00 PRE-CODE READY baseline approved; exact selections and architecture are not reopened without an ADR.
- **Deliver:** Maven parent/BOM/wrapper/toolchains; pre-resolution checksum/licence lock generator; SBOM/notices/provenance gates; empty module graph; pinned validator runtime; CI, static architecture/bytecode/quality rules and private runtime-fixture manifest.
- **Exit:** clean empty multi-module build on all compile JDKs; all documentation/licence/provenance/dependency-lock gates pass; every resolved build artifact has exact checksum/licence evidence; runtime/provider matrix is immutable; no functional production path is claimed.
- **Exit evidence:** five local checksum-locked Temurin builds (8/11/16/17/21) pass the three-POM offline reactor; 10 governance tests and all static, dependency/licence/SBOM, asset, fixture and documentation gates pass; 14 build/CI artifacts are locked; zero `.java`, feature module, product binary or certified Minecraft runtime exists.

### M02 â€” Public API, domain primitives and extension metadata

- **Implementation status:** VERIFIED on 2026-07-14; evidence in `docs/IMPLEMENTATION_M02.md`. This status covers only M02 allocations of requirements that continue into later milestones.
- **Requirements:** ARC-003/004/009/010, ECO-002/003, CONTENT-010, DISCORD-001/002/005.
- **Entry:** M01 module/API ADR accepted.
- **Deliver:** typed IDs, clocks/results/events, public API versioning, provider/extension contracts and metadata validator.
- **Exit:** binary/API contract tests and example metadata pass; no Bukkit/store dependency in domain/API; JavaDoc documents thread/error/version rules.
- **Exit evidence:** five Java-8-bytecode modules; 24 unit/contract tests; six metadata fixtures; 107-public-class binary baseline; exact Java 8/11/16/17/21 compile matrix; strict JavaDoc; architecture, Checkstyle, SpotBugs, dependency/licence/SBOM and deterministic documentation gates pass. No runtime/gameplay behavior or M03 module exists.

### M03 â€” Configuration, localization, permissions and validation foundations

- **Implementation status:** VERIFIED on 2026-07-15; evidence in `docs/IMPLEMENTATION_M03.md`. This status covers only the M03 allocations of requirements that continue into later milestones.
- **Requirements:** OPS-001..004, UX-004/005, DISCORD-006/008; M03 schema/configuration portions of ZBW-ADDON-464..473, CONTENT-001..011, COMPAT-001..009 and READY-003/004/010/011/013/014/015/018.
- **Entry:** M02 types/events stable enough for schemas.
- **Deliver:** typed versioned configuration, comments/docs generator, transactional targeted reload, localization and authorization ports, startup/manual validator.
- **Exit:** malformed/unknown/dependency/secret cases tested; every initial option has metadata; reload never partially applies; permission checks are centralized.
- **Exit evidence:** nine-project reactor and 54 tests pass on checksum-locked Temurin 8/11/16/17/21 while producing Java 8 bytecode; exact 36-file schemas, five configuration fixtures, 33 centralized actions and 183-public-class append-only API baseline validate. Checkstyle and SpotBugs report zero findings; `zbw-config` JaCoCo reaches 95.87% line and 84.43% branch coverage. Dependency/licence/SBOM, JavaDoc, architecture, governance and all three 100%-coverage documentation gates pass. No M04 module or behavior exists.

### M04 â€” Storage, migrations, outbox and cache foundations

- **Implementation status:** VERIFIED on 2026-07-15. Local and certified external evidence is in `docs/IMPLEMENTATION_M04.md`; RC-077 is resolved by PR #5 workflow run `29406777872`. This status covers only M04 allocations and does not start M05.
- **Requirements:** DEPLOY-007/008, ARC-008, data portions of PROG/STATS/REPLAY/ATLAS.
- **Entry:** M01 DB ADR and M02 IDs/events accepted; M03 secret/config services usable.
- **Deliver:** repository/unit-of-work APIs; SQLite/MySQL/MariaDB adapters; Hikari; migrations; backup/restore; inbox/outbox; bounded local cache.
- **Exit:** container/SQLite contract suites, crash/retry/idempotency/concurrency/migration tests pass; query plans and pool metrics documented; zero SQL in feature/platform classes.
- **Exit evidence:** eleven-project reactor produces Java 8 bytecode and passes on checksum-locked Temurin 8/11/16/17/21. The local suite passes 79/79 with zero skips. Independent digest-pinned MySQL 8.4.0 and MariaDB 11.4.2 jobs each pass 91/91 reactor tests, including 12/12 mandatory external contracts with zero failures/errors/skips, seven certified JSON query plans and sanitized Hikari evidence. SQLite, Flyway, crash/retry/idempotency/threading/migration/cache/privacy/backup suites pass. `zbw-storage-api` reaches 90.23% line/87.50% branch and `zbw-storage-sql` 91.34% line/72.19% branch; Checkstyle and SpotBugs report zero findings. The exact lock contains 192 non-bundled Maven components/604 files and 15 build/CI artifacts. Dependency/licence/provenance, binary/API, JavaDoc, governance and 100%-coverage documentation gates pass.

### M05 â€” Scheduler, lifecycle, observability and failure substrate

- **Implementation status:** VERIFIED and merged on 2026-07-15. Evidence is in `docs/IMPLEMENTATION_M05.md`. This status covers only M05 and does not start M06.
- **Requirements:** ARC-005/006, OPS-005/006, GAME-010.
- **Entry:** M02 application ports and M03 config exist.
- **Deliver:** owner-thread dispatch, bounded executors/queues, lifecycle drain, health/metrics, sanitized diagnostics, Plugin Doctor extension SPI.
- **Exit:** thread guards catch illegal calls; saturation/cancellation/shutdown/fault tests pass; exported diagnostics contain no test secrets.
- **Exit evidence:** twelve-project Java-8-bytecode reactor; 101/101 deterministic tests with zero skips; application coverage 96.69% line/90.40% branch; observability coverage 98.31% line/85.58% branch; Checkstyle and SpotBugs zero findings; additive 270-class M05 binary baseline; strict JavaDoc; dependency/licence/SBOM/notices and deterministic M00-M05 governance pass. The exact Temurin 8/11/16/17/21 build matrix is enforced by `.github/workflows/m05-toolchain-matrix.yml`.

### M06 â€” Compatibility and world-provider foundation

- **Implementation status:** VERIFIED on 2026-07-15. Evidence is in `docs/IMPLEMENTATION_M06.md` and `build/evidence/m06-paper-primary.json`. This status covers only the Paper 1.21.1 build 133 M06 foundation and does not start M07 or complete M22 compatibility.
- **Requirements:** ARC-002/007; M06 foundations of ARENA-005/006, INT-004/005, INT-010, COMPAT-001..009, CONTENT-009 and READY-001/002/003/006. M06 does not complete the full acceptance of INT-010, COMPAT-001..009 or READY-001/002/006.
- **Entry:** M01 runtime/toolchain ADRs and immutable dependency policy remain accepted; M02 API/provider contracts, M03 compatibility configuration/validation and M05 scheduler/thread/lifecycle/failure substrate are merged and stable; the exact Paper 1.21.1 build 133 fixture remains checksum/licence approved; the module graph contains no dependency on a later-milestone module; deterministic governance passes. M04 storage is not an M06 runtime dependency.
- **Deliver:** materialize `zbw-compat-api` and `zbw-world` as Java-8 neutral contracts/orchestration, plus `zbw-compat-v1_20-v1_21` and `zbw-paper-modern` as Java-21 primary-platform artifacts; implement semantic material/item/metadata/sound/particle/text/entity/packet/UI/scheduler capabilities, typed supported/unsupported/fallback/degraded results, primary 1.21.1 mappings, Paper owner-thread dispatch/bootstrap and the native primary world provider. The M06 certification target is only Paper 1.21.1 build 133 and only for the M06 foundation scope.
- **Exit:** the two neutral modules compile to Java 8 and contain no Bukkit/Paper/NMS/storage/Redis/proxy imports; the two modern modules compile to Java 21; graph/order/cycle checks pass; Paper 1.21.1 bootstrap start/stop and native world load/clone/reset/unload E2E pass; filesystem work is proven off the owner thread and world/entity mutation on it; bounded cancellation/drain uses M05; reusable compatibility/world-provider suites pass for primary, unsupported and fallback paths; binary/API, static, coverage, dependency/licence, documentation and governance gates pass. No 1.8 adapter, translated-client/Bedrock provider, complete 1.20â€“1.21 family claim, arena CRUD/gameplay or M07+ behavior exists.
- **Exit evidence:** sixteen-module JDK-21 reactor passes 138 tests with zero skips; Java 8/11/16/17 neutral builds pass 121 tests each and Java 21 builds the neutral plus modern artifacts. Exact class-major 52/65 and append-only M02-M05 API checks pass. Contract suites cover typed capability outcomes/LKG mappings and bounded world load/clone/reset/unload, cancellation, timeout, drain, rollback and leak accounting. The isolated Paper 1.21.1 build 133 run passes all five certification operations, owner/worker affinity, leak-free unload and non-blocking worker shutdown. Quality, dependency/licence/SBOM/notices, JavaDoc, governance and all three 100%-coverage documentation validators pass.
- **Deferred to M22:** `zbw-compat-v1_8` and every legacy/intermediate adapter and bootstrap, legacy material/NBT/particle/sound/text/entity/packet/GUI/input/scheduler implementations, Via/Geyser/Floodgate paths and full feature certification of every runtime row. M06 primary certification never closes the M22 release gate.

### M07 â€” Arena, map and setup application lifecycle

- **Implementation status:** VERIFIED on 2026-07-15; evidence in `docs/IMPLEMENTATION_M07.md` and `build/evidence/m07-paper-primary.json`. This closes only M07 core/application allocations; M09 presentation, M16 placeholders, M21 optional providers and M22 compatibility completion remain open.
- **Requirements:** core/application portions of ARENA-001..009 and ZBW-ADDON-408..423. Their final command and GUI presentation portions remain M09; PlaceholderAPI remains M16, optional world providers remain M21 and full legacy/runtime compatibility remains M22.
- **Entry:** M03â€“M06 complete; the M07/M09 allocation validator passes; `zbw-arena` has no dependency on an M09 module; M03 authorization/configuration, M04 storage, M05 scheduler/failure and M06 world/primary Paper foundations remain stable.
- **Deliver:** presentation-neutral arena/map definitions, ID registry, repositories and use cases for CRUD/import/export/backup/restore/duplicate, setup sessions and every setup step, validation, two-phase preview/apply, undo/redo, atomic save/rollback, enable gating, lifecycle health/diagnostic views, authorization/audit intents and typed test-harness entry points. No production command or GUI is delivered.
- **Exit:** duplicate creates a new mapped ID and independent state; rename preserves references; invalid arenas cannot enable; setup and administration use cases are completely exercisable through typed APIs and deterministic harnesses; concurrent reset/recovery, stale-revision, authorization, rollback and primary Paper 1.21.1 lifecycle tests pass; no M08 gameplay or M09 command/UI module is materialized. M07 verification covers only these core/application allocations and does not claim final presentation acceptance.
- **Exit evidence:** the 15-module neutral reactor passes 158 tests on each of JDK 8/11/16/17; the 17-module JDK-21 reactor passes 175 tests. M07 contributes 37 tests with zero skips, including three real SQLite transactional contracts. Exact Paper 1.21.1 build 133 passes validation, setup undo/redo, archive round trip, all five world operations and leak-free unload. M07 coverage is 95.78% line and 85.09% branch; Checkstyle, SpotBugs, binary/API, dependency/licence/provenance, JavaDoc and deterministic governance gates pass. No M08 or M09 module exists.

### M08 â€” Game engine, sessions, teams, lobby and primary Paper projections

- **Implementation status:** VERIFIED on 2026-07-15; evidence in `docs/IMPLEMENTATION_M08.md` and `build/evidence/m08-paper-primary.json`. This closes only M08 core/application, closed primary Paper and shared-server-foundation allocations; M09/M10/M16/M20/M21/M22 remain open.
- **Requirements:** core/application and closed primary Paper 1.21.1 portions of GAME-001..003, GAME-006/008/010; ZBW-ADDON-001..009, 108..114, 124..130, 148..154, 334..340, 398..407 and 424..437; shared-server runtime foundation of DEPLOY-001. Final commands, GUIs, editors and confirmation flows remain M09; selectors/modes remain M10, PlaceholderAPI remains M16, proxy delivery remains M20, NPC/hologram providers remain M21 and full legacy/runtime compatibility remains M22.
- **Entry:** M03 authorization/localization/configuration, M04 storage/outbox, M05 scheduler/recovery/failure substrate, M06 primary Paper 1.21.1/world foundation and M07 validated arena leases/use cases are merged and stable; the M08/M09 allocation validator passes; `zbw-game` has no dependency on an M09-or-later module; the milestone-qualified `zbw-paper-modern` dependency on `zbw-game` activates only in M08.
- **Deliver:** materialize `zbw-game` as the Java-8 presentation-neutral owner of deterministic match/session/team/lobby state machines, admission and player-state policies, restore/recovery and completion transaction orchestration. Extend the existing Java-21 `zbw-paper-modern` assembly only with closed feature-specific Paper 1.21.1 event translation, player-state effects, hotbar application, direct localized feedback, scoreboard/tab-list/boss-bar projection and stale-view cleanup. No production command, inventory GUI, editor, confirmation framework or public presentation extension API is delivered.
- **Exit:** transition/property/concurrency/recovery/authorization tests and exact primary Paper E2E cover waiting through reset, reconnect/crash and exactly-once end; player inventory/location/mode/state is always restored; item and projection tests prove no loss, duplication, action leakage or stale viewers; owner-thread and bounded-work rules pass. M08 use cases are completely exercisable through typed APIs and deterministic/test-only Paper harnesses. M08 verification closes only its core/application and primary-projection allocations and does not claim M09, M10, M16, M20, M21 or M22 acceptance; none of the four M09 command/UI modules is materialized.
- **Exit evidence:** `zbw-game` contributes 41 tests with zero skips and 95.89% line/88.26% branch coverage. The 16-module neutral reactor passes 199 tests on Java 8/11/16/17 and the 18-module Java-21 reactor passes 216; Paper test-JVM-safe coverage is 89.89% line/87.34% branch. Exact checksum-locked Paper 1.21.1 build 133 passes waiting-to-reset, reconnect, exactly-once completion, restoration, native boss-bar/listener cleanup and owner-thread rejection. Java matrix, Checkstyle, SpotBugs, binary/API, dependency/licence/provenance, JavaDoc and deterministic governance gates pass; all four M09 command/UI modules remain unmaterialized.

### M08.1 â€” Team configurability, arena-to-match assembly and layout matrix

- **Implementation status:** VERIFIED on 2026-07-16; evidence in `docs/IMPLEMENTATION_M08_1.md`. This corrective milestone hardens already-delivered M07/M08 foundations and does not start or close any M09 or M10 presentation/mode allocation.
- **Requirements:** corrective foundational portions of ZBW-GAME-001/002/004, ZBW-ARENA-002/008, ZBW-OPS-001, ZBW-QA-001/002 and overlapping ZBW-ADDON-156/411/419/421. No Requirement ID changes meaning or final owner: M09 still owns unified commands/GUIs and M10 still owns modes, matchmaking and selectors.
- **Entry:** M08 is merged and verified; the post-M08 hardcode audit identifies the 64-vs-32 team ceiling, capacity-boundary drift, missing arena-to-match assembly, name-heuristic generator validation, absent generic victory intent and anonymous arena defaults; M09/M10 modules remain unmaterialized.
- **Deliver:** one Java-8 neutral team-count/capacity authority; immutable runtime team identity/display/color/capacity; typed replaceable arena defaults; exact typed arena generator-validation profiles; a version-fenced enabled-arena-to-waiting-match assembler; and an overrideable deterministic victory evaluator that emits a typed completion intent while retaining the existing idempotent completion fence. Standard and custom layout definitions remain data, not engine branches. No Paper gameplay rule is added.
- **Exit:** Solo 8x1, Doubles 8x2, 3v3v3v3, 4v4v4v4, 4v4, custom 12x3 and shared-maximum 64x4 layouts assemble and exercise identity, color, capacity, assignment and admission without fixed indexes; a generic lifecycle test covers reconnect, elimination, victory intent, idempotent completion, restoration, reset and recovery; disabled, stale, capacity-inconsistent, group-inconsistent and mode-inconsistent definitions fail closed. Exact typed generator requirements reject substring lookalikes and custom profiles accept arbitrary registered generator types. Java matrix, M07/M08 regressions, Paper certification, quality, binary/API, dependency/licence/SBOM and governance gates pass with zero mandatory skips.
- **Exit evidence:** `TeamLayoutLimitsTest`, `M081ArenaHardeningTest` and `TeamLayoutMatrixTest`; 217 zero-skip tests on each Java 8/11/16/17 neutral reactor and 234 zero-skip tests on the full Java-21 reactor; M08.1 API-signature and strict-JavaDoc archives; JaCoCo remains above 90% line/85% branch for neutral domain/application modules; the checksum-locked Paper 1.21.1 build 133 certification was rerun successfully and regenerated `build/evidence/m08-paper-primary.json` without changing Paper production source. Checkstyle, SpotBugs, dependency/licence/provenance/SBOM and deterministic governance gates pass. `build/milestone-state.json` records M08.1 as completed hardening while the sequential completed milestone remains M08.

### M09 â€” Unified command and GUI frameworks

- **Implementation status:** VERIFIED on 2026-07-16; evidence in `docs/IMPLEMENTATION_M09.md`, the generated command/permission inventories and `build/evidence/m09-paper-primary.json`. This closes only M09 presentation allocations; M10 and later milestone behavior remains open.
- **Requirements:** UX-001..003/006 plus final command, GUI, editor and confirmation presentation portions of ARENA-001..009, GAME-001..003/006/008/010, ZBW-ADDON-001..009/108..114/124..130/148..154/334..340/398..407/424..437 and ZBW-ADDON-408..423.
- **Entry:** M03 authorization/localization and M05 async loading are ready; M07 supplies real arena/map/setup use cases with a stable typed boundary, and M08 is the first stable provider of real gameplay/lobby use cases plus closed primary Paper projections. Both use-case layers are independently tested before reusable presentation adapters are added.
- **Deliver:** unified command tree/help/audit, page renderer/state, common actor/action/target/expiry confirmation tokens, editor and accessibility patterns; Paper command/GUI adapters invoke the M07/M08 use cases and contain no arena or gameplay rules. M09 replaces no temporary M08 surface because M08 ships no production command or GUI framework.
- **Exit:** command/permission inventories and GUI interaction tests pass, including every arena/map/setup/game/lobby/addon/editor/confirmation mapping; command and GUI paths call identical application use cases; async data never blocks the tick thread; no feature policy is implemented in an adapter.
- **Exit evidence:** four M09 modules preserve Java 8 neutral and Java 21 Paper boundaries; 24 M09 tests pass with zero skips; 87 command actions, 87 granular permissions and 88 parity GUI pages are complete; exact Paper 1.21.1 build 133 certifies command dispatch, inventory rendering, parity, off-owner bounded work and duplicate prevention. Clean matrix, quality, strict JavaDoc, binary/API, dependency/licence/provenance/SBOM, governance, traceability, addon catalogue, documentation and dashboard gates pass.

### M10 â€” Selectors, matchmaking, mode selection and spectator framework

- **Requirements:** M10 completion portions of ZBW-GAME-004/005/007/009 and ZBW-CONTENT-003; ZBW-ADDON-092..101, 115..123, 131..140 and 155..163; registration/selection portions only of ZBW-ADDON-236..244. M10 implements the shared-server behavior of the non-mode addon rows except the explicitly split M16 PlaceholderAPI, M20 proxy-routing and M22 legacy-compatibility cells.
- **Entry:** M08/M08.1 game and configurable-layout foundations and M09 unified command/GUI frameworks are complete; the deterministic pre-implementation governance, traceability, catalogue and dashboard gates pass.
- **Deliver:** Java-8-neutral standard/custom mode registration and selection SPI with explicit deferred bindings for every later named mode, including Swappage; typed selectors; deterministic bounded shared-server queues, reservations and M08 assignment delegation; party-aware team selection; spectator lifecycle/options/menu; tracker and quick communications. M09 commands and GUIs remain adapters over the same typed use cases.
- **Exit:** selector, queue, reservation, party, spectator, Compass and Team Selector matrices pass for standard and custom layouts, concurrency, rejoin/disconnect, cleanup and exact Paper 1.21.1 projection. Mode registration/configuration/event contracts and deferred bindings are independently validated. All named-mode gameplay mechanics, including Swappage, and mode-specific shop/generator/upgrade/balance behavior remain M11; statistics remain M15; PlaceholderAPI remains M16; proxy-wide routing remains M20; legacy compatibility remains M22. No later-milestone mechanic may be reported complete from framework registration alone.
- **Evidence:** `docs/IMPLEMENTATION_M10.md`, M10 framework/API guides, generated 115-action command and permission inventories, API/JavaDoc baselines, quality reports and exact Paper primary-runtime evidence. M11 remains unstarted.

### M11 â€” Shop, item, generator and upgrade platform

- **Requirements:** M11 mechanics/component portions of GAME-004/005 and READY-004/015; SHOP-001..007; CONTENT-002/003; completion portions of ZBW-ADDON-236..244; ZBW-ADDON-010..025, 061..070, 141..147, 184..201, 300..322, 341..349, 363..368, 379..397, 438..452. M11 closes only the allocations stated below; final requirement completion waits for every retained later cell.
- **Entry:** M08 event engine, M09 command/UI frameworks and M10 mode SPI are complete; RC-086 governance reconciliation passes the module-graph, traceability, catalogue, dashboard and coverage validators with M11 still not active.
- **Implementation status:** VERIFIED and completed on 2026-07-19. PR #18 was squash-merged to `main` as `3e68835c361216e6dc8be37b9e024734bb565884`; its mandatory remote Java, governance, API, database and Paper 1.21.1 certification jobs completed successfully. The earlier `M11.1-MERGE-EXCEPTION-001` remains an accurate record of the temporary runner outage and local 346-test/36-governance-test evidence, but is superseded for closure by the successful immutable-commit remote evidence. M12 is next planned and inactive.
- **Deliver:** Java-8-neutral `zbw-shop`, `zbw-content`, `zbw-scripting-api` and `zbw-scripting-engine`; catalog/purchase/quoted-tender services; Quick Buy; match-local iron/gold/diamond/emerald/custom/multiple tenders; generators; team upgrades/forge/traps; all named-mode gameplay mechanics including Swappage; original shop/mode profiles; all original utility items; and the disabled-by-default declarative action sandbox. Feature commands, permissions and GUIs extend the existing M09 frameworks. Paper code performs primary-runtime translation only and contains no feature policy.
- **Retained ownership:** M12 owns persistent progression/virtual-currency ledgers and providers; M15 owns all mode/addon statistics; M16 owns every PlaceholderAPI cell; M19 owns Redis coordination and M20 proxy/server distribution, including the distributed portion of ZBW-ADDON-387; M21 owns Vault plus concrete NPC/hologram/shopkeeper providers; M22 owns legacy adapters, fallbacks and full 1.8â€“1.21.x certification. M11 may publish typed ports/events/state for those consumers but may not implement them.
- **Mode boundary:** M10 registration/selection and deferred bindings remain authoritative. M11 supplies named-mode mechanics and configured component/balance packs without recreating M08 lifecycle or M10 selection. The GAME-004 `Adventure` mode binding/profile is distinct from the M08/M09 `AdventureMode` player-state-transition addon ZBW-ADDON-432..437, whose allocation is unchanged.
- **Exit:** item/purchase/generator/upgrade/mode matrices, atomicity/exploit, persistence/rejoin and high-GUI-load tests pass; scripts/actions meet the accepted sandbox policy; feature-specific admin/API/config/M09-presentation and primary Paper 1.21.1 surfaces are complete. M11 exit evidence explicitly leaves M12/M15/M16/M19/M20/M21/M22 cells open and cannot claim complete statistics, placeholders, distributed delivery, external providers or legacy compatibility.

### M12 â€” Progression transaction core

- **Requirements:** PROG-001..005, PROG-011; ZBW-ADDON-174..183, 210..216, 245..251, 266..282.
- **Entry:** M04 outbox, M08 events and the completed M11 quoted-tender/match-resource contracts are stable; RC-087 is resolved; M12 is now active and all predecessor gates are satisfied.
- **Phase 1â€“5 status:** VERIFIED and completed on 2026-07-21. Phases 1â€“4 were completed in commits
  `8aa3c879cf0d1dd060ea143de87a0cc01950467c`, `7569ddd364801de49e5053ac7980fe197de0ec01`,
  `f591ec3f3c0f34c95cd4235e2222459a163731c8` and `c2f139c`; Phase 5 performs final closure and certification.
- **Deliver:** event projection, XP/level/prestige, persistent/virtual currencies, immutable transaction ledger and unified transactional reward engine with offline/cross-server-ready delivery contracts. M09 player/admin commands, M09 GUI presentation and Java 21 owner-thread Paper projections are complete. M15 retains statistics, M16 PlaceholderAPI, M17 replay, M18 Atlas, M19/M20 distributed/proxy transports, M21 Vault/NPC/hologram providers and M22 legacy compatibility.
- **Exit:** duplicate/retry/crash tests cannot double award; formula/migration/admin/audit/recovery tests pass; reward summary has complete presentation adapters; M11 settlement integration and M11.1 command/GUI ownership are confirmed; full M12 exit evidence is recorded in phase-5 documentation.

### M13 â€” Objectives, quests, achievements and battle pass

- **Implementation status:** VERIFIED and completed on 2026-07-22. Phase checkpoints are `162b62cdfe64d0f9979d19cb78fe7057154c9529`, `8915963f47907dfd27903040381fa60d9eca174f` and `2b22e83bb22102719003c2cd1485165b4a21958f`; final evidence is recorded in `docs/IMPLEMENTATION_M13_PHASE1.md`, `docs/IMPLEMENTATION_M13_PHASE2.md` and `docs/IMPLEMENTATION_M13_PHASE3.md`. This closes only M13 ownership.
- **Requirements:** PROG-009/010/012/013; CONTENT-004..006; ZBW-ADDON-081..091.
- **Entry:** M12 reward/progression core and M09 UI ready.
- **Phase 1â€“3 status:** complete. Java 8 neutral definitions/catalogue, deterministic objective execution, quest/achievement/challenge/pass runtime, M08/M11/M12 event adaptation, M12 reward intents, M04-backed SQL state/recovery and M09 command/GUI plus Java 21 Paper presentation are implemented and validated.
- **Deliver:** reusable objective engine, every listed objective/scope, quest/achievement/challenge/pass definitions and editors.
- **Exit:** satisfied by objective/reward catalogue, lifecycle, duplicate/restart/recovery, claims/chains, permission/audit/confirmation, GUI stale-state, Paper owner-thread, Java 8/21 reactor, quality, API, governance and SBOM/licence evidence. No objective logic is duplicated in Paper.

### M14 â€” Cosmetics, profiles and calendar rewards

- **Milestone status:** VERIFIED and completed; Phase 1 foundation and the runtime/presentation implementation are recorded in `docs/IMPLEMENTATION_M14_PHASE1.md`, `docs/API_M14_PHASE1.md`, `docs/IMPLEMENTATION_M14_COMPLETE.md` and `docs/API_M14_COMPLETE.md`. M15 is the next milestone and no later milestone is currently active.
- **Requirements:** PROG-006..008/014; CONTENT-007/009/011; ZBW-ADDON-026..040, 369..378.
- **Entry:** M12/M13 complete; original content/license ADR accepted.
- **Deliver:** 300 original definitions, rarity/ownership/equipment/effects, profile/privacy/settings and holiday/calendar rewards.
- **Phase 1 boundary:** typed cosmetic/profile/calendar models, M12 reward/entitlement and M13 unlock references, validation/configuration contracts and caller-owned M12 storage ports only. Concrete persistence, runtime effects, content batches and M09 presentation remain later M14 phases.
- **Exit:** catalog count/license scan, ownership/expiry/preset/migration and rate-limit load tests pass; low-performance/emergency controls meet budgets.

### M15 â€” Statistics, ratios, streaks and leaderboards

- **Requirements:** STATS-001..008; ZBW-ADDON-217..225, 260..265, 350..356.
- **Entry:** M08 event schema and M12 projection/idempotency patterns stable.
- **Deliver:** authoritative projections, all dimensions/ratios/streaks, administration and cached ranking engine, including every isolated-statistics cell retained from M11 named modes and addon mappings.
- **Exit:** duplicate/private/test separation, ratio/tie/reset/repair/migration and large-data ranking tests are implemented; replay-safe rebuild/restart projections, leaderboard tie ordering/pagination and stale-view protection are in place; no full-table query per request.
- **Phase status:** VERIFIED and completed in checkpoints `965291c`, `9758c71`, `51af49b`, `0cee0b3`, `badd504`, `586797f` with full M15 scope (foundation, persistence adapters, event adapters, leaderboards and final M15 closure validation evidence).

### M16 â€” PlaceholderAPI and external statistics surfaces

- **Requirements:** PAPI-001..006, OPS-007, DISCORD-001..008; ZBW-ADDON-071..080, 202..209, 357..362, 453..463.
- **Entry:** M12â€“M15 visible data contracts stable; M05 metrics ready.
- **Deliver:** native expansion, dynamic contexts/families/formatters/admin tools/docs generator and secured external/Discord provider APIs, including all shop, generator, upgrade, item and named-mode placeholder cells exposed by M11 state contracts.
- **Exit:** placeholder inventory/context/offline/fallback/cache/parser tests pass; zero sync I/O and p95 budget verified; privacy/scope/rate tests pass.

### M17 â€” Replay recording, storage and viewer

- **Requirements:** REPLAY-001..010.
- **Entry:** M08 canonical events, M05 queues, M04 metadata storage, replay ADR accepted.
- **Phase status:** COMPLETE — Phases 1-10 deliver immutable replay contracts, deterministic M08/M11/M12 ingestion, transactional SQL persistence, platform-neutral playback, privacy/access policy, bounded Paper spectator/viewer/visual controls, staff search/moderation/audit and fail-closed lifecycle cleanup. Web, Redis, external hosting/provider adapters, packet/NPC rendering and cross-version visual fallbacks remain with M19-M22 and final scale/restore certification remains M24.
- **Deliver:** capture/codec/manifest/store/retention/recovery/privacy, playback scene/rendering, timeline/telemetry/staff evidence and all surfaces.
- **Exit:** golden timing/order, interruption/corruption/repair/hold/privacy/provider failure and performance tests pass; evidence degradation policy demonstrated.

### M18 â€” Atlas case and review platform

- **Requirements:** ATLAS-001..013; ZBW-ADDON-323..333 controlled staff tooling.
- **Entry:** M12 rewards, M14 profile, M15 stats and M17 replay complete; moderation/privacy ADRs accepted.
- **Phase 1 status:** GOVERNANCE ALLOCATED. `zbw-atlas-api`, `zbw-atlas` and
  `zbw-atlas-sql` are allocated as Java 8 modules with one-way neutral dependencies.
- **Phase 2 status:** API FOUNDATION. `zbw-atlas-api` materializes immutable case, evidence,
  review, privacy, repository-port and event contracts for `ZBW-ATLAS-001/003/004/005/006/011`.
  The module depends only on `zbw-api` and the M17 replay API. Atlas core, SQL, Paper surfaces,
  punishment policy and the remaining M18 workflow are not implemented by this checkpoint.
- **Phase 3 status:** CORE AND SQL COMPLETE. Java 8 workflow, evidence references, eligibility, reservations, advisory verdicts, integrations, checksum-locked persistence and separated identity vault are materialized.
- **Phase 4 status:** COMPLETE. The Java 21 Paper adapter provides asynchronous `/atlas` routing, sanitized staff/reviewer projections, owner-thread presentation and cleanup. `ZBW-ADDON-323..333` use exact dotted permissions and guarded reason/confirmation/immunity/audit/rollback ports; game lifecycle ownership remains unchanged.
- **Closure evidence:** `ATLAS_M18.md`, Atlas/Paper unit suites, strict API/JavaDoc and governance validators. M19 is next but is not started by this closure.- **Deliver:** cases/anonymization/reservation/verdict/reputation/accuracy/anti-abuse/rewards/staff policy and all Atlas surfaces.
- **Exit:** eligibility/bypass/conflict/anonymization/reservation/abuse/reward/override tests pass; no default community permanent punishment; gameplay budgets hold.

### M19 â€” Redis and distributed consistency

- **M19 Phase 1 status:** COMPLETE — zbw-redis-api is materialized as a Java 8 neutral API. ZBW-DEPLOY-006/008/009 and ZBW-READY-013/014 receive immutable key, M04-envelope stream, invalidation, deduplication, reservation, monotonic fencing, and sanitized degradation contracts.
- **M19 Phase 2 status:** COMPLETE — zbw-redis provides a Java 8 nonblocking Lettuce adapter with at most 16 connections, a 5,000-operation bounded queue, namespace/schema fail-closed guards, Pub/Sub invalidation, ordered duplicate-safe stream processing, 24-hour/250,000-entry dedupe, fenced leases, HMAC-SHA-256 rotation/nonces/deadlines/size/rate checks, finite jittered reconnect policy, circuit breaking and sanitized degradation health. Redis remains ephemeral; durable exactly-once business effects remain SQL-owned. No Paper, proxy, domain bridge or M20 implementation is introduced.
- **M19 Phase 3 status:** COMPLETE — bounded coordination bridges provide versioned statistics/leaderboard invalidation, Atlas fenced reservation safety, M11 item-rotation version synchronization and opaque presence/arena/queue/replay/announcement events. M04 envelopes and M19 streams retain ordering, schema validation and duplicate protection. Redis loss triggers rebuild/local-safe degradation and pauses unsafe cross-node reservation; SQL and each domain owner remain authoritative. No Paper, proxy or M20 behavior is introduced.
- **M19 Phase 4 status:** COMPLETE — deterministic failure/recovery tests cover unavailable Redis, reconnect/circuit behavior, duplicates, ordering, schema rejection, stale fencing, partition-safe reservation, cache loss/rebuild/restart and the five-minute coordination RTO. SHARED_40 and PROXY_4-equivalent adapter benchmarks enforce p95 ≤5 ms, p99 ≤15 ms and memory bounds; production Redis/TLS network evidence remains an explicit rollout gate in `REDIS_M19.md`. M19 is complete; M20 remains unstarted.

- **Requirements:** DEPLOY-006/008/009.
- **Entry:** M04 outbox, M12/15/17/18 durable semantics complete; Redis ADR accepted.
- **Deliver:** versioned keys/messages, invalidation/streams/locks/fencing/leader election, health/circuit/degradation, including coordination for the active item-rotation state defined locally by M11 under ZBW-ADDON-387.
- **Exit:** partition/reconnect/duplicate/order/rolling-schema/lock-expiry load tests pass; no split-brain finalization and no unbounded polling.

### M20 â€” Proxy networking and scalable deployment

- **M20 Phase 1 status:** IN PROGRESS — `zbw-proxy-api` is materialized as a Java 8 neutral API; `zbw-bungeecord` and `zbw-velocity` remain planned only. ZBW-DEPLOY-002..004 receive immutable backend identity/epoch/capability, registry, heartbeat/capacity/health, deterministic routing, bounded reservation, single-use transfer-token outcome, protocol/degradation and sanitized diagnostic contracts. M04/M19 envelope, authentication, dedupe, lease and fencing ownership is reused rather than duplicated. No proxy adapter, Paper integration, Redis runtime change or M21 behavior exists.
- **M20 Phase 2 status:** IN PROGRESS — `zbw-bungeecord` (Java 8) and `zbw-velocity` (Java 21) are materialized over one Java 8 neutral runtime. ZBW-DEPLOY-002..004 and ZBW-READY-013/014 now have bounded epoch-aware registry/heartbeat/drain state, deterministic capacity routing with retry/fallback, atomic 15-second reservation/transfer consumption, HMAC-SHA-256 key rotation, version/environment/audience/deadline/nonce validation and identical nonblocking adapter semantics. M10 matchmaking and every M11-M19 durable/domain owner remain unchanged; cross-server feature flows remain later M20 work.

- **Requirements:** DEPLOY-002..004, distributed portions of GAME/PROG/STATS/REPLAY/ATLAS/INT-009; CONTENT-008; ZBW-ADDON-041..060, 102..107, 164..173, 252..259, 291..299, 464..473.
- **Entry:** M19 complete and M10 routing contracts stable.
- **Deliver:** Velocity and Bungee adapters, backend registry/reservations/transfers/failover/drain and cross-server user flows, including proxy/server distribution of the M11 item-rotation state after M19 coordination is available.
- **Exit:** provider-equivalence and signed-message security tests pass; crash/retry/fallback/duplicate player and cross-server queue/party/rejoin/play-again E2E pass.

### M21 â€” CloudNet, parties and remaining providers

- **Requirements:** DEPLOY-005, INT-001..003/006..009; ZBW-ADDON-226..235.
- **Entry:** M20 scalable routing and relevant feature APIs stable.
- **Deliver:** CloudNet scaling; native/external parties; Placeholder/Vault/LuckPerms/NPC/hologram/Grim/Vulcan adapters and dashboards. Vault tender, shopkeeper/NPC and generator-hologram providers consume M11 ports and remain M21 implementations.
- **Exit:** every pinned supported provider passes shared contract/failure/version tests; both anticheats can run together without duplicate cases; scale/drain/crash replacement works.

### M22 â€” Full 1.8â€“1.21.x and Bedrock compatibility matrix

- **Requirements:** completion of INT-010, COMPAT-001..009 and READY-001/002/006; remaining ARENA/SHOP/REPLAY/UX and feature compatibility acceptance, including every legacy/fallback cell retained from M11 shop, item, generator and named-mode mappings. M06 foundations remain explicit and are not reclassified as full completion.
- **Entry:** M06 compatibility/world-provider contracts and primary 1.21.1 foundation certification pass; all feature semantics and their required fallback rows/fixtures are stable; exact legacy/private and maintained runtime fixtures are lawfully acquired and hash-locked.
- **Deliver:** `zbw-compat-v1_8`, all other legacy/intermediate compatibility modules, matching Java 8/11/16/17 Paper bootstraps, complete legacy mappings/fallbacks, extensions to the modern adapter for every declared modern row, ViaVersion/ViaBackwards/ViaRewind integration and Geyser/Floodgate alternatives.
- **Exit:** every declared 1.8.8â€“1.21.11 server row, including release-level revalidation of the M06 primary row, passes its required build/startup/lifecycle/gameplay/GUI/item/packet/replay/provider/fallback suite on the exact JDK/fixture; translated Java and Bedrock matrices pass independently; no unsupported platform type is exposed; limitations are explicit and only owner-approved alternatives are accepted. Full 1.8â€“1.21.x certification remains a release gate.

### M23 â€” Migration, ecosystem and operational completion

- **Requirements:** ECO-001..005, OPS-006/009; ZBW-ADDON-283..290.
- **Entry:** Feature schemas/APIs stable; source formats legally documented.
- **Deliver:** migration assistants, SDK/example, marketplace metadata, extensible doctor, complete operator/developer guides.
- **Exit:** dry-run/backup/map/duplicate/rollback reports pass on fixtures; sample extension uses public API only; all required docs/reference inventories are complete.

### M24 â€” Security, performance and release qualification

- **Requirements:** QA-001..007, GOV-007/011, OPS-008, CONTENT-001/011, LICENSE-001..007, READY-001..020 and final acceptance of all 672 semantic IDs and atomic children.
- **Entry:** M01â€“M23 complete with no mandatory open implementation work.
- **Deliver:** threat/privacy/license review, compatibility report, full benchmark/security report, recovery exercise, reproducible artifacts, final compliance report and release notes.
- **Exit:** clean release CI; all budgets/matrices pass; vulnerability exceptions are approved; every one of 199 Part I IDs, 473 addon IDs and every `MP-L####` child has an allowed final status and evidence; dependency/asset SBOM, notices and fallback matrices match artifacts; atomic functional coverage remains 100%; no unresolved mandatory requirement.

## Native addon allocation summary

The authoritative row-level allocation is the `Milestone` column of `docs/ADDON_FEATURE_CATALOG.md`. The grouped ranges below are a planning index and do not merge their child requirements.

| Milestone | Addon references and stable requirement ranges |
|---|---|
| M07 core / M09 presentation | ArenaSetup (`ZBW-ADDON-408..423`): atomic policies/use cases/lifecycle in M07; all mapped command and GUI adapters in M09 |
| M08 core/primary Paper / M09 final presentation | HotbarManager (`001..009`); Deposit (`108..114`); Arena Start Message (`124..130`); AntiDrop (`148..154`); LeaveDelay (`334..340`); TabSorter (`398..407`); BossBar (`424..431`); AdventureMode (`432..437`). M08 owns engine/application behavior and closed Paper 1.21.1 projections; M09 owns every mapped command, GUI, editor and confirmation surface; M16/M20/M21/M22 retain their explicit later portions. |
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
| RC-073 original content/provenance | CONTENT-001..011 | M01 provenance gate; M02/M03 content registry | M10â€“M14 and M20 by catalogue family | M24 asset/content/SBOM scan |
| RC-074 Discord providers | DISCORD-001..008 | M02 API/SPI, M03 secrets/default, M05 failure substrate | M16 | No-provider/outage/security/protocol tests |
| RC-075 1.8 fallbacks | COMPAT-001..009 | M01 toolchain matrix; M06 neutral contract, primary mappings and Paper 1.21.1 foundation only | M22 legacy adapters, complete fallbacks and full matrix | Every fallback row/fixture passes; M06 makes no 1.8 claim |
| RC-076 dependency licensing | LICENSE-001..007 | M01 exact artifact approvals before Java | Continuous dependency changes | M24 reproducible notices/SBOM/artifact audit |

## Consolidated readiness decision allocation

Canonical requirements allocated below: `ZBW-READY-001`, `ZBW-READY-002`, `ZBW-READY-003`, `ZBW-READY-004`, `ZBW-READY-005`, `ZBW-READY-006`, `ZBW-READY-007`, `ZBW-READY-008`, `ZBW-READY-009`, `ZBW-READY-010`, `ZBW-READY-011`, `ZBW-READY-012`, `ZBW-READY-013`, `ZBW-READY-014`, `ZBW-READY-015`, `ZBW-READY-016`, `ZBW-READY-017`, `ZBW-READY-018`, `ZBW-READY-019`, `ZBW-READY-020`.

| Decisions | Stable IDs | Foundation milestone | Dependent feature milestones | Final evidence |
|---|---|---|---|---|
| RC-003/004/022 runtime matrix | READY-001/002/006 | M01 artifacts/toolchains/fixtures | M06 Paper 1.21.1 foundation only; M22 all full runtime/client rows | Per-row boot/game/fallback evidence; full 1.8â€“1.21.x release gate remains M22 |
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

`M00 â†’ M01 â†’ M02 â†’ (M03, M05) â†’ M04/M06 â†’ M07 â†’ M08 â†’ M09/M10 â†’ M11/M12 â†’ M13â€“M16 â†’ M17 â†’ M18 â†’ M19 â†’ M20 â†’ M21/M22 â†’ M23 â†’ M24`.

Parallel work is allowed only where this graph and module boundaries show no unmet data/API dependency.

#### M17 Phase 8 checkpoint

Replay UX and controls now provide the Paper command/menu foundation, exact bounded speed choices, synchronized timeline information, permission-preserving inspection and viewer-isolated cleanup for ZBW-REPLAY-004/005/007/010. Web replay, Redis, external providers, advanced GUI and cinematics remain outside this checkpoint.

#### M17 Phase 9 checkpoint

Replay staff tools add bounded metadata/event search and inspection, player/match/date/duration filters, least-privilege `replay.staff`/`replay.admin` actions, mark/archive/failed-only removal and deterministic asynchronous audit for ZBW-REPLAY-001/005/006/007/008/009/010. Durable provider implementations, web replay, Redis, external providers and M17 closure remain outside this checkpoint.

#### M17 Phase 10 closure

M17 closes with atomic 256-viewer admission, 128-entity/256-event visual bounds, bounded 100-row staff normalization, fail-closed Paper projection cleanup, archived/failed lifecycle rules and the operational record in `REPLAY_M17.md`. The three replay modules and one-way Paper dependencies remain unchanged. ZBW-REPLAY-001..010 and ZBW-READY-009/010/011/016/017/018 have implementation evidence for the M17 allocation; provider, distributed, cross-version and release-scale qualification stays explicitly allocated to M19-M22/M24. M18 is next but is not started by this checkpoint.
