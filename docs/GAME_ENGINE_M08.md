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
