# M07 setup lifecycle

A setup session has an immutable session ID, arena target, authenticated actor,
base repository revision, monotonically increasing draft revision and terminal
state. At most one active session per arena is admitted and global session
capacity is bounded.

Typed mutations cover world/template/adapter selection, waiting and spectator
spawns, bounds and build/void limits, teams, team spawns and beds, team/custom/
diamond/emerald generators, shop and upgrade NPCs, protected regions, group,
modes, player limits, speeds, holograms, metadata and rules. Removing a team
also removes its owned generator and NPC references.

Every edit appends a bounded immutable history snapshot and clears redo.
Undo/redo never changes the durable arena until commit. Marker discovery
returns a proposal only; explicit apply is required at the same draft revision.
Preview applies 1–256 ordered changes to an isolated candidate and records both
base and candidate fingerprints. Any base revision or draft change makes it
stale.

Commit revalidates, optionally enables, and delegates one atomic arena/session
transaction. Failure exposes no partial state; the active session remains
recoverable. Abandon is terminal and leaves the durable arena unchanged.

M09 owns commands, inventory tools, wizard pages, editor navigation and common
confirmation tokens. Those surfaces call this lifecycle and contain no setup
business rule.
