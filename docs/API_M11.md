# Milestone 11 API checkpoint

M11 adds Java-8-neutral contracts in `io.zartra.bedwars.shop`,
`io.zartra.bedwars.content` and `io.zartra.bedwars.scripting.api`. Public values are immutable,
validated at construction and expose no Bukkit, Paper, storage implementation or global state.

The shop API covers typed catalogues, prices, match-local tenders, quotes, atomic transaction
ports, Quick Buy/favourite/history/rotation contracts, generators, upgrades, forge, traps and
bounded utility actions. `M11MatchRuntime` is the integration boundary over M08 snapshots.

The M09 presentation framework is extended additively by `PresentationActions.Catalog.m11()`;
the generated command and permission inventories are authoritative for the 25 M11 actions.
`M11PaperProjection` is Java 21 and performs owner-thread translation only.

Exact signatures are recorded in `build/api-signature-baseline-m11.txt` and
`build/api-signature-baseline-m11-modern.txt`. M10 baselines remain immutable and are checked as
subsets. Strict JavaDoc archives are generated as
`target/zartrabedwars-m11-neutral-javadoc.zip` and
`target/zartrabedwars-m11-modern-javadoc.zip`.

M11.1 adds the declarative execution engine, transactional M11 configuration activation,
mode-balance simulation APIs, atomic inventory integration, SQL user-state recovery, complete
mechanics orchestration, M09 action bindings and exact Paper projections. The engine consumes the
M03 `AuthorizationService`, carries an
immutable `AuthorizationSubject` in each input and checks `zartrabedwars.script.execute` against
the script ID. The exact additive Java 8 surface is locked by
`build/api-signature-baseline-m11-1.txt` and additive final compatibility evidence. Retained
M12/M15/M16/M17/M18/M19/M20/M21/M22 provider surfaces are not reported complete.

Historical M02–M11 baselines remain immutable and additive checks cover the M11.1 surface. PR #18
completed the mandatory remote API and strict-JavaDoc validation before its squash merge
`3e68835c361216e6dc8be37b9e024734bb565884`. M11 is complete; this document does not claim M12
implementation or activation.
