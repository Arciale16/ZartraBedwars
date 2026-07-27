package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable bounded replay search query for staff inspection. */
public final class ReplayStaffQuery {
    private final PlayerId playerId;
    private final MatchId matchId;
    private final Instant createdFrom;
    private final Instant createdTo;
    private final Long minimumDurationMillis;
    private final Long maximumDurationMillis;
    private final int limit;

    /** Creates a validated query; at least one filter must be supplied. */
    public ReplayStaffQuery(final PlayerId playerId, final MatchId matchId,
                            final Instant createdFrom, final Instant createdTo,
                            final Long minimumDurationMillis,
                            final Long maximumDurationMillis, final int limit) {
        if (playerId == null && matchId == null && createdFrom == null && createdTo == null
                && minimumDurationMillis == null && maximumDurationMillis == null) {
            throw new IllegalArgumentException("at least one replay search filter is required");
        }
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }
        if (minimumDurationMillis != null && minimumDurationMillis.longValue() < 0L
                || maximumDurationMillis != null && maximumDurationMillis.longValue() < 0L
                || minimumDurationMillis != null && maximumDurationMillis != null
                && minimumDurationMillis.longValue() > maximumDurationMillis.longValue()) {
            throw new IllegalArgumentException("duration bounds are invalid");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("search limit must be 1..100");
        }
        this.playerId = playerId;
        this.matchId = matchId;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.minimumDurationMillis = minimumDurationMillis;
        this.maximumDurationMillis = maximumDurationMillis;
        this.limit = limit;
    }

    /** @return optional player filter */ public Optional<PlayerId> playerId() {
        return Optional.ofNullable(playerId);
    }
    /** @return optional match filter */ public Optional<MatchId> matchId() {
        return Optional.ofNullable(matchId);
    }
    /** @return optional inclusive creation lower bound */ public Optional<Instant> createdFrom() {
        return Optional.ofNullable(createdFrom);
    }
    /** @return optional inclusive creation upper bound */ public Optional<Instant> createdTo() {
        return Optional.ofNullable(createdTo);
    }
    /** @return optional minimum duration */ public Optional<Long> minimumDurationMillis() {
        return Optional.ofNullable(minimumDurationMillis);
    }
    /** @return optional maximum duration */ public Optional<Long> maximumDurationMillis() {
        return Optional.ofNullable(maximumDurationMillis);
    }
    /** @return maximum rows */ public int limit() { return limit; }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof ReplayStaffQuery)) { return false; }
        final ReplayStaffQuery value = (ReplayStaffQuery) other;
        return Objects.equals(playerId, value.playerId)
                && Objects.equals(matchId, value.matchId)
                && Objects.equals(createdFrom, value.createdFrom)
                && Objects.equals(createdTo, value.createdTo)
                && Objects.equals(minimumDurationMillis, value.minimumDurationMillis)
                && Objects.equals(maximumDurationMillis, value.maximumDurationMillis)
                && limit == value.limit;
    }

    @Override public int hashCode() {
        return Objects.hash(playerId, matchId, createdFrom, createdTo,
                minimumDurationMillis, maximumDurationMillis, limit);
    }
}
