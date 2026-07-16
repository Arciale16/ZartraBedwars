# M08 game engine

The match aggregate is the sole authority for session/team state. Every accepted
command checks an expected revision, advances exactly one immutable snapshot and emits
immutable facts. Invalid transitions have no side effect. Team assignment is stable
and capacity-bounded; start policy requires the configured minimum unless an already
authorized force-start use case chooses the typed command.

Completion follows `fence -> atomic commit/outbox -> restore every player -> reset`.
Repeating the same completion key is idempotent; conflicting keys fail. Generic timed
phase scheduling supports standard timeout, sudden-death, dragon, border and custom
semantic phases without implementing M10 game-mode rules.

All I/O is behind asynchronous ports. A match admits one pending persistence operation
at a time, keeps bounded registry state and never blocks the Minecraft owner thread.

## M08.1 configurable-layout correction

Normal runtime composition now begins at `ArenaMatchAssembler`, which consumes an
enabled, exact-version `ArenaBundle`. Player limits and complete team definitions are
derived from arena/map data; callers do not manually duplicate team IDs, labels,
colors or capacities. Shared Java-8 limits permit 2–64 teams, per-team capacity 1–64
and at most 256 admitted players. Standard 2/4/8-team layouts and custom counts follow
the same path with no fixed indexes or color list.

Elimination invokes the configured `VictoryEvaluator`. The generic evaluator considers
teams with admitted members eligible, requires at least two eligible teams and returns
a typed completion intent when exactly one remains non-eliminated. A retry returns the
same intent. The caller still applies `completionCommand(idempotencyKey)`, so the
existing fence, atomic commit/outbox, restore and reset protocol is unchanged. M10 may
replace the evaluator for mode-specific victory rules; Paper never evaluates a winner.
