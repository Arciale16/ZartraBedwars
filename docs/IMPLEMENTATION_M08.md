# Milestone 8 implementation evidence

## Scope

M08 implements the core/application and closed primary Paper 1.21.1 allocations of
`ZBW-GAME-001..003`, `ZBW-GAME-006`, `ZBW-GAME-008`, `ZBW-GAME-010`,
`ZBW-ADDON-001..009`, `108..114`, `124..130`, `148..154`, `334..340`,
`398..407`, `424..437`, and the shared-server foundation of `ZBW-DEPLOY-001`.

The Java-8 `zbw-game` module contains all game, session, team, lobby and allocated
addon rules. The Java-21 `zbw-paper-modern` module contains only closed Paper event
translation and effects. No M09 command/UI module, M10 mode/selector implementation,
PlaceholderAPI, proxy delivery, optional provider or legacy adapter is delivered.

## Delivered behavior

- A deterministic revision-checked aggregate owns waiting, countdown, playing,
  completion, restoration, reset, team assignment, bed state, elimination,
  reconnect and phase schedule behavior.
- Immutable player snapshots preserve inventory, location, game mode and visibility.
  Completion is fenced by idempotency key and committed through an atomic persistence
  port before restoration/reset.
- `GameEngineService` serializes bounded per-match work, rejects overlapping writes,
  restores the prior in-memory snapshot on failed persistence and isolates event sinks.
- Typed addon policies implement hotbar precedence and last-known-good publication,
  atomic deposit transfer, start-message dedupe, AntiDrop capture/recovery, delayed
  leave and privacy-safe lobby/tab/boss-bar projections.
- Paper adapters validate runtime values, mutate only on the primary thread and clean
  owned inventory, scoreboard, tab-list, listener and boss-bar state.
- M03 configuration and localization services can publish the validated immutable rule
  objects. Reload is atomic at policy-registry boundaries; no runtime policy reads raw
  files, untrusted maps or remote providers on the owner thread.

## Verification

- 41 M08 JUnit tests pass with zero failures, errors or skips on every neutral matrix
  runtime. The neutral reactor passes 199 tests on each of Java 8/11/16/17; the full
  Java-21 reactor passes 216 tests. They cover
  transition legality, optimistic revisions, concurrency, rollback, crash recovery,
  idempotency, inventory conservation, duplicate capture, authorization, validation,
  privacy, cleanup and malformed/null inputs.
- M08 JaCoCo coverage is 95.89% line (1,283/1,338) and 88.26% branch (782/886),
  exceeding the mandatory 90%/85% gates. Checkstyle and SpotBugs report no findings.
- The Paper adapter retains its 80%/70% JaCoCo gate for test-JVM-safe classes. Direct
  Bukkit server-bound bridges are explicitly excluded from in-process instrumentation
  and are instead mandatory in the checksum-locked Paper E2E; no class is counted as
  verified merely because it compiles.
  The measured test-JVM-safe Paper code reaches 89.89% line (169/188) and 87.34%
  branch coverage (69/79).
- The exact checksum-locked Paper 1.21.1 build 133 test-only plugin certifies waiting
  through reset, reconnect, exactly-once completion, restoration, native boss-bar
  create/update/remove, listener register/unregister and off-owner mutation rejection.
  Evidence is `build/evidence/m08-paper-primary.json`.
- Neutral artifacts are built on the approved JDK 8/11/16/17 matrix; the full primary
  assembly is built on JDK 21. Binary/API, JavaDoc, dependency/licence/provenance and
  deterministic governance gates are part of the M08 validation chain.

## Generated artifacts

- `game/zbw-game/target/zbw-game-0.1.0-SNAPSHOT.jar`
- `platform/paper/zbw-paper-modern/target/zbw-paper-modern-0.1.0-SNAPSHOT.jar`
- `build/api-signature-baseline-m08.txt`
- `build/api-signature-baseline-m08-modern.txt`
- `build/evidence/m08-paper-primary.json`
- `target/zartrabedwars-m08-neutral-javadoc.zip`
- `target/zartrabedwars-m08-modern-javadoc.zip`
- Surefire, JaCoCo, Checkstyle and SpotBugs reports

The certification plugin is assembled from test classes under `target/m08-paper-e2e`
and is excluded from every release artifact.
