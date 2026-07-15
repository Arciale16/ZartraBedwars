# ZartraBedWars Risks, Conflicts and Missing Decisions

## Reading guide

- **Fact:** directly stated or logically forced by `MASTER_PROMPT.md`/platform semantics.
- **Assumption:** a provisional interpretation used to preserve scope.
- **Recommendation:** a proposed resolution; owner approval is still required where marked.

No item below removes a requested capability.

## A. Contradictions and ambiguities

| ID | Type | Finding | Preserving resolution | Decision |
|---|---|---|---|---|
| RC-001 | Fact | Early text orders PRD → architecture → specifications → plan; later implementation order says architecture → PRD validation. | Establish PRD baseline first, then architecture and validate them bidirectionally before code. | Adopted for M00; owner confirm |
| RC-002 | Fact | Later order places documentation/testing/optimization near the end, while many rules require them in every milestone and define docs/tests as implementation. | Perform continuous docs/tests/performance review per milestone; late steps are whole-product qualification only. | Owner confirm |
| RC-003 | Fact — **RESOLVED 2026-07-14** | Primary Paper 1.21.1/Java 21 cannot share one Java 21-only JAR with mandatory 1.8-era JVMs. | `ZBW-READY-001`, ADR-0006 and `RUNTIME_COMPATIBILITY_MATRIX.md` select a Java-8 core plus separate Java 8/11/16/17/21 platform artifacts and enforce bytecode/import/boot tests. | Accepted technical architecture |
| RC-004 | Owner interpretation — **RESOLVED 2026-07-14** | Server-runtime compatibility and translated client compatibility were ambiguous. | `ZBW-READY-002` and ADR-0006 require both as independently certified matrix dimensions; a Via/Geyser client path never substitutes for a server adapter. | Accepted owner decision |
| RC-005 | Fact | “Latest stable” moves and cannot be verified in advance. | Support only matrix-tested releases; add each new stable through adapter + CI row while retaining the long-term requirement. | ADR |
| RC-006 | Fact | “Everything configurable” can conflict with integrity, security and protocol invariants. | Make operator policy configurable within validated safe ranges; IDs, atomicity, auth, integrity and schema invariants are not disable-able. | ADR/config policy |
| RC-007 | Fact | Database/Redis/filesystem/map cloning are labeled async, but Bukkit/Paper world/entity operations often require an owner thread. | Split workflows: I/O/encoding async, platform attach/mutation on documented scheduler context. | Architecture rule |
| RC-008 | Fact | “Migration rollback” is not always transactional for SQL DDL or external providers. | Require transaction rollback where supported; otherwise verified pre-backup + restore/forward-repair with identical operator outcome. | DB ADR |
| RC-009 | Fact | Map ID must survive import/export yet duplicate must get a new ID; collision behavior is unspecified. | Import preserves ID if unique; explicit duplicate creates new ID; collision imports require map/merge/reject policy and audit. | ID/migration ADR |
| RC-010 | Fact | “Changing Display Name must never modify Database” conflicts with persisting an editable display name. | Persist the display field but never change identity keys, relations, statistics or historical snapshots. | PRD assumption; owner confirm |
| RC-011 | Fact | “No generic permissions” conflicts with category examples such as admin/staff/VIP/player and “staff may always access.” | Categories are role recommendations; every action has a granular canonical node. “Staff” access requires the node, not a name. | Permission ADR |
| RC-012 | Fact | Atlas permission nodes are repeated with dot and hyphen spellings. | Select one canonical dotted action namespace and retain documented aliases/migration checks. | Permission ADR |
| RC-013 | Fact | Lifecycle statuses allow many values, but final reports allow only two. Another section uses “explicitly documented impossible,” while final uses “technically impossible with approved alternative.” | Use lifecycle statuses during work; final report uses the stricter two values, with owner-approved alternative. | PRD rule |
| RC-014 | Fact | “No future work/TODO” conflicts with mandatory long-term roadmap, marketplace and AI-oriented future design. | Track future scope as stable SHOULD/MAY requirements and ADRs, never as unowned code TODOs or fake implementation claims. | Governance rule |
| RC-015 | Fact | Some items say “where technically feasible/practical” or “where appropriate,” while the containing part says everything is mandatory. | Keep the capability mandatory; infeasibility requires evidence and approved alternative under final-status rules. | Owner approval process |
| RC-016 | Ambiguity | “Equivalent quality” and “easier than BedWars1058” are not measurable. | Use task completion time/error rate/usability test criteria and original UX patterns; freeze measures in a UX ADR. | Missing acceptance decision |
| RC-017 | Owner content decision — **RESOLVED 2026-07-14** | 300 built-in cosmetics lacked countable quality/asset depth. | `ZBW-READY-003`, ADR-0012 and `COSMETIC_PRODUCTION_PLAN.md` define five 60-item batches, count rules, provenance, performance/fallback/accessibility and automated/human gates. | Accepted owner decision |
| RC-018 | Security decision — **RESOLVED 2026-07-14** | “Custom logic” could imply arbitrary code execution. | `ZBW-READY-004`, ADR-0008 and `SCRIPTING_SECURITY.md` select a disabled declarative capability DSL with explicit instruction/time/memory/scope/audit controls and no host/JVM access. | Accepted security architecture |
| RC-019 | Fact | PDC does not exist on legacy 1.8; RGB/MiniMessage and modern materials/sounds/packets are not representable on all clients. | Compatibility SPI maps PDC↔legacy NBT/data and deterministic visual/text fallbacks; gameplay semantics remain. | Compatibility ADR |
| RC-020 | Fact | Minecraft inventory GUIs cannot provide general keyboard shortcuts on all Java/Bedrock clients. | Implement protocol-supported number/shift/hotbar actions and Bedrock/chat/command alternatives; document exact capability matrix. | Compatibility ADR |

## B. Missing dependency and compatibility decisions

| ID | Classification | Missing information / risk | Recommendation |
|---|---|---|---|
| RC-021 | Fact — **RESOLVED 2026-07-14** | Platform/provider/library coordinates, versions and licences were unpinned. | `ZBW-READY-005`, ADR-0007 and `DEPENDENCY_LICENSE_AUDIT.md` select exact baselines, scopes and a checksum/licence pre-resolution lock; floating and dynamic release dependencies are forbidden. |
| RC-022 | Fact — **RESOLVED 2026-07-14** | Exact test-server distributions and JDKs were absent. | `ZBW-READY-006`, ADR-0006 and the runtime matrix fix private BuildTools legacy rows and exact Paper build/hash/JDK rows; no server binary is redistributed. |
| RC-023 | Fact | SlimeWorldManager/provider naming/version/API availability may differ by server line. | Select maintained provider coordinates and implement SPI aliases/migration; claim only tested versions. |
| RC-024 | Technical/licensing decision — **RESOLVED 2026-07-14** | Grim/Vulcan API access and licence models differ. | `ZBW-READY-007` and ADR-0007 select the audited Grim public baseline plus a neutral optional operator-licensed Vulcan adapter/no-provider path; no proprietary binary/API is copied. |
| RC-025 | Fact | Object storage is required only as an adapter API; no concrete S3-compatible provider, credentials model or consistency behavior is chosen. | Define SPI now; decide first concrete provider, multipart/checksum/encryption semantics before replay milestone. |
| RC-026 | Fact | External REST/Discord integration technology, bind address, TLS termination and authentication provider are unspecified. | ADR for embedded vs sidecar API, OAuth/API-key scopes, reverse proxy and default-disabled behavior. |
| RC-027 | Technical owner decision — **RESOLVED 2026-07-14** | Framework/library choices were absent. | `ZBW-READY-008`, ADR-0007 and the dependency audit select manual DI, project command/GUI DSLs and exact config/serialization/migration/cache/Redis/metrics/codec/test/benchmark/quality tools. |
| RC-028 | Fact | Native party requirements overlap AlessioDP provider semantics; conflict/source-of-truth/migration behavior is unspecified. | Party provider ADR defines ownership, live switch restrictions, ID mapping and rollback. |

## C. Performance and scalability risks

| ID | Classification | Risk | Mitigation / missing acceptance |
|---|---|---|---|
| RC-029 | Owner performance decision — **RESOLVED 2026-07-14** | Scale/TPS language lacked hardware, workloads, percentiles and numeric targets. | `ZBW-READY-009`, ADR-0009 and `BENCHMARK_BASELINE.md` fix reference hardware, small/shared-40-world/proxy profiles and TPS/MSPT/memory/world/DB/Redis/replay/PAPI thresholds. |
| RC-030 | Fact | Ten active arenas plus replay, cosmetics, NPCs, holograms, placeholders and anticheat can exceed one Paper tick budget. | Per-feature budgets, adaptive degradation, distance/rate limits, bounded work and representative combined-load test—not isolated microbenchmarks only. |
| RC-031 | Fact | Async world clone/reset can saturate disk, heap and chunk lifecycle. | Concurrency semaphore, streaming copy, provider snapshots, disk-space checks, owner-thread attach and backpressure metrics. |
| RC-032 | Fact | Replay fidelity (movement/projectiles/inventory/telemetry) can create extreme allocation, I/O and storage volume. | Replay ADR selects sampling/delta/chunk sizes and retention tiers; reserved evidence capacity, load tests and quota alerts. |
| RC-033 | Fact | “Frame step,” exact projectile path and first-person reconstruction cannot always be exact from sampled server data. | Label exact/sampled/derived/estimated/unavailable; increase capture only for configured evidence windows; never overstate accuracy. |
| RC-034 | Fact | Placeholder fan-out and dynamic dimensions can cause cache explosion or database chatter. | Bounded keyed caches, precomputed views, per-tick dedupe, no sync I/O, cardinality quotas and p95/p99 metrics. |
| RC-035 | Fact | Global/dimensional leaderboards can require expensive rankings over large data. | Indexed projections/materialized top-N and player-rank caches, scheduled incremental rebuild, stale-while-revalidate. |
| RC-036 | Fact | Redis locks/leader election can cause split brain under pauses/partitions. | Fencing tokens, TTL, SQL uniqueness/source of truth and chaos tests; locks never protect money alone. |
| RC-037 | Fact | Hundreds of cosmetic effects and packet NPCs can saturate network/entity tracking. | Packet/entity budgets, visibility culling, per-player settings, low-performance mode and global kill switch. |
| RC-038 | Fact | Unbounded histories are explicitly prohibited but retention limits are unspecified for transactions, stats, reviews and audits. | Data-class retention ADR with pagination/archive/aggregation and legal requirements. |

## D. Security, privacy and legal risks

| ID | Classification | Risk | Mitigation / decision |
|---|---|---|---|
| RC-039 | Fact | Community moderation can cause false punishment, bias, brigading and harm. | Default staff final review; anonymization/conflict checks, trusted outcomes, appeals, audit and explicit threshold policy. |
| RC-040 | Privacy/legal owner decision — **RESOLVED 2026-07-14** | Replay personal-data purpose/defaults were unspecified. | `ZBW-READY-010`, ADR-0010 and `PRIVACY_AND_RETENTION.md` set chat off, metadata-only default, purpose access, encryption, notice/export/delete and explicit jurisdiction-reviewed opt-in. |
| RC-041 | Privacy/legal owner decision — **RESOLVED 2026-07-14** | Erasure conflicts with open evidence/legal hold. | `ZBW-READY-011` and ADR-0010 fix retention classes; ordinary data deletes within 30 days while scoped held evidence is identity-separated/pseudonymized until audited release. |
| RC-042 | Legal concern | Recording VPN/shared-environment indicators may be invasive or unlawful in some jurisdictions. | Default disabled; data-protection assessment, minimization and jurisdiction-specific opt-in policy. |
| RC-043 | Legal/provenance decision — **RESOLVED 2026-07-14** | Network/addon inspiration, terminology and migration raise copyright/trademark/API-licence risks. | `ZBW-READY-012`, ADR-0013 and the licensing/provenance policies require clean-room original neutral output, lawful operator-owned migration input and public/commercial legal review without reducing functionality. |
| RC-044 | Fact | External store premium access hooks can create payment/chargeback/entitlement obligations. | Adapter only; signed/idempotent entitlement events, revoke/audit and no card data handling. |
| RC-045 | Fact | Admin commands, punishment hooks, imports and script actions are high-impact abuse paths. | Least privilege, confirmation tokens, rate limits, immutable audit, safe dry run, sandbox and dual-control option for destructive bulk operations. |
| RC-046 | Security decision — **RESOLVED 2026-07-14** | Proxy/Redis/plugin messages can be forged/replayed. | `ZBW-READY-013`, ADR-0011 and `NETWORK_SECURITY.md` fix TLS/auth, HMAC/mTLS integrity, key rotation, version/ID/time/nonce/dedupe/limit/secrets controls and adversarial tests. |
| RC-047 | Fact | Diagnostics/export requirements can leak credentials or private evidence. | Allowlist-based redaction, synthetic tests seeded with secrets/PII, separate staff permissions and encrypted export where needed. |
| RC-048 | Fact | Third-party plugins execute in the same JVM and can bypass conceptual module boundaries. | Document trust boundary; minimize exposed mutable objects/secrets, isolate credentials, sign external messages and offer process-separated services for high assurance. |

## E. Reliability, data and migration risks

| ID | Classification | Risk | Mitigation / missing decision |
|---|---|---|---|
| RC-049 | Fact | Exactly-once distributed delivery is unrealistic; network and process failures yield at-least-once effects. | Use durable outbox/inbox, idempotency keys, uniqueness constraints and compensations to provide exactly-once business outcome. |
| RC-050 | Technical consistency decision — **RESOLVED 2026-07-14** | SQLite cannot safely be a shared multi-backend writer. | `ZBW-READY-014` and ADR-0011 restrict SQLite to one JVM, require MySQL/MariaDB for proxy scale and define SQL authority, epoch leases, outbox/inbox/idempotency and partition behavior. |
| RC-051 | Fact | Read/write separation can expose stale player balances, permissions or eligibility. | Route consistency-sensitive reads to authoritative writer/session; replicas only for declared stale-safe views. |
| RC-052 | Fact | Cross-server clocks and event ordering can disagree. | Sequence per aggregate, DB/Redis stream ordering, monotonic local clocks for durations and timestamps only as metadata; configure clock monitoring. |
| RC-053 | Fact | Importing IDs/data from several installations can collide or duplicate players/maps. | Installation namespace, collision-resistant IDs, dry-run identity map, deterministic duplicate policy and reversible audit. |
| RC-054 | Fact | Provider switching for worlds/NPCs/parties may be lossy. | Capability comparison, export intermediate schema, dry-run, backup and explicit unsupported-field report before cutover. |
| RC-055 | Fact | Replay codec/schema evolution may strand legal evidence. | Reader supports declared old formats, immutable migration copy, checksum preservation and compatibility fixtures. |
| RC-056 | Fact | Emergency replay disable conflicts with evidence reliability. | Reserved evidence queues/storage; only non-evidence adaptive reduction by default; explicit alarm and audit for any evidence loss. |

## F. Missing product and acceptance decisions

| ID | Classification | Missing decision | Recommended owner choice |
|---|---|---|---|
| RC-057 | Assumption | Reference hardware/workloads and scale for “hundreds of thousands of players” are absent. | Treat that phrase as ecosystem total; benchmark per node/network with explicit concurrency and horizontal scaling targets. |
| RC-058 | Assumption | Matchmaking/ranked algorithm, queue fairness and party skill policy are not specified. | Provider SPI plus initial FIFO/capacity policy; add ranked algorithm only through a new approved requirement. |
| RC-059 | Owner product decision — **RESOLVED 2026-07-14** | Core numeric balance and mode semantics lacked defaults. | `ZBW-READY-015`, ADR-0014 and `BALANCING_BASELINE.md` provide original versioned prices, generators, modes and rewards with validation, golden simulation, telemetry and controlled replacement. |
| RC-060 | Fact | Public API compatibility window described only as “reasonable.” | Define at least one major-version deprecation window or an owner-chosen time/release policy in ADR. |
| RC-061 | Owner operational decision — **RESOLVED 2026-07-14** | RPO/RTO, quotas, encryption and restore cadence were unspecified. | `ZBW-READY-016`, ADR-0015 and `OPERATIONAL_DEFAULTS.md` fix per-class targets, backup retention, evidence reserve, degradation and monthly/quarterly/annual drills. |
| RC-062 | Owner quality decision — **RESOLVED 2026-07-14** | Coverage/static/vulnerability thresholds were absent. | `ZBW-READY-017`, ADR-0009 and `QUALITY_GATES.md` fix module coverage, critical-path/mutation, static/API/architecture and zero unexcepted critical/high vulnerability gates. |
| RC-063 | Fact | Locale set and translation ownership are unspecified. | Ship one complete source locale plus validated fallback/import workflow; add supported locales only with completeness gate. |
| RC-064 | Fact | Accessibility success measures and Bedrock control equivalence are unspecified. | Define representative user tasks and capability matrix; require a non-color-only cue and alternate input for every critical action. |
| RC-065 | Owner privacy decision — **RESOLVED 2026-07-14** | Profile/replay/leaderboard/Atlas visibility defaults were inconsistent. | `ZBW-READY-018` and ADR-0010 set owner-private profiles, participant/staff replays, aggregate public leaderboards and anonymized/restricted Atlas with revocable audited overrides. |
| RC-066 | Owner licensing decision — **RESOLVED 2026-07-14** | Project, premium, API and addon licensing model was absent. | `ZBW-READY-019`, ADR-0016 and `PROJECT_LICENSING_RECOMMENDATION.md` select proprietary all-rights-reserved product/assets and separately executed public SDK terms (Apache-2.0 recommended). |

## G. Atomic coverage findings

| ID | Classification | Finding / risk | Preserving resolution or recommendation |
|---|---|---|---|
| RC-067 | Fact — **RESOLVED 2026-07-14** | `MASTER_PROMPT.md` did not contain catalogues explicitly labelled premium/free BedWars1058 addons, so the first audit could not lawfully infer one. The owner subsequently supplied an authoritative inventory of 8 premium and 41 free addon references. | `docs/ADDON_FEATURE_CATALOG.md` preserves all 49 references as the original 463 independent requirements plus the append-only `ZBW-ADDON-464..473` Resource Scarcity children, for 473 fully mapped rows. All three validators require 49/49 references, the 8/41 split, 473/473 `COVERED` rows, the accepted decision supplement, no partial/missing item and 100% combined coverage. RC-067 remains closed without reclassifying the original Master Prompt text. |
| RC-068 | Fact | The original source-to-PRD table mapped broad line ranges to parent IDs, so a detailed child could be lost while its parent still appeared covered. | ZBW-GOV-011 makes every non-empty source assertion a normative `MP-L####` child row; ZBW-QA-007 rejects missing, partial, unmapped, unknown-ID or less-than-100% coverage before Java work. This remediation preserves every child rather than splitting or merging features for convenience. |
| RC-069 | Fact | Line-derived `MP-L####` IDs remain stable only for the content-addressed source baseline; inserting or deleting source lines can shift later IDs. | Record SHA-256 and total line count, block on drift, regenerate a reviewed old-to-new coverage diff, and keep stable `ZBW-*` implementation requirements as the long-lived semantic IDs. Never silently reuse an old atomic ID for changed text. |
| RC-070 | Recommendation | A 6,000-plus-row verbatim matrix is complete but difficult to review manually and can conceal classification mistakes despite perfect mechanical coverage. | Keep the concise category summary and exception notes at the top, validate category tags deterministically, require owner review of ambiguities/conflicts, and use the atomic rows as lossless evidence rather than as the only product-design view. |
| RC-071 | Legal/provenance decision — **RESOLVED 2026-07-14** | Addon pages/names may contain protected material or restrictive terms. | `ZBW-READY-020` and ADR-0013 preserve all 49/473 mappings while requiring neutral clean-room implementation, original assets/text/balance, provenance/SBOM/notices and release legal review. |
| RC-072 | Fact — **RESOLVED 2026-07-14** | The public Private Games page advertised eleven modifiers but publicly enumerated ten, requiring an original owner-selected eleventh capability. | RESOURCE SCARCITY is the original eleventh modifier. `ZBW-ADDON-464..473` separately require iron, gold, diamond, emerald and custom-resource multipliers, five presets, host GUI, permission, API/events/placeholders and native/custom-generator correctness. Values are defined in `docs/ORIGINAL_STARTER_CATALOG.md`; ADR-0001 accepted. |
| RC-073 | Owner content decision — **RESOLVED 2026-07-14** | Original starter content, replacement/extension rules and provenance fields were missing. | `ZBW-CONTENT-001..011`, `docs/ORIGINAL_STARTER_CATALOG.md`, `docs/ASSET_PROVENANCE.md` and ADR-0002 establish original shop/mode/quest/achievement/pass/cosmetic/private/sound/effect content, versioned config/API expansion and asset provenance. Production assets remain blocked until approved; this is execution evidence, not an unresolved design choice. |
| RC-074 | Technical decision — **RESOLVED 2026-07-14** | DiscordStats, the Discord/Corebot-style adapter and DiscordUtils required a provider topology and failure boundary. | `ZBW-DISCORD-001..008`, `docs/DISCORD_ARCHITECTURE.md` and ADR-0003 define an optional secure gateway/event stream, disabled provider, embedded webhook, external bot and custom provider API. Discord failures cannot affect gameplay and bot tokens stay outside normal config/Minecraft process. |
| RC-075 | Compatibility decision — **RESOLVED 2026-07-14** | Minecraft 1.8 lacks modern boss bars, materials, packet metadata and visual/audio capabilities required by addon-equivalent features. | `ZBW-COMPAT-001..009`, `docs/COMPATIBILITY_FALLBACKS.md` and ADR-0004 make 1.8 a server-runtime target, isolate adapters and define material/item/text/particle/sound/entity/packet/GUI fallbacks. Only pure decoration may be suppressed; gameplay remains mandatory. |
| RC-076 | Licensing decision — **RESOLVED 2026-07-14** | Dependency bundling, shading, modification, attribution and commercial redistribution policy was unspecified. | `ZBW-LICENSE-001..007`, the updated exact selection/audit, `THIRD_PARTY_NOTICES.md` and ADR-0005 establish pre-resolution checksum/licence approval and default-deny redistribution; RC-021/024/027 are now also resolved by ADR-0007. |

## Pre-code decision gate result

RC-003/004/017/018/021/022/024/027/029/040/041/043/046/050/059/061/062/065/066/071 and RC-072 through RC-076 are resolved by concrete owner/technical decisions, stable requirements, accepted ADRs and normative specifications. External artifact hash/licence acquisition and jurisdiction-specific public-release legal execution remain deterministic build/release evidence, not unresolved architecture. Other non-pre-code risks remain scheduled at their dependency milestones and do not authorize scope reduction. See `PRE_CODE_READINESS_REPORT.md`.

## H. M04 implementation risks and external gates

| ID | Classification | Finding / risk | Preserving resolution or recommendation |
|---|---|---|---|
| RC-077 | Fact — RESOLVED 2026-07-15 | PR #5 run `29406777872` executed independent required suites against digest-pinned MySQL 8.4.0 and MariaDB 11.4.2. Each job passed 91 reactor tests including all 12 mandatory external contracts with zero failures/errors/skips, seven certified plans, sanitized Hikari evidence and encrypted backup/restore evidence. | Keep `.github/workflows/m04-external-database-contracts.yml`, `build/m04-database-container-lock.json` and the zero-skip certifier mandatory for relevant changes. The certified artifacts `m04-mysql-contract-evidence` and `m04-mariadb-contract-evidence` resolve the M04 external gate without reducing later production PT/chaos/drill scope. |
| RC-078 | Verified technical constraint — RESOLVED BY ADR-0019 | Flyway Core 10.20.1 is not link-safe on the mandatory Java 8 runtime, while replacing the approved version or dropping Flyway would violate READY-008. MySQL/MariaDB Flyway vendor modules are not yet approved dependencies. | Keep Java-8-linkable storage artifacts, execute Flyway reflectively on compatible runtimes, and retain the checksum-equivalent built-in migration runner on Java 8/all SQL engines. Add vendor modules only after exact audit; no migration functionality is removed. |

## I. M06/M22 allocation reconciliation

| ID | Classification | Finding / risk | Preserving resolution |
|---|---|---|---|
| RC-079 | Fact — **RESOLVED 2026-07-15** | `zbw-paper-modern` was first allocated to M06 while its direct dependency `zbw-compat-v1_20-v1_21` was first allocated to M22. Traceability also described `ZBW-COMPAT-002` as a `zbw-compat-v1_8` adapter delivered in M06 even though the graph and accepted compatibility policy reserve legacy adapters and complete certification for M22. | Move the modern adapter to M06, add Java-8-neutral `zbw-world` to M06 and make `zbw-paper-modern` depend on both. M06 owns only neutral contracts, primary mappings and pending Paper 1.21.1 foundation certification; `zbw-compat-v1_8`, all other deferred adapters and complete 1.8–1.21.x release certification remain M22. Deterministic graph-order, traceability, architecture, compatibility-matrix and 100%-coverage validators pass; any future failure reopens this conflict. No requirement or compatibility guarantee is reduced. |
