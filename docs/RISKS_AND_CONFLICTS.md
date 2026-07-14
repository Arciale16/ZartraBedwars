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
| RC-003 | Fact | Primary Paper 1.21.1/Java 21 conflicts with server-runtime compatibility down to 1.8, whose JVM/API baselines cannot safely share a Java 21-only JAR. | Separate runtime artifacts/toolchains and narrow compatibility adapters; preserve common domain/protocol semantics. | Blocking ADR |
| RC-004 | Ambiguity | “Minecraft 1.8 through latest” could mean server versions, client versions via Via, or both. | Assume both: server adapter matrix plus client capability matrix. | Owner decision |
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
| RC-017 | Fact | 300 built-in cosmetics plus “original sample cosmetics” is unclear about content/asset depth. | Require 300 original functional definitions; decide how many require unique licensed assets/effects and create content QA. | Blocking content/legal ADR |
| RC-018 | Ambiguity | “Custom logic” and “script hooks” may imply arbitrary code execution. | Provide allowlisted declarative actions or a sandbox with resource/time limits; no arbitrary shell/JVM access from config. | Blocking security ADR |
| RC-019 | Fact | PDC does not exist on legacy 1.8; RGB/MiniMessage and modern materials/sounds/packets are not representable on all clients. | Compatibility SPI maps PDC↔legacy NBT/data and deterministic visual/text fallbacks; gameplay semantics remain. | Compatibility ADR |
| RC-020 | Fact | Minecraft inventory GUIs cannot provide general keyboard shortcuts on all Java/Bedrock clients. | Implement protocol-supported number/shift/hotbar actions and Bedrock/chat/command alternatives; document exact capability matrix. | Compatibility ADR |

## B. Missing dependency and compatibility decisions

| ID | Classification | Missing information / risk | Recommendation |
|---|---|---|---|
| RC-021 | Fact | No Maven group/artifact/version, repository, license or supported-version range is pinned for Paper, proxies, CloudNet, PlaceholderAPI, Vault, LuckPerms, ProtocolLib, WorldEdit/FAWE/WorldGuard, SlimeWorldManager, Multiverse, Citizens, ZNPCs Plus, DecentHolograms, AlessioDP Parties, Grim, Vulcan, Via or Geyser/Floodgate. | M01 produces an owner-reviewed dependency/license/version matrix and BOM; no “latest” ranges. |
| RC-022 | Fact | No test-server distribution is identified for every legacy version and Paper did not have identical support semantics across the whole 1.8–1.21 range. | Decide exact runtime distribution per version family and whether some rows use Spigot-compatible APIs behind the Paper adapter contract. |
| RC-023 | Fact | SlimeWorldManager/provider naming/version/API availability may differ by server line. | Select maintained provider coordinates and implement SPI aliases/migration; claim only tested versions. |
| RC-024 | Fact | Grim/Vulcan public API/event contracts and licensing/access may vary. | Obtain documented API access/licenses; use reflection only in isolated, tested optional adapters if legally permitted. |
| RC-025 | Fact | Object storage is required only as an adapter API; no concrete S3-compatible provider, credentials model or consistency behavior is chosen. | Define SPI now; decide first concrete provider, multipart/checksum/encryption semantics before replay milestone. |
| RC-026 | Fact | External REST/Discord integration technology, bind address, TLS termination and authentication provider are unspecified. | ADR for embedded vs sidecar API, OAuth/API-key scopes, reverse proxy and default-disabled behavior. |
| RC-027 | Fact | No dependency-injection, command, GUI, migration, serialization, metrics, test-container or benchmark libraries are selected. | M01 evaluates choices against Java/version lines; core contracts must not depend on a replaceable framework. |
| RC-028 | Fact | Native party requirements overlap AlessioDP provider semantics; conflict/source-of-truth/migration behavior is unspecified. | Party provider ADR defines ownership, live switch restrictions, ID mapping and rollback. |

## C. Performance and scalability risks

| ID | Classification | Risk | Mitigation / missing acceptance |
|---|---|---|---|
| RC-029 | Fact | “40+ worlds,” “100+ arenas,” “many players,” “large DB” and “not significantly affect TPS” lack workload/hardware numbers. | Provisional PRD budgets apply; M01 freezes CPU/RAM/storage/JDK, player/arena/event counts, warmup and percentile methodology. |
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
| RC-040 | Legal concern | Replays may contain chat, UUIDs, skin references, party associations, behavior telemetry and moderation evidence. Jurisdiction, lawful basis, age policy and retention are unspecified. | Privacy/legal review before recording release; chat off by default; purpose-limited access; export/delete/hold policy and transparency. |
| RC-041 | Fact/legal | GDPR-style deletion conflicts with legal hold/open moderation evidence. | Delete ordinary personal data; pseudonymize retained evidence and restrict access when a lawful hold overrides deletion. Owner/legal approval required. |
| RC-042 | Legal concern | Recording VPN/shared-environment indicators may be invasive or unlawful in some jurisdictions. | Default disabled; data-protection assessment, minimization and jurisdiction-specific opt-in policy. |
| RC-043 | Legal concern | Inspiration from large networks, “Atlas” terminology, addon reference names and BedWars1058 migration raise copyright/trademark/API-license risks. | Enforce the clean-room/originality contract in `docs/ADDON_FEATURE_CATALOG.md`: original code/assets/text/effects/layouts/balance, neutral runtime branding, provenance manifest, third-party notices and documented lawful migration inputs; obtain legal review before commercial release. |
| RC-044 | Fact | External store premium access hooks can create payment/chargeback/entitlement obligations. | Adapter only; signed/idempotent entitlement events, revoke/audit and no card data handling. |
| RC-045 | Fact | Admin commands, punishment hooks, imports and script actions are high-impact abuse paths. | Least privilege, confirmation tokens, rate limits, immutable audit, safe dry run, sandbox and dual-control option for destructive bulk operations. |
| RC-046 | Fact | Proxy/Redis/plugin messages are untrusted network inputs and can replay/forge transfers or rewards. | TLS/auth where supported, signatures, nonce/operation ID, expiry, schema/size validation, allowlists and key rotation. |
| RC-047 | Fact | Diagnostics/export requirements can leak credentials or private evidence. | Allowlist-based redaction, synthetic tests seeded with secrets/PII, separate staff permissions and encrypted export where needed. |
| RC-048 | Fact | Third-party plugins execute in the same JVM and can bypass conceptual module boundaries. | Document trust boundary; minimize exposed mutable objects/secrets, isolate credentials, sign external messages and offer process-separated services for high assurance. |

## E. Reliability, data and migration risks

| ID | Classification | Risk | Mitigation / missing decision |
|---|---|---|---|
| RC-049 | Fact | Exactly-once distributed delivery is unrealistic; network and process failures yield at-least-once effects. | Use durable outbox/inbox, idempotency keys, uniqueness constraints and compensations to provide exactly-once business outcome. |
| RC-050 | Fact | SQLite cannot be a multi-backend shared writer and has different concurrency characteristics from MySQL/MariaDB. | Restrict SQLite to shared/single-process deployment; scalable mode validation requires a network DB. |
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
| RC-059 | Fact | Core BedWars numerical balance, map rules, item prices/cooldowns and mode semantics are not defined. | Create versioned original balance specifications before each gameplay milestone; configs need validated defaults and golden tests. |
| RC-060 | Fact | Public API compatibility window described only as “reasonable.” | Define at least one major-version deprecation window or an owner-chosen time/release policy in ADR. |
| RC-061 | Fact | Backup RPO/RTO, retention durations, quota defaults, encryption keys and restore drills are unspecified. | Operational ADR per data class with measurable RPO/RTO and scheduled restore verification. |
| RC-062 | Fact | Coverage thresholds, static-analysis warning policy and vulnerability severity/exception process are absent. | M01 quality ADR sets module-aware coverage, zero-new-critical/high vulnerability gate and time-bounded exceptions. |
| RC-063 | Fact | Locale set and translation ownership are unspecified. | Ship one complete source locale plus validated fallback/import workflow; add supported locales only with completeness gate. |
| RC-064 | Fact | Accessibility success measures and Bedrock control equivalence are unspecified. | Define representative user tasks and capability matrix; require a non-color-only cue and alternate input for every critical action. |
| RC-065 | Fact | Default privacy visibility for profiles/replays/leaderboards/Atlas history is not consistently stated. | Privacy-by-default ADR; moderation evidence remains staff-accessible under purpose/permission/audit. |
| RC-066 | Fact | Release licensing model (open/premium/enterprise modules) and API redistribution terms are absent. | Choose licenses before publishing artifacts/marketplace SDK; align dependency licenses and notices. |

## G. Atomic coverage findings

| ID | Classification | Finding / risk | Preserving resolution or recommendation |
|---|---|---|---|
| RC-067 | Fact — **RESOLVED 2026-07-14** | `MASTER_PROMPT.md` did not contain catalogues explicitly labelled premium/free BedWars1058 addons, so the first audit could not lawfully infer one. The owner subsequently supplied an authoritative inventory of 8 premium and 41 free addon references. | `docs/ADDON_FEATURE_CATALOG.md` now preserves all 49 references as 463 independent `ZBW-ADDON-001..463` requirements with full surface, test, module and milestone mappings. Both verifiers require 49/49 references, the 8/41 split, 463/463 `COVERED` rows, no partial/missing item and 100% combined coverage. RC-067 is closed without reclassifying the original Master Prompt text. |
| RC-068 | Fact | The original source-to-PRD table mapped broad line ranges to parent IDs, so a detailed child could be lost while its parent still appeared covered. | ZBW-GOV-011 makes every non-empty source assertion a normative `MP-L####` child row; ZBW-QA-007 rejects missing, partial, unmapped, unknown-ID or less-than-100% coverage before Java work. This remediation preserves every child rather than splitting or merging features for convenience. |
| RC-069 | Fact | Line-derived `MP-L####` IDs remain stable only for the content-addressed source baseline; inserting or deleting source lines can shift later IDs. | Record SHA-256 and total line count, block on drift, regenerate a reviewed old-to-new coverage diff, and keep stable `ZBW-*` implementation requirements as the long-lived semantic IDs. Never silently reuse an old atomic ID for changed text. |
| RC-070 | Recommendation | A 6,000-plus-row verbatim matrix is complete but difficult to review manually and can conceal classification mistakes despite perfect mechanical coverage. | Keep the concise category summary and exception notes at the top, validate category tags deterministically, require owner review of ambiguities/conflicts, and use the atomic rows as lossless evidence rather than as the only product-design view. |
| RC-071 | Legal concern | Public addon/product pages and their names describe behavior but may include protected trademarks, copyrighted text/layout/assets, proprietary balance/content or terms that do not permit redistribution. | Use pages only as functional references. Implement clean-room native equivalents, choose neutral runtime names, create all code/content/assets independently, retain provenance/SBOM/notices and complete legal review. No mapped capability is removed. |
| RC-072 | Fact | The public Private Games page advertises eleven modifiers but publicly enumerates ten named modifiers. The missing eleventh behavior cannot be recovered safely by guessing or copying private material. | Ten named modifiers are separate requirements and an additional validated custom-modifier registry preserves the broader functional scope. Owner must select the eleventh original built-in default before M20 content freeze. |
| RC-073 | Missing content decision | Exact original names, visuals, sounds, models, messages and balance values are not specified for 300+ cosmetics, five Armed weapons, Lucky Block outcomes, rotating items, special mobs and mode abilities. | M01/M11/M14 create versioned original content/balance specifications, provenance records and golden tests. Counts, categories and mechanics remain mandatory while content values are chosen. |
| RC-074 | Technical decision | DiscordStats, the Discord/Corebot-style adapter and DiscordUtils overlap but the deployment topology—one shared bot with adapters or separate processes—is unspecified. | Define one provider-neutral Discord contract with identity linking, privacy, rate, cache and secret controls; choose topology by ADR before M16 and test feature equivalence. Do not merge away any of the three explicit feature sets. |
| RC-075 | Compatibility decision | Minecraft 1.8 lacks modern boss bars, materials, packet metadata and some visual/audio capabilities required by addon-equivalent features. Exact approved visual fallbacks are not selected. | M06/M22 capability matrix and fixtures choose deterministic original fallbacks per version; gameplay semantics and access remain, with visible documented degradation rather than silent disablement. |
| RC-076 | Licensing decision | The source catalogue's premium/free labels do not define ZartraBedWars licensing, and optional CloudNet/DiscordSRV/PlaceholderAPI/proxy/library terms may constrain bundling or redistribution. | Treat premium/free solely as source classification. RC-021/RC-066 dependency and product licensing ADRs must approve coordinates, licences, optional-download strategy, notices and redistribution before release. |

## Required pre-code decision gate

No Java implementation should begin until at least RC-003/004, RC-017/018, RC-021/022/024/027, RC-029, RC-040/041/043, RC-046, RC-050, RC-059/061/062/065/066 and RC-071/073/075/076 have an owner-approved ADR or policy. RC-072 must close before M20, and RC-074 before M16. Other items may be resolved immediately before their dependent milestone but remain visible in traceability.
