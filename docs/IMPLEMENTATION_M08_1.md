# M08.1 implementation evidence

## Scope and requirements

M08.1 corrects configurability defects in the merged M07/M08 foundation for
`ZBW-GAME-001/002/004`, `ZBW-ARENA-002/008`, `ZBW-OPS-001`, `ZBW-QA-001/002`
and the overlapping atomic capabilities `ZBW-ADDON-156/411/419/421`.

It adds no command, GUI, editor, selector, matchmaking behavior, mode implementation,
storage migration or Paper production behavior. M09 and M10 remain unstarted. The
existing module graph is unchanged and no dependency was added.

## Delivered hardening

- `TeamLayoutLimits` is the single Java-8 authority used by arena, map, team and match
  models. It allows 2–64 teams, 1–64 players per team and at most 256 admitted players.
- `ArenaDefaultProfile` replaces anonymous arena draft defaults. Values are immutable,
  typed, replaceable by composition/configuration and copied into each new definition.
- `ArenaValidationProfile` compares exact `GeneratorTypeId` values. The starter profile
  requires exact diamond and emerald shared generators; custom profiles may declare
  arbitrary types and team/NPC prerequisite switches.
- `ArenaMatchAssembler` derives arena identity, player limits and complete immutable
  runtime team definitions from an enabled, version-fenced, validated `ArenaBundle`.
  Disabled, stale, incomplete or cross-definition-inconsistent inputs fail closed with
  typed errors.
- `VictoryEvaluator` and `VictoryEvaluation` provide a deterministic, replaceable
  Java-8 boundary. The starter evaluator emits a typed completion intent only when
  exactly one of at least two participating teams survives. The existing
  `IdempotencyKey` completion fence, durable commit, restoration and reset remain the
  only terminal mutation path.
- `TeamSnapshot` retains configured stable identity, display name, semantic color and
  capacity through assignment, elimination, persistence recovery and reset.

## Verification evidence

- `TeamLayoutLimitsTest` covers every shared lower/upper boundary.
- `M081ArenaHardeningTest` covers typed defaults, exact diamond/emerald requirements,
  substring lookalike rejection, arbitrary custom resource profiles, cross-definition
  consistency and malformed/null values.
- `TeamLayoutMatrixTest` names and exercises Solo 8x1, Doubles 8x2, 3v3v3v3, 4v4v4v4,
  4v4, custom 12x3 and the shared 64x4 maximum. It proves assembly, identity/color
  preservation, capacity, assignment, admission, reconnect, elimination, automatic
  victory intent, idempotent completion, restoration, reset and recovery.
- The neutral reactor passes 217 tests with zero failures, errors or skips on each of
  the pinned Java 8.0.442, 11.0.26, 16.0.2 and 17.0.14 toolchains. The complete
  Java-21.0.6 reactor passes 234 tests with zero failures, errors or skips, including
  the modern compatibility and Paper modules.
- The checksum-locked Paper 1.21.1 build 133 certification was rerun and passed
  waiting-through-reset, reconnect recovery, exactly-once completion, player-state
  restoration, boss-bar create/update/remove, runtime register/unregister and
  off-owner mutation rejection. The server SHA-256 is
  `39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9`; regenerated
  evidence is `build/evidence/m08-paper-primary.json`.
- Checkstyle reports zero violations and SpotBugs reports zero findings. JaCoCo is
  91.01% line/100% branch for `zbw-domain`, 95.99%/85.08% for `zbw-arena`,
  95.87%/86.59% for `zbw-game`, 98.46%/90% for the modern compatibility adapter and
  89.89%/87.34% for the test-JVM-safe Paper projection.
- The M08.1 binary/API gate locks 434 Java-8 neutral and 14 Java-21 modern signatures
  while proving every M08 signature remains present. Strict JavaDoc covers 264 neutral
  and 19 modern source files. Dependency, licence, provenance, SBOM and deterministic
  governance gates pass without a mandatory skip.
- Historical M02-M06 compatibility gates retain their committed baselines as immutable
  subsets of the current API: additions are permitted, while any removed or changed
  descriptor still fails. The exact current surface remains locked by the M08.1
  baselines. This prevents later additive APIs from invalidating historical CI gates.
- Strict modern JavaDoc resolves the exact reactor JARs rather than platform-sensitive
  class-directory lists, keeping the same locked inputs on Windows and Linux.

## Artifacts

- `domain/zbw-domain/target/zbw-domain-0.1.0-SNAPSHOT.jar`
- `arena/zbw-arena/target/zbw-arena-0.1.0-SNAPSHOT.jar`
- `game/zbw-game/target/zbw-game-0.1.0-SNAPSHOT.jar`
- `platform/paper/zbw-paper-modern/target/zbw-paper-modern-0.1.0-SNAPSHOT.jar`
- `build/api-signature-baseline-m08-1.txt`
- `build/api-signature-baseline-m08-1-modern.txt`
- `build/evidence/m08-paper-primary.json`
- `target/zartrabedwars-m08-1-neutral-javadoc.zip`
- `target/zartrabedwars-m08-1-modern-javadoc.zip`
- Surefire, JaCoCo, Checkstyle and SpotBugs reports

Generated `target/` outputs are evidence/build artifacts and are not committed.
