package io.zartra.bedwars.game.application;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.game.model.MatchTimingPolicy;
import java.time.Instant;
import java.util.Objects;

/** Immutable version-fenced input for constructing one arena-backed match aggregate. */
public final class MatchAssemblyRequest {
    private final MatchId matchId;
    private final ArenaBundle arenaBundle;
    private final long expectedArenaVersion;
    private final long expectedMapVersion;
    private final MatchTimingPolicy timingPolicy;
    private final Instant createdAt;

    /** Creates a request that rejects stale arena or map definitions. */
    public MatchAssemblyRequest(
            final MatchId matchId, final ArenaBundle arenaBundle,
            final long expectedArenaVersion, final long expectedMapVersion,
            final MatchTimingPolicy timingPolicy, final Instant createdAt) {
        if (expectedArenaVersion < 0L || expectedMapVersion < 0L) {
            throw new IllegalArgumentException("expected versions must be non-negative");
        }
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.arenaBundle = Objects.requireNonNull(arenaBundle, "arenaBundle");
        this.expectedArenaVersion = expectedArenaVersion;
        this.expectedMapVersion = expectedMapVersion;
        this.timingPolicy = Objects.requireNonNull(timingPolicy, "timingPolicy");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    /** @return new match identity */ public MatchId matchId() { return matchId; }
    /** @return atomic arena and map definition input */ public ArenaBundle arenaBundle() { return arenaBundle; }
    /** @return required arena revision */ public long expectedArenaVersion() { return expectedArenaVersion; }
    /** @return required map revision */ public long expectedMapVersion() { return expectedMapVersion; }
    /** @return timings independent of arena-derived limits */ public MatchTimingPolicy timingPolicy() { return timingPolicy; }
    /** @return initial aggregate timestamp */ public Instant createdAt() { return createdAt; }
}
