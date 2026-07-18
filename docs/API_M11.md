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

The declarative execution engine, complete named-mode orchestration and retained M12/M15/M16/
M19/M20/M21/M22 provider surfaces are not part of this checkpoint and are not reported complete.

Merged PR #17 preserves this checkpoint boundary. Its successful CI validates the delivered APIs
but does not supply the explicitly missing M11 exit artifacts, so this API document does not claim
M11 completion or M12 activation.
