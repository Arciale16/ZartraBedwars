# Milestone 7 implementation evidence

## Scope

M07 implements the core/application allocations of `ZBW-ARENA-001..009` and
`ZBW-ADDON-408..423` in one Java-8 module, `zbw-arena`. It does not implement
gameplay, commands, GUIs, editor rendering, confirmation tokens,
PlaceholderAPI, optional providers or legacy compatibility.

The module depends at production scope only on `zbw-api`, `zbw-domain`,
`zbw-application` and `zbw-world`. M04 SQL storage and the M06 Paper bootstrap
are used only by contract/certification tests and are absent from the arena
artifact.

## Delivered behavior

- Immutable typed arena/map aggregates preserve IDs across rename, validation,
  backup and restore. Deep duplication allocates new arena/map IDs while
  retaining explicit semantic references and independently owned collections.
- Authorized CRUD, list, rename, duplicate, enable, disable, validation,
  deletion and last-known-good recovery use optimistic revisions, typed
  failures, cancellable pre-events, immutable post-events and audit facts.
- Setup sessions isolate drafts, enforce actor/target/revision scope, expose all
  ArenaSetup mutations, bound history to 128 snapshots, support undo/redo,
  marker proposals, validation, base-bound preview/apply and atomic commit.
- `SetupPreview` binds both candidate and unchanged base fingerprints. A
  candidate cannot be replayed after the session draft changes.
- Validation reports cover missing worlds/spawns/beds/generators/NPCs,
  capacity, broken team references, unsafe locations, out-of-bounds regions
  and location collisions. Any error blocks enable and known-good promotion.
- Canonical schema-one archives are bounded to 4 MiB, SHA-256 checked,
  deterministic and decoded without Java object deserialization. Import,
  export, durable backup and exact-revision restore preserve identities.
- Arena world reset, recovery and duplication delegate to M06 asynchronous
  world handles. M05/M06 enforce bounded admission, cancellation, timeout,
  owner-thread mutation, off-owner filesystem work, compensation and
  same-world concurrency exclusion.
- Health and diagnostic projections retain only bounded public counts.

## Storage and migration

No database schema or production M04 repository changed. Arena, map, setup,
revision and last-known-good persistence are represented by typed M07 ports;
the SQLite contract suite binds those ports to the existing M04 transactional
record store and verifies restart persistence, optimistic revision, atomic
arena/setup publication and rollback. Existing prepared-statement, uniqueness,
transaction and recovery behavior therefore remains owned and certified by
M04. MySQL/MariaDB external regressions are not triggered because M07 changes
neither SQL, migration checksums nor repository implementation.

## Verification

- 37 M07 JUnit tests pass with zero failures, errors or skips, including three
  real SQLite contracts.
- The exact Paper 1.21.1 build 133 test-only plugin validates an arena, setup
  mutation/undo/redo and archive round trip, then completes LOAD, UNLOAD,
  CLONE, RESET and UNLOAD through the M06 runtime. The final snapshot is
  leak-free and evidence writing occurs off the owner thread.
- Java 8/11/16/17 neutral reactors each pass 158 tests; the Java-21 full
  reactor passes 175 tests. Class-major 52 and additive M06 API preservation
  are recorded in `build/api-signature-baseline-m07.txt`.
- M07 JaCoCo reports 95.78% line and 85.09% branch coverage against mandatory
  90%/85% gates. Checkstyle and SpotBugs report zero findings.
- Dependency, licence, provenance, SBOM, notices, governance, catalogue,
  traceability and documentation checks pass.

Exact reports are generated under each module `target` directory. Runtime
evidence is `build/evidence/m07-paper-primary.json`.

## Generated artifacts

- `arena/zbw-arena/target/zbw-arena-0.1.0-SNAPSHOT.jar`
- `build/api-signature-baseline-m07.txt`
- `target/zartrabedwars-m07-neutral-javadoc.zip`
- `target/zartrabedwars-m07-modern-javadoc.zip`
- Surefire, JaCoCo, Checkstyle and SpotBugs reports
- `build/evidence/m07-paper-primary.json`

The Paper certification plugin is assembled under `target/m07-paper-e2e` from
test classes and is never included in a release artifact.
