# Milestone 4 implementation evidence

## Scope

M04 implements the storage, migration, outbox/inbox and bounded-cache foundations allocated by `docs/MILESTONES.md`. Direct Requirement IDs are `ZBW-DEPLOY-007`, `ZBW-DEPLOY-008` and `ZBW-ARC-008`. It supplies only the durable-data foundations of `ZBW-PROG-001..014`, `ZBW-STATS-001..008`, `ZBW-REPLAY-001..010`, `ZBW-ATLAS-001..013`, `ZBW-READY-011`, `ZBW-READY-014` and `ZBW-READY-016`.

No scheduler, Minecraft adapter, proxy/Redis transport, feature repository, gameplay, progression behavior, statistics aggregation, replay recorder, Atlas workflow or M05 module is present. The generic storage foundation preserves those later owners.

## Modules and boundaries

- `zbw-storage-api` is Java-8 bytecode and depends only on `zbw-api` and `zbw-domain`. It contains no JDBC, Hikari, Flyway, cache implementation, filesystem, runtime configuration or platform dependency.
- `zbw-storage-sql` is Java-8 bytecode and is the only production module permitted to import JDBC or contain SQL. It provides Hikari-backed SQLite, MySQL and MariaDB adapters, the deterministic migration runner, a Java-8-linkable Flyway provider, Caffeine cache and recovery coordination.
- Both modules are synchronous/blocking by contract. Every call belongs on a bounded storage worker supplied by M05; owner/tick-thread use is forbidden. M04 does not create threads or executors.

The thin module JARs do not contain HikariCP, Caffeine, Flyway, database drivers or test libraries. The exact union of the JDK-specific Maven graphs contains 192 binary components and 604 JAR/POM files, each checksum/licence locked; product bundling is disabled for every row. The lock includes both the project JUnit Platform 1.11.4 graph and Surefire 3.5.4's isolated 1.12.1 provider graph so a clean CI cache cannot select an unrecorded artifact.

## Delivered behavior

- Immutable typed record keys, revisions, serialization payloads, transaction options, recovery objectives, migration plans and message envelopes.
- Thread-confined units of work with explicit commit, rollback and close-as-rollback semantics.
- Prepared-statement CRUD with optimistic concurrency and atomic business mutation/outbox or inbox operation in the caller's transaction.
- Uniqueness-based outbox/inbox idempotency, causal/event version metadata, deterministic ordering, bounded claim batches and expiring claims.
- SQLite single-connection serialized-writer enforcement and rejection of SQLite in scalable-proxy topology.
- MySQL/MariaDB bounded Hikari pools, read-only transaction hints, query timeouts and bounded deadlock retry classification.
- Versioned ordered schema history, SHA-256 checksum-drift rejection, restart-safe DDL and an actual Flyway classpath migration contract. External-engine unsafe DDL is marked backup-required.
- Bounded Caffeine cache with per-entry expiry, minimum-revision fencing, targeted/global invalidation and SQL authority.
- Retention classes, legal hold/release authorization records and duplicate-safe tombstones without evidence payload exposure.
- Backup/restore coordinator accepting only encrypted, independently validated provider evidence and enforcing zero committed-state RPO/15-minute SQL RTO declarations.
- Sanitized pool health counters; no URL, username, password, SQL error message, player ID or case ID appears in health output.

## Test and validation evidence

The local reactor executes 79 deterministic tests with zero failures, errors or skips. The external contract class is deliberately not selected outside required external mode, so absence of Docker is never reported as a successful skipped test. In the RC-077 workflow each database job selects exactly one approved image and must execute all 12 mandatory external contracts with zero failures, errors or skips. The local SQLite tests cover migration/restart, crash rollback, CRUD, optimistic conflicts, transactions, outbox/inbox idempotency and ordering, retention/hold/tombstone semantics, cache expiry/versioning, recovery evidence, retry bounds and thread confinement.

The 12 external contracts cover: exact server/image identity; repository prepared statements and unique constraints; migrations/checksum tamper rejection and repair; transaction rollback; concurrent writers; bounded deadlock retry; query timeout; outbox/inbox crash, reclaim and duplicate recovery; encrypted backup and validated restore; retention/legal-hold/tombstone persistence across engine restart; seven certified JSON query plans at non-trivial cardinality; and sanitized HikariCP pool-health evidence. Required mode fails immediately if the chosen suite, Docker runtime, provenance verification or evidence set is absent.

Local quality evidence:

| Module | Line coverage | Branch coverage | Required | Checkstyle | SpotBugs |
|---|---:|---:|---|---|---|
| `zbw-storage-api` | 90.23% | 87.50% | 90% / 85% | 0 violations | 0 findings |
| `zbw-storage-sql` | 91.34% | 72.19% | 80% / 70% | 0 violations | 0 findings |

The clean offline reactor passed on every approved compile JDK. Each row compiled all production classes to Java 8 bytecode and executed the same local 79-test suite:

| Compile JDK | Reactor | Local tests | Skipped tests |
|---:|---|---:|---:|
| 8 | PASS | 79 passed | 0 |
| 11 | PASS | 79 passed | 0 |
| 16 | PASS | 79 passed | 0 |
| 17 | PASS | 79 passed | 0 |
| 21 | PASS | 79 passed | 0 |

The first JDK 8 run exposed HikariCP's JDK-activated `slf4j-api:1.7.30` branch. The controlled acquisition process added its exact JAR, checksum and MIT licence evidence to the cross-JDK lock before the successful offline matrix was rerun.

The M04 deterministic entry point is `tools/validation/run_m04_validation.py`. It preserves M02/M03 binary signatures, verifies Java-8 class files, generates strict JavaDoc, confines SQL, validates exact dependencies/licences/SBOM, and runs every governance and 100%-coverage documentation gate.

## External exit gate

`.github/workflows/m04-external-database-contracts.yml` defines independent required MySQL `8.4.0` and MariaDB `11.4.2` jobs. Before pulling a container it validates the dependency/licence locks and immutable image provenance; after execution `tools/ci/certify_m04_external.py` rejects a missing suite, any skipped/failed/error test, an incomplete contract list, unsafe query plan, incomplete pool/backup evidence or possible credential exposure. Test reports and certified JSON evidence are uploaded under database-specific artifact names.

RC-077 is resolved and M04 is VERIFIED by PR #5 workflow run [29406777872](https://github.com/Arciale16/ZartraBedwars/actions/runs/29406777872) at source commit `eba88fe1b5e6e638a5a47f1c05a5d7c10e6c5344`. MySQL and MariaDB each executed 91 reactor tests, including all 12 mandatory external contracts, with zero failures, errors or skips. Both jobs passed Checkstyle, SpotBugs, JaCoCo, dependency/licence/provenance validation, the 215-class binary/API gate, 21 governance tests and deterministic documentation validators.

The uploaded `m04-mysql-contract-evidence` and `m04-mariadb-contract-evidence` artifacts each contain Surefire XML, JaCoCo reports, identity, certification, seven raw JSON query plans, pool health, and encrypted backup/restore evidence. Certified server versions are MySQL `8.4.0` and MariaDB `11.4.2-MariaDB-ubu2404`. This verification completes only M04; M05 was not started.
