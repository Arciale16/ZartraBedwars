# M10 matchmaking extension guide

Implement `MatchingPolicy` as a pure deterministic function over immutable requests, arena facts,
an explicit instant and aging interval. Sort all inputs, return structured reasons, retain complete
parties and keep every loop bounded. Invoke game creation only through `AssignmentPort`; confirm
after accepted M08 assignment and release on rejection, timeout or failure. Extensions must not
use Bukkit types, common-pool execution, synchronous I/O or unbounded caches.
