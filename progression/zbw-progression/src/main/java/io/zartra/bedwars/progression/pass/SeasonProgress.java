package io.zartra.bedwars.progression.pass;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Durable player XP, tier and claim state for one battle-pass season. */
public final class SeasonProgress {
    private final SeasonId seasonId;
    private final PlayerProgressionId playerId;
    private final int definitionVersion;
    private final long experience;
    private final int tier;
    private final Set<Integer> claimedFreeTiers;
    private final long revision;
    private final Optional<IdempotencyKey> lastEvent;
    private final Instant updatedAt;

    /** Creates a validated season snapshot. Premium claims remain outside M13 Phase 2. */
    public SeasonProgress(final SeasonId seasonId, final PlayerProgressionId playerId,
                          final int definitionVersion, final long experience, final int tier,
                          final Set<Integer> claimedFreeTiers, final long revision,
                          final Optional<IdempotencyKey> lastEvent, final Instant updatedAt) {
        this.seasonId = Objects.requireNonNull(seasonId, "seasonId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (definitionVersion < 1 || experience < 0 || tier < 0 || revision < 0) {
            throw new IllegalArgumentException("invalid season counters");
        }
        this.definitionVersion = definitionVersion;
        this.experience = experience;
        this.tier = tier;
        final Set<Integer> claims = new LinkedHashSet<Integer>(
                Objects.requireNonNull(claimedFreeTiers, "claimedFreeTiers"));
        if (claims.contains(null) || claims.contains(0)) {
            throw new IllegalArgumentException("claimed tiers must be positive");
        }
        for (Integer claim : claims) {
            if (claim < 1 || claim > tier) { throw new IllegalArgumentException("claim exceeds unlocked tier"); }
        }
        this.claimedFreeTiers = Collections.unmodifiableSet(claims);
        this.revision = revision;
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
    /** @return season identity */ public SeasonId seasonId() { return seasonId; }
    /** @return player identity */ public PlayerProgressionId playerId() { return playerId; }
    /** @return definition version */ public int definitionVersion() { return definitionVersion; }
    /** @return accumulated season XP */ public long experience() { return experience; }
    /** @return highest unlocked tier */ public int tier() { return tier; }
    /** @return immutable claimed free tiers */ public Set<Integer> claimedFreeTiers() { return claimedFreeTiers; }
    /** @return optimistic revision */ public long revision() { return revision; }
    /** @return last applied event */ public Optional<IdempotencyKey> lastEvent() { return lastEvent; }
    /** @return update instant */ public Instant updatedAt() { return updatedAt; }
}
